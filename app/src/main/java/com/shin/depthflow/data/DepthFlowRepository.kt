package com.shin.depthflow.data

import com.shin.depthflow.util.Logger
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class PipelineState {
    object Idle : PipelineState()
    data class Uploading(val progress: Int) : PipelineState()
    data class Processing(val progress: Int, val statusText: String) : PipelineState()
    data class Success(
        val videoUri: Uri,
        val originalUri: Uri?,
        val motionPath: String,
        val amplitude: Float,
        val remainingCredits: Int? = null,
        // Happy Hour response headers (server decides; the app only reflects them)
        val happyHourActive: Boolean = false,
        val happyHourRemainingVideos: Int? = null,
        val cost: Int? = null
    ) : PipelineState()
    data class Error(val errorMessage: String) : PipelineState()
}

class DepthFlowRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val projectDao = database.projectDao()

    // Shared Retrofit client (base URL comes from BuildConfig.BASE_URL).
    private val apiService: DepthFlowApiService? get() = ApiClient.service

    suspend fun loginUser(email: String, password: String): LoginResponse {
        val response = apiService?.login(LoginRequest(email, password))
            ?: throw Exception("API Service not initialized")
        return response
    }

    suspend fun registerUser(request: RegisterRequest): MessageResponse {
        return apiService?.register(request)
            ?: throw Exception("API Service not initialized")
    }

    suspend fun verifyOtp(request: OTPVerifyRequest): LoginResponse {
        return apiService?.verifyOtp(request)
            ?: throw Exception("API Service not initialized")
    }

    suspend fun requestPasswordReset(email: String): MessageResponse {
        return apiService?.forgotPassword(ForgotPasswordRequest(email))
            ?: throw Exception("API Service not initialized")
    }

    suspend fun resetPassword(request: ResetPasswordRequest): MessageResponse {
        return apiService?.resetPassword(request)
            ?: throw Exception("API Service not initialized")
    }

    // ==========================================
    // --- NEW: Verify Google Token ---
    // ==========================================
    suspend fun verifyGoogleToken(idToken: String): LoginResponse {
        val request = GoogleTokenRequest(id_token = idToken)
        val response = apiService?.verifyGoogleToken(request)
            ?: throw Exception("API Service not initialized or network failed")
        return response
    }

    suspend fun fetchGenerationLogs(token: String): List<GenerationLog> {
        return apiService?.getGenerationLogs(token) ?: emptyList()
    }

    suspend fun fetchBillingInfo(token: String): BillingInfoResponse {
        return apiService?.getBillingInfo(token) 
            ?: throw Exception("API Service not initialized or network failed")
    }

    suspend fun fetchPaymentHistory(token: String): List<PaymentHistoryItem> {
        return apiService?.getPaymentHistory(token) ?: emptyList()
    }

    suspend fun verifyPurchase(token: String, purchaseToken: String, productId: String): PurchaseVerificationResponse {
        val request = PurchaseVerificationRequest(purchaseToken, productId)
        val response = apiService?.verifyPurchase(token, request)
        
        if (response != null && response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response from server")
        } else {
            val errorMsg = response?.errorBody()?.string() ?: "Unknown Server Error"
            throw Exception("Verification failed: $errorMsg")
        }
    }

    suspend fun cancelSubscription(token: String, planType: String): CancelSubscriptionResponse {
        val request = CancelSubscriptionRequest(planType)
        val response = apiService?.cancelSubscription(token, request)
        if (response != null && response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            val errorMsg = response?.errorBody()?.string() ?: "Unknown error"
            throw Exception(errorMsg)
        }
    }

    suspend fun resubscribeSubscription(token: String, planType: String): CancelSubscriptionResponse {
        val request = CancelSubscriptionRequest(planType)
        val response = apiService?.resubscribeSubscription(token, request)
        if (response != null && response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            val errorMsg = response?.errorBody()?.string() ?: "Unknown error"
            throw Exception(errorMsg)
        }
    }

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun saveProject(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
    }

    suspend fun updateProfile(
        token: String,
        fullName: String?,
        email: String?,
        imageFile: File?
    ): UserProfileResponse {
        val fullNameBody = fullName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email?.toRequestBody("text/plain".toMediaTypeOrNull())
        val profilePicPart = imageFile?.let {
            val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("profile_pic", it.name, requestFile)
        }

        val response = apiService?.updateProfile(token, fullNameBody, emailBody, profilePicPart)
        if (response != null && response.isSuccessful) {
            return response.body() ?: throw Exception("Empty response body")
        } else {
            val errorMsg = response?.errorBody()?.string() ?: "Unknown error"
            throw Exception(errorMsg)
        }
    }

    suspend fun changePassword(token: String, request: ChangePasswordRequest) {
        val response = apiService?.changePassword(token, request)
        if (response == null || !response.isSuccessful) {
            val errorMsg = response?.errorBody()?.string() ?: "Unknown error"
            throw Exception(errorMsg)
        }
    }

    // --- UPDATED: Process Image connected to Python Backend ---
    suspend fun processImage(
        imageUri: Uri,
        token: String,
        renderSettings: Map<String, Any>,
        motionSettings: Map<String, Any>,
        effectsSettings: Map<String, Any>,
        plan: String
    ): Flow<PipelineState> = flow {

        emit(PipelineState.Uploading(0))

        // Live Retrofit Networking Flow against Python Backend
        try {
            val fileData = getFileFromUri(context, imageUri)
            if (fileData == null) {
                emit(PipelineState.Error("Could not resolve image file path."))
                return@flow
            }
            val file = fileData.first
            val originalName = fileData.second

            // 1. Prepare File
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // 2. Create a dynamic JSON payload.
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(Map::class.java)

            // Convert style name to API format (lowercase with underscores)
            val apiMotionSettings = motionSettings.toMutableMap().apply {
                val prettyStyle = this["style"] as? String
                if (prettyStyle != null) {
                    this["style"] = prettyStyle.lowercase().replace(" ", "_")
                }
            }

            val fullPayload = mapOf(
                "render" to renderSettings,
                "motion" to apiMotionSettings,
                "effects" to effectsSettings,
                "plan" to plan
            )
            val payloadJson = adapter.toJson(fullPayload)

            val payloadBody = payloadJson.toRequestBody("application/json".toMediaTypeOrNull())
            // Updated sourceBody to track app generations correctly in your DB
            val sourceBody = "android_app".toRequestBody("text/plain".toMediaTypeOrNull())

            emit(PipelineState.Processing(25, "Uploading image to DepthFlow Cloud..."))

            // 3. Make the API Call
            val response = apiService?.generate3D(
                token = token,
                file = filePart,
                payload = payloadBody,
                source = sourceBody
            )

            if (response != null && response.isSuccessful) {
                // Success! Save the video to a file
                val responseBody = response.body()
                val remainingCredits = response.headers()["X-Remaining-Credits"]?.toIntOrNull()
                val happyHourActive = response.headers()["X-Happy-Hour"].equals("active", ignoreCase = true)
                val happyHourRemaining = response.headers()["X-Happy-Hour-Remaining-Videos"]?.toIntOrNull()
                val cost = response.headers()["X-Cost"]?.toIntOrNull()

                if (responseBody != null) {
                    val videoFile = saveVideoToFile(context, responseBody, originalName)
                    if (videoFile != null) {
                        val videoUri = Uri.fromFile(videoFile)
                        val motionPath = (motionSettings["style"] as? String) ?: "Dolly"
                        val amplitude = (motionSettings["amplitude"] as? Number)?.toFloat() ?: 1.0f
                        
                        // NEW: Save to local project history
                        val projectTitle = "${originalName}_${motionPath}"
                        val project = ProjectEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            title = projectTitle,
                            imageUrl = imageUri.toString(),
                            videoUrl = videoUri.toString(),
                            timestamp = System.currentTimeMillis(),
                            motionPath = motionPath,
                            intensity = amplitude
                        )
                        saveProject(project)

                        emit(PipelineState.Success(videoUri, imageUri, motionPath, amplitude, remainingCredits, happyHourActive, happyHourRemaining, cost))
                    } else {
                        emit(PipelineState.Error("Failed to save video file."))
                    }
                } else {
                    emit(PipelineState.Error("Empty response from server."))
                }
            } else {
                // Handle FastAPI 402 Payment Required or 500 Server Error
                val raw = response?.errorBody()?.string()
                // Prefer the server's human-readable `detail` (e.g. 402 insufficient credits)
                val detail = try {
                    (ApiClient.moshi.adapter(Map::class.java).fromJson(raw ?: "")?.get("detail")) as? String
                } catch (e: Exception) { null }
                emit(PipelineState.Error("Generation failed: ${detail ?: raw ?: "Unknown Server Error"}"))
            }
        } catch (e: CancellationException) {
            throw e // cancelled, not a network error
        } catch (e: Exception) {
            Logger.e("DepthFlow", "Generation failed", e)
            emit(PipelineState.Error("Network error: ${e.message}"))
        }
    }
        // File copy, upload, MP4 save and the Room insert run off the main thread;
        // the collector still receives states on its own (main) dispatcher.
        .flowOn(Dispatchers.IO)

    private fun saveVideoToFile(context: Context, responseBody: okhttp3.ResponseBody, originalName: String): File? {
        return try {
            val videoFile = File(context.cacheDir, "generated_3d_${originalName}_${System.currentTimeMillis()}.mp4")
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(videoFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            videoFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileFromUri(context: Context, uri: Uri): Pair<File, String>? {
        return try {
            var fileName = "unknown"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    val fullName = cursor.getString(nameIndex)
                    fileName = fullName.substringBeforeLast(".")
                }
            }

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("depthflow_input", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            Pair(tempFile, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Happy Hour / notifications: HTTP failures surface as ApiException(code, detail) ---
    private suspend fun <T> call(block: suspend (DepthFlowApiService) -> T): T {
        val api = apiService ?: throw Exception("API Service not initialized")
        try {
            return block(api)
        } catch (e: retrofit2.HttpException) {
            throw e.toApiException()
        }
    }

    suspend fun getEligiblePopups(token: String) = call { it.getEligiblePopups(token) }

    suspend fun trackPopup(token: String, id: Int) = call { it.trackPopup(token, id) }

    suspend fun getActiveBanner() = call { it.getActiveBanner() }

    suspend fun getWhatsNew() = call { it.getWhatsNew() }

    suspend fun getHappyHourStatus(token: String) = call { it.getHappyHourStatus(token) }

    suspend fun activateHappyHour(token: String, idempotencyKey: String) =
        call { it.activateHappyHour(token, idempotencyKey) }

    suspend fun getHappyHourHistory(token: String) = call { it.getHappyHourHistory(token) }

    suspend fun getNotifications(token: String, beforeId: Long? = null) =
        call { it.getNotifications(token, beforeId = beforeId) }

    suspend fun getUnreadCount(token: String) = call { it.getUnreadCount(token) }

    suspend fun markNotificationRead(token: String, id: Long) = call { it.markNotificationRead(token, id) }

    suspend fun markAllNotificationsRead(token: String) = call { it.markAllNotificationsRead(token) }

    suspend fun fetchUserProfile(token: String): UserProfileResponse {
        return apiService?.getUserProfile(token)
            ?: throw Exception("API Service not initialized")
    }
}
