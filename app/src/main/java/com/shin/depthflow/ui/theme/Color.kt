package com.shin.depthflow.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// --- Brand & Accent Colors ---
val DepthFlowPurple = Color(0xFF9034EA)
val DepthFlowLightPurple = Color(169, 91, 243) // Used in the Nav Plus button glow
val DepthFlowBlue = Color(0xFF5544E5)
val DepthFlowCyan = Color(35, 211, 238)        // Used in the Feature Card icon gradient

// --- Background & Surface Colors ---
val Surface = Color(6, 0, 10)
val BackgroundDark = Color(0xFF0B0B14)       // Deepest dark for background
val SurfaceDark = Color(0xFF151529)          // Slightly lighter for standard cards

val CardSurfaceTop = Color(0xFF1E1C38) // Matching the glassmorphism feel
val CardSurfaceBottom = Color(0xFF0F0F1B)

val BlendGlowColor = Color(0xFF9034EA)

val navBarBackground = Color(19, 15, 40)

// --- Borders & Strokes ---
val Border = Color(0xFF4C3B8C)
val BorderDark = Color(0xFF1D1442).copy(alpha = 0.3f)// Used in the Nav outer wrapper
val BorderHighlight = Color(0x1D5B5A8A)      // Used for the glassmorphic light-catch on cards

// --- Text & Icon Colors ---
val TextPrimary = Color.White
val TextMuted = Color(0xFFAAAAC0)
val TextSecondary = Color(0xFF9E9EB8)        // Cool-toned gray for card descriptions
val IconInactive = Color(0xFF7A7A9D)
val WarningRed = Color(0xFFFF4B4B)


val sectionContainerOuter = Color(29, 24, 73)
val sectionContainerInside  = Color(12, 13, 41)
// Outer container top-left subtle catch-light gradient
val sectionBorderBrush = Brush.linearGradient(
    colors = listOf(
        Border,
        BorderDark,
        Color.Transparent
    )
)

val cardBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF100C24).copy(0.3f),
        Color(0xFF080614).copy(0.7f)
    )
)

// Re-declared brushes assuming your theme colors exist in the file
val cardBackgroundSolidBrush = Brush.linearGradient(colors = listOf(CardSurfaceTop.copy(alpha = 0.35f), CardSurfaceBottom.copy(alpha = 0.35f)))







