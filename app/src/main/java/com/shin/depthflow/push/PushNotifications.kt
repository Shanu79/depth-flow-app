package com.shin.depthflow.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shin.depthflow.MainActivity
import com.shin.depthflow.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Where a tapped push should take the user. Built from the intent extras FCM / our notification carry. */
data class PushTarget(val route: String, val notificationId: Long?) {
    companion object {
        const val EXTRA_ROUTE = "route"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        /** Local 3D-generation notifications open Workspace. */
        const val ROUTE_WORKSPACE = "workspace"

        /** Background FCM messages put their `data` entries into the launcher intent extras. */
        fun from(intent: Intent?): PushTarget? = from(intent?.extras)

        fun from(extras: Bundle?): PushTarget? {
            val route = extras?.getString(EXTRA_ROUTE)?.takeIf { it.isNotBlank() } ?: return null
            return PushTarget(route, extras.getString(EXTRA_NOTIFICATION_ID)?.toLongOrNull())
        }
    }
}

/** Process-wide "a push arrived while the app is running" signal. The UI reacts by refetching state. */
object PushEvents {
    private val _received = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val received: SharedFlow<Unit> = _received.asSharedFlow()

    fun notifyReceived() {
        _received.tryEmit(Unit)
    }
}

object PushNotifications {
    /** Must match the `channel_id` the backend sets on every push. */
    const val CHANNEL_ID = "happy_hour"
    private const val CHANNEL_NAME = "Happy Hour"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Happy Hour availability and session updates"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /**
     * Shows a heads-up notification. Used when a message arrives while the app is in the foreground
     * (the system shows background messages itself). `data` values are all strings in a push.
     */
    fun show(context: Context, title: String?, body: String?, data: Map<String, String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // no permission: the in-app inbox still has the item

        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            data[PushTarget.EXTRA_ROUTE]?.let { putExtra(PushTarget.EXTRA_ROUTE, it) }
            data[PushTarget.EXTRA_NOTIFICATION_ID]?.let { putExtra(PushTarget.EXTRA_NOTIFICATION_ID, it) }
        }
        val notificationKey = data[PushTarget.EXTRA_NOTIFICATION_ID]?.toIntOrNull() ?: System.currentTimeMillis().toInt()
        val pending = PendingIntent.getActivity(
            context, notificationKey, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_happy_hour)
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(notificationKey, notification)
    }
}
