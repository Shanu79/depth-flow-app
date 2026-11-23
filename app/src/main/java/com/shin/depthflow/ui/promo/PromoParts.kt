package com.shin.depthflow.ui.promo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * Seconds left until [endAt], anchored to the server clock through [skewMillis]
 * (serverNow - deviceNow). Null when there is no valid end time. Ticks once per second.
 */
@Composable
fun rememberCountdownSeconds(endAt: String?, skewMillis: Long): Long? {
    val endMillis = remember(endAt) {
        endAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
    } ?: return null
    var left by remember(endMillis) { mutableLongStateOf(secondsUntil(endMillis, skewMillis)) }
    LaunchedEffect(endMillis, skewMillis) {
        while (true) {
            left = secondsUntil(endMillis, skewMillis)
            if (left <= 0L) break
            delay(1000)
        }
    }
    return left
}

private fun secondsUntil(endMillis: Long, skewMillis: Long): Long =
    ((endMillis - (System.currentTimeMillis() + skewMillis)) / 1000).coerceAtLeast(0L)

/** `DD:HH:MM:SS` countdown with labelled cells. Renders nothing at zero. */
@Composable
fun CountdownRow(seconds: Long, color: Color, accent: Color, compact: Boolean = false) {
    if (seconds <= 0L) return
    val parts = listOf(
        "DAYS" to seconds / 86400,
        "HRS" to seconds % 86400 / 3600,
        "MIN" to seconds % 3600 / 60,
        "SEC" to seconds % 60
    )
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
        parts.forEach { (label, value) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 3.dp else 6.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d".format(value),
                        color = accent,
                        fontSize = if (compact) 14.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!compact) Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Coupon code pill with a Copy button. Display only: nothing is applied at checkout. */
@Composable
fun CouponPill(code: String, textColor: Color, accent: Color, compact: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, accent, RoundedCornerShape(50))
            .clickable {
                clipboard.setText(AnnotatedString(code))
                copied = true
            }
            .padding(start = 12.dp, end = 10.dp, top = if (compact) 4.dp else 8.dp, bottom = if (compact) 4.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 12.sp else 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.defaultMinSize(minWidth = 0.dp)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
            contentDescription = if (copied) "Copied" else "Copy code",
            tint = accent,
            modifier = Modifier.size(if (compact) 14.dp else 16.dp)
        )
        Text(
            text = if (copied) " Copied" else " Copy",
            color = accent,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Filled CTA button; label colour is chosen from the button fill's luminance. */
@Composable
fun PromoButton(text: String, palette: PromoPalette, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(palette.button)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = if (compact) 8.dp else 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = palette.buttonText,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Close (X) with a 48dp touch target. */
@Composable
fun PromoCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.White) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.35f), androidx.compose.foundation.shape.CircleShape)
    ) {
        Icon(Icons.Filled.Close, contentDescription = "Close", tint = tint, modifier = Modifier.size(18.dp))
    }
}
