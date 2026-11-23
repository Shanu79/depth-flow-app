package com.shin.depthflow.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Path
import okhttp3.ResponseBody
import retrofit2.http.*

// API request/response models for the Depth Flow Backend
data class UploadResponse(
    val jobId: String,
    val status: String,
    val message: String? = null
)

data class JobStatusResponse(
    val jobId: String,
    val status: String, // "PENDING", "PROCESSING", "SUCCESS", "FAILED"
    val progress: Int,  // 0 to 100
    val message: String? = null,
    val resultUrl: String? = null
)



interface DepthFlowApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): MessageResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: OTPVerifyRequest): LoginResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): MessageResponse

    @POST("auth/google/verify")
    suspend fun verifyGoogleToken(@Body request: GoogleTokenRequest): LoginResponse

    @GET("auth/me")
    suspend fun getUserProfile(@Header("Authorization") token: String): UserProfileResponse

    @GET("payments/history")
    suspend fun getPaymentHistory(@Header("Authorization") token: String): List<PaymentHistoryItem>

    @GET("ai/depthflow/logs")
    suspend fun getGenerationLogs(@Header("Authorization") token: String): List<GenerationLog>

    @GET("ai/depthflow/billing")
    suspend fun getBillingInfo(@Header("Authorization") token: String): BillingInfoResponse

    @Multipart
    @POST("ai/depthflow/generate-3d")
    suspend fun generate3D(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("payload") payload: RequestBody,
        @Part("request_source") source: RequestBody
    ): Response<ResponseBody>

    @POST("payments/verify-purchase")
    suspend fun verifyPurchase(
        @Header("Authorization") token: String,
        @Body request: PurchaseVerificationRequest
    ): Response<PurchaseVerificationResponse>

    @POST("payments/cancel-subscription")
    suspend fun cancelSubscription(
        @Header("Authorization") token: String,
        @Body request: CancelSubscriptionRequest
    ): Response<CancelSubscriptionResponse>

    @POST("payments/resubscribe")
    suspend fun resubscribeSubscription(
        @Header("Authorization") token: String,
        @Body request: CancelSubscriptionRequest
    ): Response<CancelSubscriptionResponse>

    @Multipart
    @POST("auth/update-profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Part("full_name") fullName: RequestBody?,
        @Part("email") email: RequestBody?,
        @Part profilePic: MultipartBody.Part?
    ): Response<UserProfileResponse>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    // --- Admin-managed content: home popup, offer banner, What's New ---
    @GET("popups/eligible")
    suspend fun getEligiblePopups(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 1
    ): PopupsResponse

    @POST("popups/{id}/track")
    suspend fun trackPopup(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): TrackResponse

    @GET("offer-banner/active")
    suspend fun getActiveBanner(): BannerResponse

    @GET("whats-new")
    suspend fun getWhatsNew(): WhatsNewResponse

    // --- Happy Hour ---
    @GET("happy-hour/status")
    suspend fun getHappyHourStatus(@Header("Authorization") token: String): HappyHourStatus

    @POST("happy-hour/activate")
    suspend fun activateHappyHour(
        @Header("Authorization") token: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): HappyHourStatus

    @GET("happy-hour/history")
    suspend fun getHappyHourHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 50
    ): List<HappyHourSession>

    // --- Notification inbox ---
    @GET("notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = 20,
        @Query("after_id") afterId: Long? = null,
        @Query("before_id") beforeId: Long? = null,
        @Query("unread_only") unreadOnly: Boolean = false
    ): InboxResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(@Header("Authorization") token: String): UnreadCountResponse

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: Long
    ): ReadResponse

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(@Header("Authorization") token: String): ReadResponse

    // --- Push device registration ---
    @POST("notifications/devices")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceRequest
    ): DeviceStatusResponse

    @POST("notifications/devices/unregister")
    suspend fun unregisterDevice(
        @Header("Authorization") token: String,
        @Body request: DeviceUnregisterRequest
    ): DeviceStatusResponse
}

// Data Classes matching your Python schemas.py
data class LoginRequest(val email: String, val password: String, val message: String? = null)

data class RegisterRequest(
    val full_name: String,
    val email: String,
    val password: String
)

data class OTPVerifyRequest(
    val email: String,
    val otp: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val new_password: String
)

data class MessageResponse(
    val message: String
)

data class LoginResponse(val access_token: String, val token_type: String)
// In data/DepthFlowApi.kt
data class UserProfileResponse(
    val id: Int,
    val email: String,
    val full_name: String?, // Backend can return null
    val credits: Int,
    val profile_pic: String?, // Backend can return null
    val plan: String,
    val subscription_status: String?,
    val billing_cycle: String?,
    val created_at: String?,
    val next_credit_drop_date: String?,
    val subscription_end_date: String?,
    val api_plan: String?,
    val api_subscription_status: String?
)
// Data class for sending the Google ID Token to the backend
data class GoogleTokenRequest(
    val id_token: String
)

data class GenerationLog(
    val id: String,
    val status: String,
    val credits: Int,
    val duration: String,
    val time: String
)

data class BillingInfoResponse(
    val credits: Int,
    val platform: SubscriptionDetail,
    val api: SubscriptionDetail
)

data class SubscriptionDetail(
    val plan: String,
    val billing_cycle: String,
    val status: String,
    val next_billing_date: String,
    val is_active: Boolean
)

data class PurchaseVerificationRequest(
    val purchase_token: String,
    val product_id: String
)

data class PurchaseVerificationResponse(
    val status: String,
    val message: String? = null,
    val plan: String? = null,
    val billing_cycle: String? = null,
    val new_balance: Int? = null
)

data class PaymentHistoryItem(
    val id: String,
    val amount: Double,
    val status: String,
    val created_at: String,
    val invoice_url: String?,
    val source: String? // "app" (Google Play) or "web" (Dodo)
)

data class CancelSubscriptionRequest(
    val plan_type: String = "workspace"
)

data class CancelSubscriptionResponse(
    val status: String,
    val message: String,
    val new_status: String
)

data class ChangePasswordRequest(
    val old_password: String,
    val new_password: String
)







