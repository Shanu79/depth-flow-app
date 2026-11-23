package com.shin.depthflow.ui.promo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.shin.depthflow.data.PromoItem
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark

/** Null-safe hex parser: an invalid or null colour falls back to the caller's default. */
fun parsePromoColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex.trim()))
    } catch (e: Exception) {
        null
    }
}

/** Black or white, whichever reads better on this colour. */
fun Color.contrastingText(): Color = if (luminance() > 0.5f) Color.Black else Color.White

@Immutable
class PromoPalette(
    val bg: Color,
    val bg2: Color?,
    val text: Color,
    val accent: Color,
    val button: Color
) {
    val brush: Brush get() = Brush.verticalGradient(listOf(bg, bg2 ?: bg))
    val buttonText: Color get() = button.contrastingText()
}

fun PromoItem.palette() = PromoPalette(
    bg = parsePromoColor(bg_color) ?: SurfaceDark,
    bg2 = parsePromoColor(bg_color2),
    text = parsePromoColor(text_color) ?: Color.White,
    accent = parsePromoColor(accent_color) ?: Color(0xFFF59E0B),
    button = parsePromoColor(button_color) ?: DepthFlowPurple
)

fun promoFontFamily(style: String?): FontFamily = when (style) {
    "elegant" -> FontFamily.Serif
    else -> FontFamily.SansSerif
}

fun promoTitleWeight(style: String?): FontWeight = when (style) {
    "bold" -> FontWeight.Black
    "elegant" -> FontWeight.SemiBold
    else -> FontWeight.Bold
}

/**
 * Link rules shared by popup, banner and What's New.
 * `https://` opens externally, `/path` is an in-app path from the web app, anything else is ignored.
 * Returns true when the host surface should close afterwards (in-app, empty or unknown link).
 */
object LinkRouter {
    fun handle(context: Context, link: String?, onInAppPath: (String) -> Unit): Boolean {
        val target = link?.trim().orEmpty()
        return when {
            target.startsWith("https://") -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    // No browser available: ignore.
                }
                false
            }
            target.startsWith("/") -> {
                try { onInAppPath(target) } catch (e: Exception) { /* unknown path: just close */ }
                true
            }
            else -> true
        }
    }
}
