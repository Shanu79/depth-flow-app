package com.shin.depthflow.ui.promo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.shin.depthflow.data.PromoItem

/**
 * Home popup. `image` mode shows only the uploaded creative; `designed` mode builds the layout
 * from the item's colours, theme, emoji, countdown and coupon. All text is rendered as plain text.
 */
@Composable
fun HomePopupDialog(
    item: PromoItem,
    skewMillis: Long,
    onDismiss: () -> Unit,
    onCta: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // A full-window layer so a tap outside the card dismisses.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            val cardModifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp)
                .heightIn(max = maxHeight)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                // Swallow taps on the card so they do not reach the dismiss layer.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}

            if (item.display_mode == "image") {
                ImagePopupCard(item, cardModifier, onDismiss, onCta)
            } else {
                DesignedPopupCard(item, skewMillis, cardModifier, onDismiss, onCta)
            }
        }
    }
}

@Composable
private fun ImagePopupCard(item: PromoItem, modifier: Modifier, onDismiss: () -> Unit, onCta: () -> Unit) {
    Box(modifier) {
        AsyncImage(
            model = item.image_url,
            contentDescription = item.title,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF151529))
                .then(if (!item.button_link.isNullOrBlank()) Modifier.clickable(onClick = onCta) else Modifier)
        )
        PromoCloseButton(onDismiss, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

@Composable
private fun DesignedPopupCard(
    item: PromoItem,
    skewMillis: Long,
    modifier: Modifier,
    onDismiss: () -> Unit,
    onCta: () -> Unit
) {
    val palette = remember(item) { item.palette() }
    val family = promoFontFamily(item.font_style)
    val titleWeight = promoTitleWeight(item.font_style)
    val seconds = if (item.show_countdown == true) rememberCountdownSeconds(item.end_at, skewMillis) else null
    val hasCta = !item.button_text.isNullOrBlank() && !item.button_link.isNullOrBlank()

    Box(modifier.background(palette.brush)) {
        if (!item.bg_image_url.isNullOrBlank()) {
            AsyncImage(
                model = item.bg_image_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Tint so text stays readable over the picture.
            Box(Modifier.matchParentSize().background(palette.bg.copy(alpha = 0.7f)))
        }
        PromoDecor(item.theme, item.emoji, palette.accent, Modifier.matchParentSize())

        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (!item.image_url.isNullOrBlank()) {
                val imageModifier = if (item.layout == "split") Modifier.height(180.dp) else Modifier.height(190.dp)
                AsyncImage(
                    model = item.image_url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier.fillMaxWidth().background(palette.accent.copy(alpha = 0.12f))
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!item.header_text.isNullOrBlank()) {
                    Text(
                        text = item.header_text.uppercase(),
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (!item.emoji.isNullOrBlank()) Text(item.emoji + " ", fontSize = 24.sp)
                    Text(
                        text = item.title.orEmpty(),
                        color = palette.text,
                        fontFamily = family,
                        fontWeight = titleWeight,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (!item.description.isNullOrBlank()) {
                    Text(
                        text = item.description,
                        color = palette.text.copy(alpha = 0.85f),
                        fontFamily = family,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
                if (seconds != null && seconds > 0) CountdownRow(seconds, palette.text, palette.accent)
                if (!item.coupon_code.isNullOrBlank()) CouponPill(item.coupon_code, palette.text, palette.accent)
                if (hasCta) PromoButton(item.button_text.orEmpty(), palette, onCta, Modifier.fillMaxWidth())
                if (!item.footnote.isNullOrBlank()) {
                    Text(
                        text = item.footnote,
                        color = palette.text.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        PromoCloseButton(onDismiss, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}
