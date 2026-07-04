package com.shin.depthflow.util

import android.util.Log
import com.shin.depthflow.BuildConfig

object Logger {
    private const val TAG = "DepthFlow"

    fun d(tag: String = TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }

    fun i(tag: String = TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun w(tag: String = TAG, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }
}
