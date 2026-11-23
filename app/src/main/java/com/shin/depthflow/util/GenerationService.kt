package com.shin.depthflow.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service that keeps the process alive while a 3D generation request is in flight
 * (the server can take ~5 minutes), so leaving the app does not kill the upload or the render.
 *
 * It does no work itself: the request runs in [AppScope]. This service only owns the ongoing
 * progress notification ([GenerationNotifier]) and stops when the pipeline reports a result.
 * Type `dataSync` (the app uploads an image and downloads the rendered video).
 */
class GenerationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val safetyStop = Runnable { stopNow() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stage = intent?.getStringExtra(EXTRA_STAGE)
            ?.let { runCatching { GenerationNotifier.Stage.valueOf(it) }.getOrNull() }
            ?: GenerationNotifier.Stage.UPLOADING

        // Must be called promptly after startForegroundService().
        ServiceCompat.startForeground(
            this,
            GenerationNotifier.PROGRESS_ID,
            GenerationNotifier.buildProgress(this, stage),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        )
        // The generation already ended before the service got here: startForeground() was still
        // required (or the system crashes the app), so stop right away.
        if (!wanted) {
            stopNow()
            return START_NOT_STICKY
        }
        // Never outlive a stuck request: the HTTP read timeout is 5 minutes.
        handler.removeCallbacks(safetyStop)
        handler.postDelayed(safetyStop, SAFETY_TIMEOUT_MS)
        // If the system kills the process, the request is gone too: do not restart.
        return START_NOT_STICKY
    }

    /** Android 15+: the system's dataSync time limit was reached. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopNow()
    }

    override fun onDestroy() {
        handler.removeCallbacks(safetyStop)
        if (running === this) running = null
        super.onDestroy()
    }

    private fun stopNow() {
        handler.removeCallbacks(safetyStop)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val EXTRA_STAGE = "stage"
        private const val SAFETY_TIMEOUT_MS = 7 * 60 * 1000L

        private val mainHandler = Handler(Looper.getMainLooper())

        /** The live instance, so stop() works without startService() (which the system may refuse in the background). */
        @Volatile
        private var running: GenerationService? = null

        /** False once the generation ended; a service that starts late checks it and stops at once. */
        @Volatile
        private var wanted = false

        /** Starts the service, or updates its notification if it is already running. */
        fun start(context: Context, stage: GenerationNotifier.Stage) {
            wanted = true
            val intent = Intent(context, GenerationService::class.java).putExtra(EXTRA_STAGE, stage.name)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException if somehow started from the
                // background. The generation still runs; only the keep-alive is lost.
                Logger.e(message = "GenerationService start failed: ${e.message}")
                GenerationNotifier.notifyProgress(context, stage)
            }
        }

        fun stop() {
            wanted = false
            mainHandler.post { running?.stopNow() }
        }
    }
}
