package com.shin.depthflow.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide scope for work that must outlive a screen or ViewModel, such as a 3D generation
 * request that keeps running (under [GenerationService]) after the user leaves the app.
 */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
