package com.shin.depthflow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.DepthFlowCyan
import com.shin.depthflow.ui.theme.DepthFlowPurple
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// --- CENTRAL NAV PLUS SHEET MODAL PANEL ---
@Composable
fun PlusButtonModalSheet(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onSelectMode: (String) -> Unit,
) {
    // 1. Full Screen Dark Translucent Backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    blurRadius = 24.dp,
                    tint = HazeTint(Color(0xFF070511).copy(alpha = 0.7f)),
                    noiseFactor = 0.15f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 2. The Glowing Dome Overlay
        Box(
            modifier = Modifier
                .offset(y = 300.dp) // Push down so only the top arc is visible
                .size(800.dp) // Even larger circle to create a gentle arc
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9034EA).copy(alpha = 0.25f),
                            Color(0xFF070511).copy(alpha = 0.0f)
                        ),
                        center = androidx.compose.ui.geometry.Offset(450f, 0f)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB42BFE).copy(alpha = 0.8f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = 200f // Fades out the border quickly to only show the top rim
                    ),
                    shape = CircleShape
                )
        )

        // 3. The Content Area
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                // Prevents clicks on the content from dismissing the sheet
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(bottom = 140.dp), // Height above the nav bar and close button
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                Modifier.padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Glowing Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF9034EA).copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color(0xFFB42BFE).copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Titles
                Text(
                    text = "Create 3D Image",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose a mode to get started",
                    color = Color(0xFFBBB8CF),
                    fontSize = 12.sp
                )
            }

            // Selection Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModeSelectionCard(
                    title = "Basic Mode",
                    description = "Quick and easy 3D generation with optimized settings.",
                    icon = Icons.Default.ViewInAr,
                    color = DepthFlowCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode("Basic") }
                )

                ModeSelectionCard(
                    title = "Advanced Mode",
                    description = "Fine-tune every detail with advanced controls and custom settings.",
                    icon = Icons.Default.ViewInAr,
                    color = DepthFlowPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode("Advanced") }
                )
            }
        }
    }
}

// --- REUSABLE MODE CARD COMPONENT ---
@Composable
fun ModeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF131024).copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .drawBehind {
                val radiusPx = 24.dp.toPx()
                // Subtle inner glow at the top
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.15f), Color.Transparent),
                        startY = 0f,
                        endY = 100f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx)
                )
            }
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Card Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.25f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Card Text
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFFBBB8CF),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}







