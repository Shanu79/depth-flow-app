package com.shin.depthflow.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shin.depthflow.MainActivity
import com.shin.depthflow.R
import com.shin.depthflow.push.PushTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Local notifications for a 3D generation. The backend sends no progress events (generation is one
 * long request), so the stages come from the app's own pipeline: preparing, then rendering, then
 * done or failed.
 *
 * - Progress: ongoing and silent; the notification of [GenerationService], the foreground service
 *   that keeps the process alive during the render.
 *   - Android 16+: a progress-centric notification (`Notification.ProgressStyle`) with three
 *     segments (Preparing, Rendering, Finishing). On 16 QPR1 (API 36.1)+ it also requests promotion
 *     to a Live Update (status-bar chip, top of the shade). Because the server reports no
 *     percentage, the tracker moves on an **estimate** from elapsed time, easing toward the end of
 *     the Rendering segment and never reaching it before the server answers. The chip shows the
 *     real elapsed time.
 *   - Older versions: an indeterminate progress bar with elapsed time.
 * - Result: only posted when the app is in the background (in the app, Workspace shows it).
 * Tapping any of them opens Workspace.
 */
object GenerationNotifier {
    private const val PROGRESS_CHANNEL = "generation_progress"
    private const val RESULT_CHANNEL = "generation_result"
    const val PROGRESS_ID = 4201
    private const val RESULT_ID = 4202

    // Only for the fallback (service could not start): if the process dies mid-render, the ongoing
    // notification removes itself instead of spinning forever. The service has its own safety stop.
    private const val PROGRESS_TIMEOUT_MS = 6 * 60 * 1000L

    // --- ProgressStyle layout: 100 units split into the three real stages. ---
    private const val PREPARE_END = 10
    private const val RENDER_END = 90
    private const val PROGRESS_MAX = 100
    // Estimate curve, tuned to measured production renders of ~21 s and ~31 s: about 69% through the
    // Rendering segment at 21 s, 82% at 31 s, 96% at 60 s. It never reaches the Finishing point.
    private const val RENDER_TIME_CONSTANT_MS = 18_000.0
    private const val TICK_MS = 2_000L

    private const val COLOR_PREPARE = 0xFF3B82F6.toInt() // blue
    private const val COLOR_RENDER = 0xFF9034EA.toInt()  // DepthFlowPurple
    private const val COLOR_FINISH = 0xFF22C55E.toInt()  // green

    /** Set by MainActivity onStart/onStop. */
    @Volatile
    var appInForeground: Boolean = false

    private var startedAt: Long = 0L
    private var renderStartedAt: Long = 0L
    private var currentStage: Stage = Stage.UPLOADING
    private var ticker: Job? = null

    enum class Stage { UPLOADING, RENDERING }

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(PROGRESS_CHANNEL, "3D generation progress", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows while your 3D video is being created"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(RESULT_CHANNEL, "3D generation results", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when your 3D video is ready or if it failed"
            }
        )
    }

    /** Starts (or updates) the foreground service, which shows the progress notification. */
    fun showProgress(context: Context, stage: Stage) {
        val now = System.currentTimeMillis()
        if (stage == Stage.UPLOADING || startedAt == 0L) startedAt = now
        if (stage == Stage.RENDERING && currentStage != Stage.RENDERING) renderStartedAt = now
        currentStage = stage
        manager(context).cancel(RESULT_ID)
        GenerationService.start(context, stage)
        if (stage == Stage.RENDERING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            startTicker(context.applicationContext)
        }
    }

    /** Fallback when the service cannot start: a plain ongoing notification that times out. */
    internal fun notifyProgress(context: Context, stage: Stage) {
        if (!canNotify(context)) return
        manager(context).notify(PROGRESS_ID, buildProgress(context, stage, timeoutMs = PROGRESS_TIMEOUT_MS))
    }

    internal fun buildProgress(context: Context, stage: Stage, timeoutMs: Long? = null): Notification {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            buildProgressCentric(context, stage, timeoutMs)
        } else {
            buildLegacyProgress(context, stage, timeoutMs)
        }
    }

    private fun texts(stage: Stage): Pair<String, String> = when (stage) {
        Stage.UPLOADING -> "Preparing your 3D video…" to "Uploading your image"
        Stage.RENDERING -> "Rendering your 3D video…" to "Usually under a minute. You can leave the app."
    }

    /** Android 16+: progress-centric notification, requested as a Live Update on 36.1+. */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildProgressCentric(context: Context, stage: Stage, timeoutMs: Long?): Notification {
        val (title, text) = texts(stage)
        val style = Notification.ProgressStyle()
            .setStyledByProgress(true) // segments ahead of the tracker are drawn faded
            .setProgressSegments(
                listOf(
                    Notification.ProgressStyle.Segment(PREPARE_END).setColor(COLOR_PREPARE),
                    Notification.ProgressStyle.Segment(RENDER_END - PREPARE_END).setColor(COLOR_RENDER),
                    Notification.ProgressStyle.Segment(PROGRESS_MAX - RENDER_END).setColor(COLOR_FINISH)
                )
            )
            .setProgressPoints(
                listOf(
                    Notification.ProgressStyle.Point(PREPARE_END).setColor(COLOR_RENDER),
                    Notification.ProgressStyle.Point(RENDER_END).setColor(COLOR_FINISH)
                )
            )
            .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_stat_generation))
            .setProgress(estimatedProgress(stage))

        val builder = Notification.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_generation)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Elapsed time: shown in the notification and used as the status-bar chip text.
            .setWhen(startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openWorkspace(context))
        if (timeoutMs != null) builder.setTimeoutAfter(timeoutMs)
        if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
            // Ask for Live Update treatment. The system decides (the user can turn it off per app;
            // NotificationManager.canPostPromotedNotifications() reports it). Without it, this is
            // still a normal progress-centric notification.
            builder.setRequestPromotedOngoing(true)
        }
        return builder.build()
    }

    /** Before Android 16: indeterminate bar with elapsed time. */
    private fun buildLegacyProgress(context: Context, stage: Stage, timeoutMs: Long?): Notification {
        val (title, text) = texts(stage)
        return NotificationCompat.Builder(context, PROGRESS_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_generation)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setWhen(startedAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .apply { if (timeoutMs != null) setTimeoutAfter(timeoutMs) }
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            // Show immediately instead of the system's up-to-10 s deferral for new services.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openWorkspace(context))
            .build()
    }

    /**
     * Estimated tracker position. Preparing sits inside the first segment; Rendering eases from the
     * start of its segment toward (never reaching) its end. Only the server's answer completes it.
     */
    private fun estimatedProgress(stage: Stage): Int = when (stage) {
        Stage.UPLOADING -> PREPARE_END / 2
        Stage.RENDERING -> {
            val elapsed = (System.currentTimeMillis() - renderStartedAt).coerceAtLeast(0L)
            val fraction = 1.0 - exp(-elapsed / RENDER_TIME_CONSTANT_MS)
            val span = (RENDER_END - PREPARE_END) - 2 // stop 2 units short of the Finishing point
            PREPARE_END + (span * fraction).roundToInt()
        }
    }

    /** Re-posts the progress notification every few seconds so the tracker moves during the render. */
    private fun startTicker(context: Context) {
        if (ticker?.isActive == true) return
        ticker = AppScope.launch {
            while (isActive && currentStage == Stage.RENDERING && startedAt != 0L) {
                delay(TICK_MS)
                if (startedAt == 0L || !canNotify(context)) break
                // Same id as the foreground service's notification: this updates it in place.
                manager(context).notify(PROGRESS_ID, buildProgress(context, Stage.RENDERING))
            }
        }
    }

    fun showSuccess(context: Context) {
        finish(context)
        if (appInForeground || !canNotify(context)) return
        val notification = NotificationCompat.Builder(context, RESULT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_generation)
            .setContentTitle("Your 3D video is ready")
            .setContentText("Tap to watch, save or share it.")
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openWorkspace(context))
            .build()
        manager(context).notify(RESULT_ID, notification)
    }

    fun showError(context: Context, message: String?) {
        finish(context)
        if (appInForeground || !canNotify(context)) return
        val body = message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
        val notification = NotificationCompat.Builder(context, RESULT_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("3D video generation failed")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(openWorkspace(context))
            .build()
        manager(context).notify(RESULT_ID, notification)
    }

    /** Stops the ticker and the service and removes the progress notification. */
    fun finish(context: Context) {
        ticker?.cancel()
        ticker = null
        startedAt = 0L
        renderStartedAt = 0L
        currentStage = Stage.UPLOADING
        GenerationService.stop()
        manager(context).cancel(PROGRESS_ID)
    }

    private fun openWorkspace(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(PushTarget.EXTRA_ROUTE, PushTarget.ROUTE_WORKSPACE)
        }
        return PendingIntent.getActivity(
            context, PROGRESS_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
}
