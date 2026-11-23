package com.shin.depthflow.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Import your centralized theme colors
import com.shin.depthflow.ui.theme.DepthFlowLightPurple
import com.shin.depthflow.ui.theme.IconInactive
import com.shin.depthflow.ui.theme.TextPrimary
import com.shin.depthflow.ui.theme.DepthFlowCyan
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.BorderHighlight
import com.shin.depthflow.ui.theme.navBarBackground
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

@Composable
fun DepthFlowBottomNavigation(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    isModalOpen: Boolean = false, // Add this parameter
    currentRoute: String,
    onNavigate: (String) -> Unit = {},
    onPlusClicked: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // 1. The Frosted Glass Background Layer
        Surface(
            color = Color.Transparent,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, BorderHighlight.copy(alpha = 0.3f)),
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        blurRadius = 60.dp,
                        tint = HazeTint(navBarBackground.copy(alpha = 0.5f)),
                        noiseFactor = 0.2f
                    )
                )
        ) {
            // Empty Surface acting as the glass layer
        }

        // 2. The Content Layer (Icons and Text)
        Row(
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left side items
            // Notifications (top-bar bell) and Happy Hour (banner on Home and Workspace, or a push) are
            // reachable from several tabs, so no tab is highlighted there. DepthFlowApp's
            // isTargetSectionCurrent still treats them as Home for the "tap Home returns to Home root"
            // behaviour; that difference is intentional.
            val isHomeSection = currentRoute == Routes.HOME ||
                               currentRoute.startsWith(Routes.EXPLORE_LIBRARY) ||
                               currentRoute in listOf(Routes.REGISTER, Routes.VERIFY_OTP, Routes.RESET_PASSWORD)
            
            BottomNavItem(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = isHomeSection,
                onSelect = { onNavigate(Routes.HOME) },
                modifier = Modifier.weight(1f)
            )
            BottomNavItem(
                label = "Workspace",
                icon = Icons.Default.Folder,
                isSelected = currentRoute == Routes.WORKSPACE,
                onSelect = { onNavigate(Routes.WORKSPACE) },
                modifier = Modifier.weight(1f)
            )

            // Central Glowing Action Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(y = (-16).dp)
                    .clickable(
                        onClick = onPlusClicked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 1. Soft radial backdrop glow (Only show when modal is NOT open)
                if (!isModalOpen) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        DepthFlowLightPurple.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                // 2. The button itself
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .then(
                            if (isModalOpen) {
                                Modifier
                                    .background(Color(0xFF131024))
                                    .border(1.dp, Color(0xFF2E2452), CircleShape)
                            } else {
                                Modifier.background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            DepthFlowPurple.copy(alpha = 0.75f),
                                            DepthFlowCyan.copy(alpha = 0.85f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isModalOpen) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isModalOpen) "Close Options" else "Create Options",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Right side items
            BottomNavItem(
                label = "Pricing",
                icon = Icons.Outlined.ShoppingCart,
                isSelected = currentRoute == Routes.PRICING,
                onSelect = { onNavigate(Routes.PRICING) },
                modifier = Modifier.weight(1f)
            )
            
            val isProfileSection = currentRoute in listOf(
                Routes.PROFILE, 
                Routes.EDIT_PROFILE, 
                Routes.MY_PROJECTS, 
                Routes.BILLING, 
                Routes.FAQ, 
                Routes.CONTACT_US
            )
            
            BottomNavItem(
                label = "Profile",
                icon = Icons.Default.Person,
                isSelected = isProfileSection,
                onSelect = { onNavigate(Routes.PROFILE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onSelect,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) DepthFlowLightPurple else IconInactive,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) DepthFlowLightPurple else IconInactive,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}







