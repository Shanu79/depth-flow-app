package com.shin.depthflow.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.BlendGlowColor
import com.shin.depthflow.ui.theme.DepthFlowLightPurple
import com.shin.depthflow.ui.theme.TextPrimary
import com.shin.depthflow.ui.theme.TextSecondary
import com.shin.depthflow.ui.theme.cardBackgroundBrush

@Composable
fun UpgradeBannerCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val borderBrush = Brush.linearGradient(
        colors = listOf(BlendGlowColor.copy(alpha = 0.4f), Color.Transparent, BlendGlowColor.copy(alpha = 0.1f))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Left: Icon and Text Details ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f) // Takes up available space to push button to the right
        ) {
            // Diamond Icon
            Icon(
                imageVector = Icons.Filled.Diamond, // Replace with painterResource(id = R.drawable.ic_diamond) if you have a custom asset
                contentDescription = "Upgrade to Ultimate",
                tint = DepthFlowLightPurple, // Gives it that bright neon purple look
                modifier = Modifier.size(36.dp)
            )

            // Texts
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Upgrade to Ultimate",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Unlock all premium features and max out your creativity.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- Right: Upgrade Button ---
        // Using a custom Box here instead of standard OutlinedGlassButton to accommodate the trailing icon
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(BlendGlowColor.copy(alpha = 0.2f), Color.Transparent)),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Upgrade Now",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium, // Small badge/ribbon icon
                    contentDescription = null,
                    tint = DepthFlowLightPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}







