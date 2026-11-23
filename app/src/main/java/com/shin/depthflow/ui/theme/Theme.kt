package com.shin.depthflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 1. Map your brand colors to the Material 3 Dark Scheme
private val DepthFlowColorScheme = darkColorScheme(
    primary = DepthFlowPurple,
    secondary = DepthFlowBlue,
    tertiary = DepthFlowLightPurple,

    background = BackgroundDark,
    surface = SurfaceDark,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun DepthflowTheme(
    // Force dark mode logic out; we always want our custom dark theme
    darkTheme: Boolean = true,
    // 2. Disable dynamic color by default so user wallpapers don't break the UI
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use the custom DepthFlow scheme
    val colorScheme = DepthFlowColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Standard edge-to-edge handling is now managed in MainActivity via enableEdgeToEdge()
            // Android 15 (SDK 35+) automatically handles bar colors and icon appearance.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Make sure your Type.kt is set up
        content = content
    )
}







