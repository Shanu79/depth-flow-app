package com.shin.depthflow.ui.promo

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val x: Float,      // 0..1 across the width
    val phase: Float,  // 0..1 start offset
    val speed: Float,  // screens per second
    val size: Float,   // 0..1 relative size
    val color: Int     // index into a small palette
)

private val confettiColors = listOf(
    Color(0xFFF59E0B), Color(0xFF7C3AED), Color(0xFF22C55E), Color(0xFFEF4444), Color(0xFF38BDF8)
)

/** True when the user turned animations off (system "remove animations" / animator scale 0). */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Decorative animated layer drawn behind the content. Non-interactive and cheap: at most ~28
 * particles on a single Canvas. Renders a still frame when animations are turned off.
 */
@Composable
fun PromoDecor(theme: String?, emoji: String?, accent: Color, modifier: Modifier = Modifier) {
    val kind = theme?.trim()?.lowercase()
    if (kind.isNullOrEmpty() || kind == "none") return
    if (kind !in setOf("snow", "lights", "confetti", "sparkles", "rain")) return

    val reduceMotion = rememberReduceMotion()
    val count = when (kind) { "lights" -> 14; "sparkles" -> 14; "rain" -> 16; else -> 28 }
    val particles = remember(kind) {
        val rnd = Random(kind.hashCode())
        List(count) {
            Particle(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                speed = 0.06f + rnd.nextFloat() * 0.10f,
                size = 0.4f + rnd.nextFloat() * 0.6f,
                color = rnd.nextInt(confettiColors.size)
            )
        }
    }
    var timeNanos by remember { mutableLongStateOf(0L) }
    if (!reduceMotion) {
        LaunchedEffect(kind) {
            val start = withFrameNanos { it }
            while (true) withFrameNanos { timeNanos = it - start }
        }
    }
    val measurer = rememberTextMeasurer()
    val dropGlyph = emoji?.takeIf { it.isNotBlank() } ?: "💧"

    Canvas(modifier) {
        val t = timeNanos / 1_000_000_000f
        val w = size.width
        val h = size.height
        val d = density
        when (kind) {
            "snow" -> particles.forEach { p ->
                val y = ((p.phase + t * p.speed) % 1f) * h
                val x = p.x * w + sin(t * 0.8f + p.phase * 6f) * 10f * d
                drawCircle(Color.White.copy(alpha = 0.55f), radius = (1.5f + 2.5f * p.size) * d, center = Offset(x, y))
            }
            "confetti" -> particles.forEach { p ->
                val y = ((p.phase + t * p.speed * 1.3f) % 1f) * h
                val x = p.x * w + sin(t + p.phase * 6f) * 8f * d
                rotate(degrees = t * 90f * p.size + p.phase * 360f, pivot = Offset(x, y)) {
                    drawRect(
                        confettiColors[p.color].copy(alpha = 0.85f),
                        topLeft = Offset(x, y),
                        size = Size(5f * d, 8f * d * p.size + 3f * d)
                    )
                }
            }
            "rain" -> particles.forEach { p ->
                val y = ((p.phase + t * p.speed * 2.2f) % 1f) * h
                val layout = measurer.measure(dropGlyph, TextStyle(fontSize = (12f + 8f * p.size).sp))
                drawText(layout, topLeft = Offset(p.x * w, y), alpha = 0.8f)
            }
            "sparkles" -> particles.forEach { p ->
                val twinkle = (sin(t * 2f + p.phase * 2f * PI.toFloat()) + 1f) / 2f
                val r = (3f + 5f * p.size) * d * (0.4f + 0.6f * twinkle)
                val c = Offset(p.x * w, ((p.phase * 7f) % 1f) * h)
                val col = accent.copy(alpha = 0.25f + 0.65f * twinkle)
                drawLine(col, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 1.5f * d)
                drawLine(col, Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 1.5f * d)
            }
            "lights" -> {
                // A string of bulbs along the top edge.
                val n = particles.size
                particles.forEachIndexed { i, p ->
                    val x = (i + 0.5f) / n * w
                    val y = 6f * d + sin(i * 0.9f) * 3f * d
                    val glow = (sin(t * 2.4f + i) + 1f) / 2f
                    val col = confettiColors[i % confettiColors.size]
                    drawCircle(col.copy(alpha = 0.15f + 0.25f * glow), radius = 9f * d, center = Offset(x, y))
                    drawCircle(col.copy(alpha = 0.5f + 0.5f * glow), radius = 3.5f * d, center = Offset(x, y))
                }
            }
        }
    }
}
