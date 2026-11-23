package com.shin.depthflow.ui.promo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shin.depthflow.data.PromoItem

/**
 * Compact inline card for the top of the Pricing screen. Callers render nothing (and leave no
 * gap) when there is no banner. `image` mode shows `mobile_image_url` (else `image_url`) only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferBanner(
    item: PromoItem,
    skewMillis: Long,
    onCta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    if (item.display_mode == "image") {
        val url = item.mobile_image_url?.takeIf { it.isNotBlank() } ?: item.image_url
        if (url.isNullOrBlank()) return
        AsyncImage(
            model = url,
            contentDescription = item.title,
            contentScale = ContentScale.FillWidth,
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF151529))
                .then(if (!item.button_link.isNullOrBlank()) Modifier.clickable(onClick = onCta) else Modifier)
        )
        return
    }

    val palette = remember(item) { item.palette() }
    val family = promoFontFamily(item.font_style)
    val seconds = if (item.show_countdown == true) rememberCountdownSeconds(item.end_at, skewMillis) else null
    val hasCta = !item.button_text.isNullOrBlank() && !item.button_link.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.brush)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    ) {
        if (!item.bg_image_url.isNullOrBlank()) {
            AsyncImage(
                model = item.bg_image_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(Modifier.matchParentSize().background(palette.bg.copy(alpha = 0.7f)))
        }
        PromoDecor(item.theme, item.emoji, palette.accent, Modifier.matchParentSize())

        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!item.header_text.isNullOrBlank()) {
                        Text(
                            text = item.header_text.uppercase(),
                            color = palette.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = (if (!item.emoji.isNullOrBlank()) item.emoji + " " else "") + item.title.orEmpty(),
                        color = palette.text,
                        fontFamily = family,
                        fontWeight = promoTitleWeight(item.font_style),
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!item.description.isNullOrBlank()) {
                        Text(
                            text = item.description,
                            color = palette.text.copy(alpha = 0.8f),
                            fontFamily = family,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (!item.badge_text.isNullOrBlank()) {
                    Text(
                        text = item.badge_text,
                        color = palette.accent,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accent.copy(alpha = 0.16f))
                            .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            val hasExtras = (seconds != null && seconds > 0) || !item.coupon_code.isNullOrBlank() || hasCta
            if (hasExtras) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    if (seconds != null && seconds > 0) CountdownRow(seconds, palette.text, palette.accent, compact = true)
                    if (!item.coupon_code.isNullOrBlank()) CouponPill(item.coupon_code, palette.text, palette.accent, compact = true)
                    if (hasCta) PromoButton(item.button_text.orEmpty(), palette, onCta, compact = true)
                }
            }
        }
    }
}
