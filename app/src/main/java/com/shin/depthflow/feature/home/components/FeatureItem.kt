package com.shin.depthflow.feature.home.components

import com.shin.depthflow.util.Logger
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.cardBackgroundBrush
import com.shin.depthflow.ui.theme.sectionContainerOuter
import kotlinx.coroutines.delay

data class FeatureItem(
    val icon: Int,
    val title: String,
    val subtitle: String
)

@Composable
fun FeatureCardsSection(
    modifier: Modifier = Modifier
) {
    val features = listOf(
        FeatureItem(
            icon = com.shin.depthflow.R.drawable.l,
            title = "Fast Generation",
            subtitle = "Generate 3D images in seconds"
        ),
        FeatureItem(
            icon = com.shin.depthflow.R.drawable.ai,
            title = "AI Powered",
            subtitle = "Advanced AI for photorealistic results"
        ),
        FeatureItem(
            icon = com.shin.depthflow.R.drawable.q,
            title = "High Quality",
            subtitle = "Ultra-realistic 3D image outputs"
        ),
        FeatureItem(
            icon = com.shin.depthflow.R.drawable.m,
            title = "Multiple Styles",
            subtitle = "Choose from various camera & render styles"
        )
    )

    // Tracks which card is currently glowing/expanded (Starts at 0)
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Carousel Effect: Automatically cycles to the next card every 3 seconds.
    // Because it is keyed to `selectedIndex`, clicking a card manually resets the timer automatically!
    LaunchedEffect(selectedIndex) {
        delay(3000L) // Wait 3 seconds
        selectedIndex = (selectedIndex + 1) % features.size // Move to next, loop back to 0 at the end
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically // Centers scaled cards nicely within the row
    ) {
        features.forEachIndexed { index, feature ->
            FeatureCard(
                feature = feature,
                isSelected = selectedIndex == index,
                onClick = { selectedIndex = index }, // Manual tap selection
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. ORGANIC SPRING SCALE: Gives a subtle, smooth bounce when scaling up
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow // Low stiffness = smoother, slower transition
        ),
        label = "scale_animation"
    )

    // 2. SMOOTH COLOR FADE: Animates the glowing border color
    val animatedBorderHighlight by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8B6BFF) else Color(0xFF4C3B8C).copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "border_color_animation"
    )

    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            animatedBorderHighlight, // Uses the animated color
            Color(0xFF1D1442).copy(alpha = 0.3f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundBrush)
            .border(
                width = 1.dp,
                brush = cardBorderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            // 3. FLUID CONTENT SIZE: Uses an easing curve to smoothly unroll the text
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
            .defaultMinSize(minHeight = 145.dp)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4C22FF),
                                Color(0xFF240E8A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B6BFF), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = feature.icon),
                    contentDescription = feature.title,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = feature.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                lineHeight = 14.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = feature.subtitle,
                color = Color(0xFF8A8A9D),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = if (isSelected) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}







