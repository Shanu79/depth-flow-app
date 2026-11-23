package com.shin.depthflow.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DepthFlowMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Token created or rotated. Register it when logged in (otherwise login registers it). */
    override fun onNewToken(token: String) {
        scope.launch { PushRegistrar.registerFromSession(applicationContext, token) }
    }

    /**
     * Called for every message while the app is in the foreground. In the background the system
     * shows the `notification` part itself on channel `happy_hour`, and a tap delivers `data` as
     * launcher-intent extras (handled in MainActivity).
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        PushNotifications.show(
            applicationContext,
            title = notification?.title ?: message.data["title"],
            body = notification?.body ?: message.data["body"],
            data = message.data
        )
        // Let any running UI refetch /happy-hour/status and the unread badge.
        PushEvents.notifyReceived()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
