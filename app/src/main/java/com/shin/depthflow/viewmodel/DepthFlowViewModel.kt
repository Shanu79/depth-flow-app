package com.shin.depthflow.viewmodel

import com.shin.depthflow.util.GenerationNotifier
import com.shin.depthflow.util.AppScope

import com.shin.depthflow.util.Logger
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Import your specific packages for Repository and PipelineState
import com.shin.depthflow.data.DepthFlowRepository
import com.shin.depthflow.data.PipelineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.shin.depthflow.data.ChangePasswordRequest
import com.shin.depthflow.data.ProjectEntity
import com.shin.depthflow.data.ForgotPasswordRequest
import com.shin.depthflow.data.ResetPasswordRequest
import com.shin.depthflow.data.OTPVerifyRequest
import com.shin.depthflow.data.RegisterRequest
import com.shin.depthflow.data.SessionManager
import com.shin.depthflow.data.PaymentHistoryItem
import java.text.SimpleDateFormat
import java.util.Locale
import com.shin.depthflow.R
import com.google.firebase.analytics.FirebaseAnalytics
import android.os.Bundle
import org.json.JSONObject
import retrofit2.HttpException


class DepthFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DepthFlowRepository(application)
    private val sessionManager = SessionManager(application) // <-- ADD THIS
    private val analytics by lazy { 
        try {
            FirebaseAnalytics.getInstance(application)
        } catch (e: Exception) {
            Logger.e("DepthFlowViewModel", "Firebase Analytics initialization failed", e)
            null
        }
    }

    fun logEvent(name: String, params: Bundle? = null) {
        try {
            analytics?.logEvent(name, params)
        } catch (e: Exception) {
            Logger.e("DepthFlowViewModel", "Failed to log event: $name", e)
        }
    }

    // ==========================================
    // --- NEW: AUTH & CREDITS STATE ---
    // ==========================================
    private var jwtToken: String? = null

    // Raw JWT (no "Bearer " prefix) mirrored for other ViewModels (e.g. HappyHourViewModel).
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    // Fires after the backend verified a Play purchase (Happy Hour tokens are granted there).
    private val _purchaseVerified = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val purchaseVerified: kotlinx.coroutines.flow.SharedFlow<Unit> = _purchaseVerified

    private val _credits = MutableStateFlow(0)
    val credits: StateFlow<Int> = _credits.asStateFlow()

    // ADD THESE MISSING AUTH VARIABLES:
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _registrationEmail = MutableStateFlow("")
    val registrationEmail: StateFlow<String> = _registrationEmail.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _loginToken = MutableStateFlow<String?>(null)
    val loginToken: StateFlow<String?> = _loginToken.asStateFlow()

    fun clearLoginToken() {
        _loginToken.value = null
    }

    // ADD THIS MISSING LOGIN FUNCTION:
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null

            try {
                val response = repository.loginUser(email, password)
                _loginToken.value = response.access_token
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 403) _authError.value = getApplication<Application>().getString(R.string.err_email_not_verified)
                else if (e.code() == 401) _authError.value = getApplication<Application>().getString(R.string.err_incorrect_credentials)
                else _authError.value = getApplication<Application>().getString(R.string.err_server_error)
            } catch (e: Exception) {
                // ADD THIS LINE TO EXPOSE THE REAL ERROR:
                Logger.e("NetworkDebug", "Retrofit Failed: ${e.message}", e)

                _authError.value = getApplication<Application>().getString(R.string.err_network_error)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    // --- Google Login Flow ---
    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                // Call your repository which calls the Retrofit endpoint we added in Step 1
                val response = repository.verifyGoogleToken(idToken)

                // Trigger the UI navigation just like email login
                _loginToken.value = response.access_token
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = getApplication<Application>().getString(R.string.err_google_login_failed)
                Logger.e("Auth", "Google Error: ${e.message}", e)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun register(fullName: String, email: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val request = RegisterRequest(fullName, email, password)
                val response = repository.registerUser(request)
                _registrationEmail.value = email
                onSuccess(response.message)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val message = try {
                    if (!errorBody.isNullOrEmpty()) {
                        JSONObject(errorBody).getString("detail")
                    } else getApplication<Application>().getString(R.string.err_registration_failed)
                } catch (jsonEx: Exception) {
                    getApplication<Application>().getString(R.string.err_registration_failed)
                }
                _authError.value = message
                onError(message)
            } catch (e: Exception) {
                val msg = e.message ?: getApplication<Application>().getString(R.string.err_registration_failed)
                _authError.value = msg
                onError(msg)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun verifyOtp(email: String, otp: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val request = OTPVerifyRequest(email, otp)
                val response = repository.verifyOtp(request)
                setAuthToken(response.access_token)
                _loginToken.value = response.access_token
                onSuccess(getApplication<Application>().getString(R.string.msg_email_verified))
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_otp_verification_failed))
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun requestPasswordReset(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val response = repository.requestPasswordReset(email)
                _registrationEmail.value = email // Track for the reset screen
                onSuccess(response.message)
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_request_reset))
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun resetPassword(otp: String, newPassword: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val request = ResetPasswordRequest(_registrationEmail.value, otp, newPassword)
                val response = repository.resetPassword(request)
                onSuccess(response.message)
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_reset_password))
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    // --- Auth Management Functions ---
    fun setAuthToken(token: String) {
        viewModelScope.launch {
            sessionManager.saveAuthToken(token) // <-- SAVE TO DEVICE
        }
        jwtToken = "Bearer $token"
        fetchUserProfile()
    }

    fun logout() {
        // Unregister this device from push with the JWT that is about to be discarded.
        // Best effort and independent of the local sign-out (offline or an already-expired
        // token must not block logging out).
        jwtToken?.let { bearer ->
            viewModelScope.launch { com.shin.depthflow.push.PushRegistrar.unregister(bearer) }
        }
        viewModelScope.launch {
            sessionManager.clearSession() // <-- DELETE FROM DEVICE
            jwtToken = null
            _credits.value = 0
            _userEmail.value = ""
            _userFullName.value = ""
            _userPlan.value = ""
            _userProfilePic.value = null
            _subscriptionStatus.value = "inactive"

            // Clear the new dummy dates
            _userJoinDate.value = ""
            _userPlanRenewalDate.value = ""
        }
    }

    // ==========================================
    // --- EXISTING PIPELINE STATES ---
    // ==========================================
    private val _uiState = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val uiState: StateFlow<PipelineState> = _uiState.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _motionPath = MutableStateFlow(application.getString(R.string.default_motion_path))
    val motionPath: StateFlow<String> = _motionPath.asStateFlow()

    private val _depthIntensity = MutableStateFlow(0.85f)
    val depthIntensity: StateFlow<Float> = _depthIntensity.asStateFlow()

    private val _localProjects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val localProjects: StateFlow<List<ProjectEntity>> = _localProjects.asStateFlow()

    // Add these below your credits state in DepthFlowViewModel.kt:
    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userFullName = MutableStateFlow("")
    val userFullName: StateFlow<String> = _userFullName.asStateFlow()

    private val _userPlan = MutableStateFlow("")
    val userPlan: StateFlow<String> = _userPlan.asStateFlow()

    private val _userProfilePic = MutableStateFlow<String?>(null)
    val userProfilePic: StateFlow<String?> = _userProfilePic.asStateFlow()

    private val _subscriptionStatus = MutableStateFlow("inactive")
    val subscriptionStatus: StateFlow<String> = _subscriptionStatus.asStateFlow()

    // --- NEW: History & Billing ---
    private val _generationLogs = MutableStateFlow<List<com.shin.depthflow.data.GenerationLog>>(emptyList())
    val generationLogs = _generationLogs.asStateFlow()

    private val _billingInfo = MutableStateFlow<com.shin.depthflow.data.BillingInfoResponse?>(null)
    val billingInfo = _billingInfo.asStateFlow()

    private val _paymentHistory = MutableStateFlow<List<PaymentHistoryItem>>(emptyList())
    val paymentHistory = _paymentHistory.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        checkInitialNetwork()
        observeNetworkChanges()
        observeLocalProjects()
        // Collect Auth Token from SessionManager
        viewModelScope.launch {
            sessionManager.authTokenFlow.collect { token ->
                _authToken.value = token?.takeIf { it.isNotEmpty() }
                if (!token.isNullOrEmpty()) {
                    jwtToken = "Bearer $token"
                    _isLoggedIn.value = true
                    fetchUserProfile()
                } else {
                    jwtToken = null
                    _isLoggedIn.value = false
                    // Reset profile data on logout/null token
                    _credits.value = 0
                    _userEmail.value = ""
                    _userFullName.value = ""
                    _userPlan.value = ""
                    _subscriptionStatus.value = "inactive"
                }
            }
        }
        // NEW: Collect Local Profile Pic from SessionManager
        viewModelScope.launch {
            sessionManager.profilePicFlow.collect { uri ->
                _userProfilePic.value = uri
            }
        }
    }

    // Inside DepthFlowViewModel.kt

    // 1. Add this new loading state variable near your other states
    private val _isProfileLoading = MutableStateFlow(true)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    // 2. Replace your fetchUserProfile function with this error-transparent version
    fun fetchUserProfile() {
        val token = jwtToken ?: return
        viewModelScope.launch {
            _isProfileLoading.value = true
            try {
                Logger.d("DepthFlowAuth", "🔄 Cold Start: Syncing profile with production...")
                val profile = repository.fetchUserProfile(token)

                _credits.value = profile.credits
                _userEmail.value = profile.email
                _userFullName.value = profile.full_name ?: getApplication<Application>().getString(R.string.default_user_name)
                _userPlan.value = profile.plan
                _subscriptionStatus.value = profile.subscription_status ?: "inactive"

                // Real Join Date parsing
                profile.created_at?.let { rawDate ->
                    _userJoinDate.value = formatJoinDate(rawDate)
                } ?: run {
                    _userJoinDate.value = "N/A"
                }

                // Parse Next Credit Drop Date (New from Python Model)
                profile.next_credit_drop_date?.let { rawDate ->
                    _nextCreditDropDate.value = formatRenewalDate(rawDate)
                }
                
                // NEW: Robustly handle potential subscription end date if status isn't just "active"
                if (_subscriptionStatus.value.contains("cancel", ignoreCase = true) || 
                    _subscriptionStatus.value.contains("end", ignoreCase = true)) {
                    profile.subscription_end_date?.let {
                        _userPlanRenewalDate.value = formatRenewalDate(it)
                    }
                }

                Logger.d("DepthFlowAuth", "✅ Profile synced: ${profile.email} (${profile.credits} Credits)")

                // Fetch logs and billing as well
                fetchGenerationLogs()
                fetchBillingInfo()

            } catch (e: retrofit2.HttpException) {
                Logger.e("DepthFlowAuth", "❌ Server rejected your JWT token! HTTP Code: ${e.code()}", e)
                if (e.code() == 401 || e.code() == 403) {
                    // Token has expired or is invalid, force user to log in again safely
                    logout()
                }
            } catch (e: Exception) {
                // This exposes missing internet permissions, SSL handshake failures, or JSON parsing exceptions
                Logger.e("DepthFlowAuth", "❌ Network Exception during cold start: ${e.message}", e)
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    fun fetchGenerationLogs() {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                val logs = repository.fetchGenerationLogs(token)
                _generationLogs.value = logs
            } catch (e: Exception) {
                Logger.e("DepthFlow", "Failed to fetch logs", e)
            }
        }
    }

    fun fetchBillingInfo() {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                val billing = repository.fetchBillingInfo(token)
                _billingInfo.value = billing
                
                // Real Renewal Date parsing
                val status = billing.platform.status
                val nextBilling = billing.platform.next_billing_date
                
                // Sync the detailed status to the UI
                _subscriptionStatus.value = status

                if (status.contains("Scheduled for cancellation", ignoreCase = true) || 
                    status.contains("canceled", ignoreCase = true) || 
                    status.contains("cancelled", ignoreCase = true)) {
                    
                    // Extract date from "Scheduled for cancellation on 2027-05-21"
                    val extractedDate = status.substringAfterLast(" ").trim().removeSuffix(".")
                    
                    if (extractedDate.length >= 10 && extractedDate.any { it.isDigit() }) {
                         _userPlanRenewalDate.value = formatRenewalDate(extractedDate)
                    } else if (!nextBilling.isNullOrEmpty() && nextBilling != "N/A") {
                        _userPlanRenewalDate.value = formatRenewalDate(nextBilling)
                    } else {
                        _userPlanRenewalDate.value = "N/A"
                    }
                } else if (!nextBilling.isNullOrEmpty() && nextBilling != "N/A") {
                    _userPlanRenewalDate.value = formatRenewalDate(nextBilling)
                } else {
                    _userPlanRenewalDate.value = "N/A"
                }
            } catch (e: Exception) {
                Logger.e("DepthFlow", "Failed to fetch billing", e)
            }
        }
    }

    fun fetchPaymentHistory() {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                _paymentHistory.value = repository.fetchPaymentHistory(token)
            } catch (e: Exception) {
                Logger.e("DepthFlow", "Failed to fetch history", e)
            }
        }
    }

    fun verifyPurchaseOnBackend(purchaseToken: String, productId: String) {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                Logger.d("BillingTest", "6. Verifying with backend: $productId")
                val response = repository.verifyPurchase(token, purchaseToken, productId)
                Logger.d("BillingTest", "7. Success! Status: ${response.status}, New Balance: ${response.new_balance}")
                
                // Update local states immediately if provided
                response.new_balance?.let { _credits.value = it }
                response.plan?.let { _userPlan.value = it }
                
                // Refresh the whole profile to get all new credit/plan details safely
                fetchUserProfile()
                fetchBillingInfo() // NEW: Also refresh billing info
                _purchaseVerified.tryEmit(Unit) // Happy Hour tokens are granted inside verify-purchase
            } catch (e: Exception) {
                Logger.e("BillingTest", "7. Backend verification FAILED: ${e.message}")
            }
        }
    }

    fun cancelSubscription(
        planType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                val response = repository.cancelSubscription(token, planType)
                _subscriptionStatus.value = response.new_status
                fetchUserProfile() // Refresh data
                fetchBillingInfo()
                onSuccess(response.message)
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_cancel_subscription))
            }
        }
    }

    fun resubscribeSubscription(
        planType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                val response = repository.resubscribeSubscription(token, planType)
                _subscriptionStatus.value = response.new_status
                fetchUserProfile() // Refresh data
                fetchBillingInfo()
                onSuccess(response.message)
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_resubscribe))
            }
        }
    }

    // --- UI Updaters ---
    fun setImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
        if (_uiState.value is PipelineState.Success || _uiState.value is PipelineState.Error) {
            _uiState.value = PipelineState.Idle
        }
    }

    fun setMotionPath(path: String) {
        _motionPath.value = path
    }

    fun setDepthIntensity(intensity: Float) {
        _depthIntensity.value = intensity
    }

    private fun checkInitialNetwork() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isNetworkAvailable.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun observeNetworkChanges() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
                if (isLoggedIn.value) fetchUserProfile()
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = false
            }
        })
    }

    private fun observeLocalProjects() {
        viewModelScope.launch {
            repository.getAllProjects().collect { projects ->
                _localProjects.value = projects
            }
        }
    }

    // 2. UPDATE THE FUNCTION SIGNATURE
    /**
     * @param skipCreditCheck true while Happy Hour is active: generation is free then, and the
     * server is the authority anyway (it answers 402 if the user is not actually entitled).
     */
    fun generate3DMotion(skipCreditCheck: Boolean = false) {
        val uri = _selectedImageUri.value
        val token = jwtToken

        if (token == null) {
            _uiState.value = PipelineState.Error(getApplication<Application>().getString(R.string.err_login_required))
            return
        }

        if (uri == null) {
            _uiState.value = PipelineState.Error(getApplication<Application>().getString(R.string.err_select_image_first))
            return
        }

        val duration = _lengthValue.value.toInt()

        // 3. MATCH PYTHON BACKEND COST DYNAMICS
        val dynamicCost = when {
            duration > 20 -> 30
            duration > 10 -> 25
            else -> 20
        }

        if (!skipCreditCheck && _credits.value < dynamicCost) {
            _uiState.value = PipelineState.Error(getApplication<Application>().getString(R.string.err_insufficient_credits, dynamicCost))
            logEvent("insufficient_credits", Bundle().apply {
                putInt("cost", dynamicCost)
                putInt("balance", _credits.value)
            })
            return
        }

        // AppScope, not viewModelScope: the request must survive leaving the screen. GenerationService
        // (started through GenerationNotifier) keeps the process alive meanwhile.
        AppScope.launch {
            logEvent("generate_3d_start", Bundle().apply {
                putString("style", _motionPath.value)
                putInt("duration", duration)
                putString("plan", _userPlan.value)
            })
            val renderSettings = mapOf(
                "duration" to duration,
                "fps" to _fpsValue.value.replace("p", "").toInt(),
                "ssaa" to _ssaaValue.value.replace("x", "").toDouble(),
                "quality" to when(_bokehQuality.value) {
                    "High (100)" -> 100
                    "Medium (80)" -> 80
                    else -> 50
                }
            )

            val motionSettings = mapOf(
                "style" to _motionPath.value,
                "amplitude" to _depthIntensity.value,
                "speed" to _speedValue.value,
                "focus" to _focusValue.value,
                "reverse" to _reverseMode.value,
                "smooth" to _smoothMode.value,
                "loop" to _loopMode.value
            )

            val effectsSettings = mapOf(
                "dof_enable" to (_cinematicIntensity.value > 0),
                "dof_intensity" to _cinematicIntensity.value
            )

            val app = getApplication<Application>()
            try {
            repository.processImage(
                imageUri = uri,
                token = token,
                renderSettings = renderSettings,
                motionSettings = motionSettings,
                effectsSettings = effectsSettings,
                plan = _userPlan.value.lowercase()
            ).collect { state ->
                _uiState.value = state
                // Progress notification (local: the backend sends no progress events).
                when (state) {
                    is PipelineState.Uploading -> GenerationNotifier.showProgress(app, GenerationNotifier.Stage.UPLOADING)
                    is PipelineState.Processing -> GenerationNotifier.showProgress(app, GenerationNotifier.Stage.RENDERING)
                    is PipelineState.Success -> GenerationNotifier.showSuccess(app)
                    is PipelineState.Error -> GenerationNotifier.showError(app, state.errorMessage)
                    PipelineState.Idle -> Unit
                }
                if (state is PipelineState.Success) {
                    logEvent("generate_3d_success", Bundle().apply {
                        putString("style", state.motionPath)
                        putFloat("amplitude", state.amplitude)
                    })
                    state.remainingCredits?.let {
                        _credits.value = it
                    } ?: fetchUserProfile()
                } else if (state is PipelineState.Error) {
                    logEvent("generate_3d_error", Bundle().apply {
                        putString("message", state.errorMessage)
                    })
                }
            }
            } finally {
                // Cancelled (e.g. the ViewModel was cleared) or the flow ended without a result:
                // never leave a spinning notification behind. A no-op after Success/Error.
                GenerationNotifier.finish(app)
            }
        }
    }

    // ==========================================
    // --- NEW: DUMMY UI STATES FOR PROFILE ---
    // ==========================================

    fun deleteLocalProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun updateProfile(
        fullName: String?,
        email: String?,
        imageUri: Uri?,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                // Update Name and Email on backend
                val updatedProfile = repository.updateProfile(token, fullName, email, null)
                
                // Update local states
                _userFullName.value = updatedProfile.full_name ?: ""
                _userEmail.value = updatedProfile.email
                _credits.value = updatedProfile.credits
                _userPlan.value = updatedProfile.plan
                
                // Save image URI locally only
                imageUri?.let {
                    sessionManager.saveProfilePic(it.toString())
                }
                
                onSuccess(getApplication<Application>().getString(R.string.msg_profile_updated))
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_update_profile))
            }
        }
    }

    fun changePassword(
        request: ChangePasswordRequest,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = jwtToken ?: return
        viewModelScope.launch {
            try {
                repository.changePassword(token, request)
                onSuccess(getApplication<Application>().getString(R.string.msg_password_changed))
            } catch (e: Exception) {
                onError(e.message ?: getApplication<Application>().getString(R.string.err_failed_change_password))
            }
        }
    }

    private val _userJoinDate = MutableStateFlow("")
    val userJoinDate: StateFlow<String> = _userJoinDate.asStateFlow()

    private val _userPlanRenewalDate = MutableStateFlow("")
    val userPlanRenewalDate: StateFlow<String> = _userPlanRenewalDate.asStateFlow()

    private val _nextCreditDropDate = MutableStateFlow("")
    val nextCreditDropDate: StateFlow<String> = _nextCreditDropDate.asStateFlow()

    private fun formatJoinDate(rawDate: String): String {
        if (rawDate.isEmpty() || rawDate == "N/A") return "N/A"
        return try {
            // Attempt to handle both ISO (with T) and simple YYYY-MM-DD
            val dateStr = if (rawDate.contains("T")) rawDate.substringBefore("T") else rawDate
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            Logger.e("DateParsing", "Failed to parse Join Date: $rawDate", e)
            rawDate
        }
    }

    private fun formatRenewalDate(rawDate: String): String {
        if (rawDate.isEmpty() || rawDate == "N/A") return "N/A"
        return try {
            val dateStr = if (rawDate.contains("T")) rawDate.substringBefore("T") else rawDate
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            Logger.e("DateParsing", "Failed to parse Renewal Date: $rawDate", e)
            rawDate
        }
    }

    // --- Dummy Functions ---
    // You can call these to simulate data updates, or eventually
    // replace them by parsing these dates from your repository's profile object.

    fun loadDummyProfileDates() {
        // No longer needed as we use real data
    }

    private val _activeSettingsTab = MutableStateFlow(application.getString(R.string.default_settings_tab))
    val activeSettingsTab = _activeSettingsTab.asStateFlow()

    private val _shouldScrollToSettings = MutableStateFlow(false)
    val shouldScrollToSettings = _shouldScrollToSettings.asStateFlow()

    fun setScrollTrigger(shouldScroll: Boolean) {
        _shouldScrollToSettings.value = shouldScroll
    }

    fun setActiveSettingsTab(tab: String) {
        _activeSettingsTab.value = tab
        _shouldScrollToSettings.value = true
    }

    // --- NEW: Workspace Settings States ---
    private val _speedValue = MutableStateFlow(1f)
    val speedValue = _speedValue.asStateFlow()
    fun setSpeedValue(value: Float) { _speedValue.value = value }

    private val _lengthValue = MutableStateFlow(5f)
    val lengthValue = _lengthValue.asStateFlow()
    fun setLengthValue(value: Float) { _lengthValue.value = value }

    private val _ssaaValue = MutableStateFlow(application.getString(R.string.default_ssaa))
    val ssaaValue = _ssaaValue.asStateFlow()
    fun setSsaaValue(value: String) { _ssaaValue.value = value }

    private val _fpsValue = MutableStateFlow(application.getString(R.string.default_fps))
    val fpsValue = _fpsValue.asStateFlow()
    fun setFpsValue(value: String) { _fpsValue.value = value }

    private val _focusValue = MutableStateFlow(0f)
    val focusValue = _focusValue.asStateFlow()
    fun setFocusValue(value: Float) { _focusValue.value = value }

    private val _cinematicIntensity = MutableStateFlow(1f)
    val cinematicIntensity = _cinematicIntensity.asStateFlow()
    fun setCinematicIntensity(value: Float) { _cinematicIntensity.value = value }

    private val _bokehQuality = MutableStateFlow(application.getString(R.string.default_bokeh_quality))
    val bokehQuality = _bokehQuality.asStateFlow()
    fun setBokehQuality(value: String) { _bokehQuality.value = value }

    // --- NEW: Independent Motion Modes ---
    private val _reverseMode = MutableStateFlow(false)
    val reverseMode = _reverseMode.asStateFlow()
    fun setReverseMode(value: Boolean) { _reverseMode.value = value }

    private val _smoothMode = MutableStateFlow(true)
    val smoothMode = _smoothMode.asStateFlow()
    fun setSmoothMode(value: Boolean) { _smoothMode.value = value }

    private val _loopMode = MutableStateFlow(true)
    val loopMode = _loopMode.asStateFlow()
    fun setLoopMode(value: Boolean) { _loopMode.value = value }
}








