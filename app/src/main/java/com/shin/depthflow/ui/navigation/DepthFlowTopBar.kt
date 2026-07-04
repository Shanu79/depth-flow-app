package com.shin.depthflow.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.R
import com.shin.depthflow.ui.theme.BackgroundDark
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark

@Composable
fun DepthFlowTopBar(
    credits: Int,
    isLoggedIn: Boolean = false,
    onLoginClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end= 16.dp, top = 64.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Left: Logo ---
        Image(
            painter = painterResource(id = R.drawable.depthflow_logo),
            contentDescription = "DepthFlow AI Logo",
            modifier = Modifier.height(26.dp),
            contentScale = ContentScale.Fit
        )

        // --- Right: Actions ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoggedIn) {
                // 1. Dynamic Credits Pill
                CreditsPill(credits)
            } else {
                // Login Button for unauthenticated users
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DepthFlowPurple.copy(alpha = 0.2f))
                        .border(1.dp, DepthFlowPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onLoginClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // 2. Notification Bell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = "Alerts",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CreditsPill(credits: Int) {
    // Subtle gradient for the top-left edge highlight (catch-light)
    val pillBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.1f),
            Color.White.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF16162C)) // Deep dark navy/purple background
            .border(
                width = 1.dp,
                brush = pillBorderBrush,
                shape = CircleShape
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Storage, // Represents the stacked disks
                contentDescription = "Credits",
                tint = Color(0xFF9D64FF), // Vibrant purple from the design
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "%,d Credits".format(credits),
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium // Medium looks closer to the reference than Bold
            )
        }
    }
}







