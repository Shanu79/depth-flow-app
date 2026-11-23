package com.shin.depthflow.ui.promo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.data.WhatsNewRelease
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark
import com.shin.depthflow.ui.theme.TextMuted

val WhatsNewLilac = Color(0xFFD8B4FE)

/** Maps the backend icon key to a Material icon. Unknown keys fall back to a sparkle. */
fun whatsNewIcon(key: String?): ImageVector = when (key?.trim()?.lowercase()) {
    "smartphone" -> Icons.Filled.Smartphone
    "devices" -> Icons.Filled.Devices
    "zap" -> Icons.Filled.Bolt
    "gift" -> Icons.Filled.CardGiftcard
    "coins" -> Icons.Filled.MonetizationOn
    "code" -> Icons.Filled.Code
    "toggle" -> Icons.Filled.ToggleOn
    "film" -> Icons.Filled.Movie
    "sliders" -> Icons.Filled.Tune
    "rocket" -> Icons.Filled.RocketLaunch
    "sparkles" -> Icons.Filled.AutoAwesome
    "star" -> Icons.Filled.Star
    "heart" -> Icons.Filled.Favorite
    "shield" -> Icons.Filled.Shield
    "globe" -> Icons.Filled.Public
    "bell" -> Icons.Filled.Notifications
    "video" -> Icons.Filled.Videocam
    "image" -> Icons.Filled.Image
    "wand" -> Icons.Filled.AutoFixHigh
    "palette" -> Icons.Filled.Palette
    "clock" -> Icons.Filled.Schedule
    "check" -> Icons.Filled.CheckCircle
    "users" -> Icons.Filled.Group
    "trophy" -> Icons.Filled.EmojiEvents
    "flame" -> Icons.Filled.LocalFireDepartment
    "layers" -> Icons.Filled.Layers
    "cpu" -> Icons.Filled.Memory
    "cloud" -> Icons.Filled.Cloud
    "mail" -> Icons.Filled.Mail
    "lock" -> Icons.Filled.Lock
    else -> Icons.Filled.AutoAwesome
}

/**
 * One What's New release as a banner in the Notifications screen: badge, title and subtitle.
 * Tapping it opens the release's detail page ([onClick]).
 *
 * @param isLatest the first release from the server, styled as the hero
 * @param isNew true while this announcement has not been opened yet
 */
@Composable
fun WhatsNewCard(
    release: WhatsNewRelease,
    isLatest: Boolean,
    isNew: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val border = if (isLatest) {
        Brush.linearGradient(listOf(DepthFlowPurple, Color(0xFF3B82F6)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.10f)))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isLatest) Brush.verticalGradient(listOf(Color(0xFF241446), SurfaceDark))
                else Brush.verticalGradient(listOf(SurfaceDark, SurfaceDark))
            )
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DepthFlowPurple.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                whatsNewIcon(release.features?.firstOrNull()?.icon ?: "sparkles"),
                contentDescription = null,
                tint = WhatsNewLilac,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val label = release.badge_text?.takeIf { it.isNotBlank() } ?: release.tab_label
                if (!label.isNullOrBlank()) {
                    Text(
                        text = label,
                        color = WhatsNewLilac,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (isNew) {
                    Text(
                        "NEW",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(DepthFlowPurple)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = release.title.orEmpty(),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!release.subtitle.isNullOrBlank()) {
                Text(
                    text = release.subtitle,
                    color = TextMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = TextMuted)
    }
}

/** The release's feature rows: icon in a tinted square, bold title, muted description. */
@Composable
fun WhatsNewFeatureList(release: WhatsNewRelease, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        release.features.orEmpty().forEach { f ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(DepthFlowPurple.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(whatsNewIcon(f.icon), contentDescription = null, tint = WhatsNewLilac, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(f.title.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (!f.description.isNullOrBlank()) {
                        Text(f.description, color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * The release's two buttons (each only if its text is set). [onLink] receives the button's link.
 * A Play-store button pointing at this app's own listing is relabelled "Rate us".
 */
@Composable
fun WhatsNewButtons(release: WhatsNewRelease, onLink: (String?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!release.button1_text.isNullOrBlank()) {
            val ownListing = release.button1_style == "google_play" &&
                release.button1_link?.contains("id=${context.packageName}") == true
            val label = if (ownListing) "Rate us" else release.button1_text
            val (fill, textColor, border) = when (release.button1_style) {
                "light" -> Triple(Color.White, Color(0xFF111827), null)
                "dark" -> Triple(Color(0xFF1F2937), Color.White, BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)))
                else -> Triple(Color.Black, Color.White, BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))) // google_play
            }
            WnButton(
                label = label,
                fill = fill,
                textColor = textColor,
                border = border,
                leading = if (release.button1_style == "google_play") Icons.Filled.PlayArrow else null
            ) { onLink(release.button1_link) }
        }
        if (!release.button2_text.isNullOrBlank()) {
            WnButton(
                label = release.button2_text,
                fill = Color.Transparent,
                textColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) { onLink(release.button2_link) }
        }
    }
}

@Composable
private fun WnButton(
    label: String,
    fill: Color,
    textColor: Color,
    border: BorderStroke?,
    leading: ImageVector? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
