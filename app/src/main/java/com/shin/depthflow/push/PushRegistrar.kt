package com.shin.depthflow.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.shin.depthflow.BuildConfig
import com.shin.depthflow.data.ApiClient
import com.shin.depthflow.data.DeviceRequest
import com.shin.depthflow.data.DeviceUnregisterRequest
import com.shin.depthflow.data.SessionManager
import com.shin.depthflow.util.Logger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Registers/unregisters this device's FCM token with the backend. Every call is best effort:
 * failures are logged and never block login or logout (the backend also disables dead tokens,
 * and we re-register on every login and app start).
 */
object PushRegistrar {

    /** Current FCM registration token, or null if Firebase is unavailable. */
    suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (cont.isActive) cont.resume(if (task.isSuccessful) task.result else null)
            }
        } catch (e: Exception) {
            Logger.e("Push", "FCM token unavailable", e)
            if (cont.isActive) cont.resume(null)
        }
    }

    /** POST /notifications/devices. Safe to call repeatedly. [bearer] is "Bearer <jwt>". */
    suspend fun register(bearer: String, token: String? = null) {
        try {
            val fcmToken = token ?: currentToken() ?: return
            ApiClient.service?.registerDevice(
                bearer,
                DeviceRequest(token = fcmToken, app_version = BuildConfig.VERSION_NAME)
            )
            Logger.d("Push", "Device token registered")
        } catch (e: Exception) {
            Logger.e("Push", "Device registration failed: ${e.message}", e)
        }
    }

    /**
     * Logout: POST /notifications/devices/unregister with the JWT being discarded, then drop the
     * local FCM token so it cannot be delivered to the next account on this phone.
     */
    suspend fun unregister(bearer: String) {
        try {
            val fcmToken = currentToken()
            if (fcmToken != null) {
                ApiClient.service?.unregisterDevice(bearer, DeviceUnregisterRequest(fcmToken))
            }
        } catch (e: Exception) {
            Logger.e("Push", "Device unregister failed: ${e.message}", e)
        }
        try {
            FirebaseMessaging.getInstance().deleteToken()
        } catch (e: Exception) {
            Logger.e("Push", "deleteToken failed", e)
        }
    }

    /** Registers using the persisted session, for contexts without a ViewModel (e.g. onNewToken). */
    suspend fun registerFromSession(context: Context, newToken: String) {
        val jwt = SessionManager(context.applicationContext).authTokenFlow.firstOrNull()
        if (!jwt.isNullOrEmpty()) register("Bearer $jwt", newToken)
    }
}
