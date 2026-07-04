package com.shin.depthflow.feature.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import com.shin.depthflow.R

@Composable
fun VideoLibraryScreen(
    onVideoSelect: (ShowcaseVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track which video is currently expanded/playing inline
    var activeVideo by remember { mutableStateOf<ShowcaseVideo?>(null) }

    val recentCreations = remember {
        listOf(
            ShowcaseVideo("Mech Warrior", "2 mins ago", R.raw.rc_one),
            ShowcaseVideo("Cyber Car", "1 hour ago", R.raw.rc_two),
            ShowcaseVideo("Modern House", "3 hours ago", R.raw.rc_three),
            ShowcaseVideo("Fantasy World", "Yesterday", R.raw.rc_four)
        )
    }

    val visualShowcase = remember {
        listOf(
            ShowcaseVideo("Vehicles", videoResId = R.raw.vs_one),
            ShowcaseVideo("Architecture", videoResId = R.raw.vs_two),
            ShowcaseVideo("Product", videoResId =  R.raw.vs_three),
            ShowcaseVideo("Fantasy World", videoResId = R.raw.vs_four),
            ShowcaseVideo("Gaming Assets", videoResId = R.raw.vs_five)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        VideoListSection(
            title = "Recent Creations",
            videos = recentCreations,
            activeVideo = activeVideo,
            onVideoClick = { video ->
                activeVideo = if (activeVideo == video) null else video
                onVideoSelect(video)
            }
        )
        VideoListSection(
            title = "Visual Showcase",
            videos = visualShowcase,
            activeVideo = activeVideo,
            onVideoClick = { video ->
                activeVideo = if (activeVideo == video) null else video
                onVideoSelect(video)
            }
        )
    }
}

@Composable
fun VideoListSection(
    title: String,
    videos: List<ShowcaseVideo>,
    activeVideo: ShowcaseVideo?,
    onVideoClick: (ShowcaseVideo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { }
            ) {
                Text(text = "See All", color = Color(0xFF9E65FF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "See All",
                    tint = Color(0xFF9E65FF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(videos.size) { i ->
                val video = videos[i]
                val isExpanded = video == activeVideo
                
                // Animate scaling linearly
                val scale by animateFloatAsState(
                    targetValue = if (isExpanded) 1.08f else 1f,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "scale"
                )

                VideoShowcaseCard(
                    video = video,
                    isExpanded = isExpanded,
                    onVideoClick = onVideoClick,
                    modifier = Modifier
                        .fillParentMaxWidth(0.35f)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            // Apply a bit of elevation shadow when expanded
                            if (isExpanded) {
                                shadowElevation = 8.dp.toPx()
                                shape = RoundedCornerShape(16.dp)
                                clip = false // Allow the scale-up to overlap slightly
                            }
                        }
                )
            }
        }
    }
}

/**
 * Creates an inner glow effect by drawing a blurred stroke along the bounds.
 * Note: Must be used on a modifier chain that has `.clip()` applied before it
 * so the outer bleed of the blur is hidden.
 */
fun Modifier.innerGlow(
    color: Color,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 16.dp,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            this.isAntiAlias = true
            this.style = PaintingStyle.Stroke
            this.strokeWidth = blurRadius.toPx()
        }

        // Applies a native Android blur to the stroke
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(
            blurRadius.toPx(),
            BlurMaskFilter.Blur.NORMAL
        )

        // Draw the blurred stroke along the edges.
        // Because the parent modifier is clipped, the outer half of the blur
        // is cleanly cut off, leaving a perfect inner edge glow.
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = cornerRadius.toPx(),
            radiusY = cornerRadius.toPx(),
            paint = paint
        )
    }
}

/**
 * Creates an outer glow effect by drawing a blurred shape behind the content.
 * Note: Must be used on a modifier chain BEFORE `.clip()` is applied,
 * otherwise the expanding blurred edges will be cut off.
 */
fun Modifier.outerGlow(
    color: Color,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 8.dp,
): Modifier = this.drawBehind {
    // Prevent crash if blur radius is 0
    if (blurRadius == 0.dp) return@drawBehind

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = color
            this.isAntiAlias = true
            // Using Fill ensures the glow emanates fully from behind the shape
            this.style = PaintingStyle.Fill
        }

        // Applies a native Android blur to the shape
        paint.asFrameworkPaint().maskFilter = BlurMaskFilter(
            blurRadius.toPx(),
            BlurMaskFilter.Blur.NORMAL
        )

        // Draw the blurred shape behind the content.
        // Because it's drawn behind, your component's background will cover the inside,
        // leaving only the outer blurred edge visible as a glowing aura.
        canvas.drawRoundRect(
            left = 0f,
            top = 0f,
            right = size.width,
            bottom = size.height,
            radiusX = cornerRadius.toPx(),
            radiusY = cornerRadius.toPx(),
            paint = paint
        )
    }
}







