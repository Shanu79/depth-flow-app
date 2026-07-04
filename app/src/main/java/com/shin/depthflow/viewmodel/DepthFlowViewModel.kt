package com.shin.depthflow.viewmodel

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
import org.json.JSONObject
import retrofit2.HttpException


class DepthFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DepthFlowRepository(application)
    private val sessionManager = SessionManager(application) // <-- ADD THIS

    // ==========================================
    // --- NEW: AUTH & CREDITS STATE ---
    // ==========================================
    private var jwtToken: String? = null

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
                if (e.code() == 403) _authError.value = "Email not verified."
                else if (e.code() == 401) _authError.value = "Incorrect email or password."
                else _authError.value = "Server error. Please try again."
            } catch (e: Exception) {
                // ADD THIS LINE TO EXPOSE THE REAL ERROR:
                Logger.e("NetworkDebug", "Retrofit Failed: ${e.message}", e)

                _authError.value = "Network error. Check your connection."
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
                _authError.value = "Google login failed. Please try again."
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
                    } else "Registration failed"
                } catch (jsonEx: Exception) {
                    "Registration failed"
                }
                _authError.value = message
                onError(message)
            } catch (e: Exception) {
                val msg = e.message ?: "Registration failed"
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
                onSuccess("Email verified successfully")
            } catch (e: Exception) {
                onError(e.message ?: "OTP Verification failed")
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
                onError(e.message ?: "Failed to request reset")
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
                onError(e.message ?: "Failed to reset password")
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

    private val _motionPath = MutableStateFlow("Circular Orbit")
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
                _userFullName.value = profile.full_name ?: "DepthFlow User"
                _userPlan.value = profile.plan
                _subscriptionStatus.value = profile.subscription_status ?: "inactive"

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
                Logger.d("BillingTest", "7. Success! Granted: ${response.credits_granted}")
                
                // Refresh the whole profile to get the new credit balance immediately
                fetchUserProfile()
                fetchBillingInfo() // NEW: Also refresh billing info
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
                onError(e.message ?: "Failed to cancel subscription")
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
                onError(e.message ?: "Failed to resubscribe")
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
    fun generate3DMotion() {
        val uri = _selectedImageUri.value
        val token = jwtToken

        if (token == null) {
            _uiState.value = PipelineState.Error("Please log in to generate 3D images.")
            return
        }

        if (uri == null) {
            _uiState.value = PipelineState.Error("Please select a 2D source image first.")
            return
        }

        val duration = _lengthValue.value.toInt()

        // 3. MATCH PYTHON BACKEND COST DYNAMICS
        val dynamicCost = when {
            duration > 20 -> 30
            duration > 10 -> 25
            else -> 20
        }

        if (_credits.value < dynamicCost) {
            _uiState.value = PipelineState.Error("Insufficient credits. You need at least $dynamicCost credits.")
            return
        }

        viewModelScope.launch {
            val renderSettings = mapOf(
                "duration" to duration,
                "fps" to _fpsValue.value.replace("p", "").toInt(),
                "ssaa" to _ssaaValue.value.replace("x", "").toDouble(),
                "bokeh_quality" to _bokehQuality.value
            )

            val motionSettings = mapOf(
                "style" to _motionPath.value,
                "intensity" to _depthIntensity.value,
                "speed" to _speedValue.value,
                "focus" to _focusValue.value,
                "cinematic_intensity" to _cinematicIntensity.value,
                "mode" to _motionMode.value
            )

            repository.processImage(
                imageUri = uri,
                token = token,
                renderSettings = renderSettings,
                motionSettings = motionSettings
            ).collect { state ->
                _uiState.value = state
                if (state is PipelineState.Success) fetchUserProfile()
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
                
                onSuccess("Profile updated successfully")
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update profile")
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
                onSuccess("Password changed successfully")
            } catch (e: Exception) {
                onError(e.message ?: "Failed to change password")
            }
        }
    }

    private val _userJoinDate = MutableStateFlow("May 2024") // Dummy default
    val userJoinDate: StateFlow<String> = _userJoinDate.asStateFlow()

    private val _userPlanRenewalDate = MutableStateFlow("25 May 2025") // Dummy default
    val userPlanRenewalDate: StateFlow<String> = _userPlanRenewalDate.asStateFlow()

    // --- Dummy Functions ---
    // You can call these to simulate data updates, or eventually
    // replace them by parsing these dates from your repository's profile object.

    fun loadDummyProfileDates() {
        viewModelScope.launch {
            // Simulating parsing/fetching dates
            _userJoinDate.value = "May 2024"
            _userPlanRenewalDate.value = "25 May 2025"
        }
    }

    private val _activeSettingsTab = MutableStateFlow("Basic")
    val activeSettingsTab = _activeSettingsTab.asStateFlow()

    fun setActiveSettingsTab(tab: String) {
        _activeSettingsTab.value = tab
    }

    // --- NEW: Workspace Settings States ---
    private val _speedValue = MutableStateFlow(1f)
    val speedValue = _speedValue.asStateFlow()
    fun setSpeedValue(value: Float) { _speedValue.value = value }

    private val _lengthValue = MutableStateFlow(5f)
    val lengthValue = _lengthValue.asStateFlow()
    fun setLengthValue(value: Float) { _lengthValue.value = value }

    private val _ssaaValue = MutableStateFlow("1.0x")
    val ssaaValue = _ssaaValue.asStateFlow()
    fun setSsaaValue(value: String) { _ssaaValue.value = value }

    private val _fpsValue = MutableStateFlow("30p")
    val fpsValue = _fpsValue.asStateFlow()
    fun setFpsValue(value: String) { _fpsValue.value = value }

    private val _cameraStyle = MutableStateFlow("Dolly")
    val cameraStyle = _cameraStyle.asStateFlow()
    fun setCameraStyle(value: String) { _cameraStyle.value = value }

    private val _focusValue = MutableStateFlow(0f)
    val focusValue = _focusValue.asStateFlow()
    fun setFocusValue(value: Float) { _focusValue.value = value }

    private val _cinematicIntensity = MutableStateFlow(1f)
    val cinematicIntensity = _cinematicIntensity.asStateFlow()
    fun setCinematicIntensity(value: Float) { _cinematicIntensity.value = value }

    private val _bokehQuality = MutableStateFlow("Low (50)")
    val bokehQuality = _bokehQuality.asStateFlow()
    fun setBokehQuality(value: String) { _bokehQuality.value = value }

    private val _motionMode = MutableStateFlow("Loop")
    val motionMode = _motionMode.asStateFlow()
    fun setMotionMode(value: String) { _motionMode.value = value }
}








