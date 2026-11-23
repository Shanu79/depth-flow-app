package com.shin.depthflow.feature.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import com.shin.depthflow.ui.theme.cardBackgroundSolidBrush
import com.shin.depthflow.ui.theme.sectionBorderBrush
import com.shin.depthflow.ui.theme.WarningRed
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.shin.depthflow.feature.home.components.UpgradeBannerCard
import com.shin.depthflow.ui.components.OutlinedGlassButton
import com.shin.depthflow.ui.theme.CardSurfaceBottom
import com.shin.depthflow.ui.theme.CardSurfaceTop

// --- Shimmer Utility (Unchanged) ---
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200)
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1C38).copy(alpha = 0.6f),
                Color(0xFF5B5A8A).copy(alpha = 0.2f),
                Color(0xFF1E1C38).copy(alpha = 0.6f),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned {
            size = it.size
        }
}

// --- Centralized Theme Colors (Refined) ---
private val DepthFlowPurple = Color(0xFF9034EA)
private val DepthFlowLightPurple = Color(0xFFB42BFE)
private val DepthFlowPink = Color(0xFFA451F8)

private val TextPrimary = Color.White
private val TextMuted = Color(0xFF7A7A9D) // Main grey for small descriptions
private val TextSecondary = Color(0xFF9E9EB8) // Greier grey for subtitles
// WarningRed removed from here as it is now in theme/Color.kt

// The purple-ish blend/glow color used in borders and buttons
private val BlendGlowColor = DepthFlowPurple

@Composable
fun ProfileScreen(
    viewModel: DepthFlowViewModel,
    onLogout: () -> Unit,
    onNavigateToContactUs: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToFAQ: () -> Unit,
    onNavigateToMyProjects: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onOpenWhatsNew: () -> Unit = {}
) {
    val isLoading by viewModel.isProfileLoading.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val fullName by viewModel.userFullName.collectAsState()
    val plan by viewModel.userPlan.collectAsState()
    val profilePic by viewModel.userProfilePic.collectAsState()
    val subStatus by viewModel.subscriptionStatus.collectAsState()

    // Dynamic state placeholders for missing data
    val joinDate by viewModel.userJoinDate.collectAsState()
    val planRenewalDate by viewModel.userPlanRenewalDate.collectAsState()

    // Gradient used for dynamic plan badging and icons
    val planIconBrush = when (plan.lowercase().trim()) {
        "pro" -> Brush.linearGradient(listOf(Color(0xFFFFEA7A), Color(0xFFF39C12)))    // Gold
        "basic" -> Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)))  // Silver
        else -> Brush.linearGradient(listOf(Color(0xFFE5A65D), Color(0xFF9C5A24)))     // Bronze (Free Default)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Main Title removed to match screenshot's top bar layout ---

        if (!isNetworkAvailable) {
            NoInternetCard(onRetry = { viewModel.fetchUserProfile() })
        } else if (isLoading) {
            // Shimmer Loading States (Unchanged)
            LoadingProfileHeader()
            LoadingInfoCard()
            LoadingInfoCard()
            LoadingActionMenu()
        } else {
            // 1. Profile Identity Header (Redesigned)
            ProfileHeaderCard(fullName, email, plan, profilePic, joinDate, onNavigateToEditProfile)

            // 2. Active Plan Configuration (New direct implementation)
            PlanCard(plan, planRenewalDate, subStatus, onManageClick = onNavigateToBilling)

            // 3. Grouped Application Menu Items (Redesigned)
            ActionMenu(
                onLogout = onLogout,
                onNavigateToContactUs = onNavigateToContactUs,
                onNavigateToBilling = onNavigateToBilling,
                onNavigateToFAQ = onNavigateToFAQ,
                onNavigateToMyProjects = onNavigateToMyProjects,
                onOpenWhatsNew = onOpenWhatsNew
            )

            UpgradeBannerCard(onClick = onNavigateToPricing)
        }
    }
}

// --- Redesigned Profile Header Card ---
@Composable
fun ProfileHeaderCard(fullName: String, email: String, plan: String, profilePic: String?, joinDate: String, onEditClick: () -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundSolidBrush)
            .border(
                width = 1.dp,
                brush = sectionBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp) // Adjusted padding slightly for better centering
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, // FIX: Centers the button vertically like the screenshot
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { onEditClick() }
            ) {
                val imageSource: Any = profilePic ?: R.drawable.profile_icon_male

                AsyncImage(
                    model = imageSource,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(2.dp, BlendGlowColor.copy(alpha = 0.5f), CircleShape) // Muted avatar glow border
                )
                // Small edit icon on avatar
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Image",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .background(DepthFlowPurple, CircleShape)
                        .border(1.5.dp, CardSurfaceBottom, CircleShape)
                        .padding(4.dp)
                )
            }

            // Right: Details
            Column(
                modifier = Modifier.weight(1f) // Ensures it takes available space without pushing button off screen
            ) {
                // Name Row with Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = fullName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1, // FIX: Prevent extra lines
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false) // Lets name truncate before crushing the badge
                    )
                    // Plan badge matching screenshot
                    Box(
                        modifier = Modifier
                            .background(DepthFlowPurple.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                            .border(1.dp, DepthFlowPurple.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = plan.lowercase().replaceFirstChar { it.uppercase() }, // FIX: Capitalizes to "Pro"
                            color = DepthFlowLightPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column{
                    // Email
                    Text(
                        text = email,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1, // FIX: Prevent extra lines
                        overflow = TextOverflow.Ellipsis
                    )

                    // Member Since with Calendar Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Member since $joinDate",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1, // FIX: Prevent extra lines
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Contextual Edit Profile Button, using our common outlined style
            OutlinedGlassButton(
                text = "Edit Profile",
                onClick = onEditClick
            )
        }
    }
}

// --- Specific Plan Card implementation to match screenshot ---
@Composable
fun PlanCard(
    plan: String,
    renewalDate: String,
    subStatus: String,
    onManageClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardBackgroundSolidBrush)
            .border(
                width = 1.dp,
                brush = sectionBorderBrush,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trophy/Award icon, color with blend mode
            Icon(
                // Use a trophy like Filled.EmojiEvents (replace with your custom vector asset)
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer(alpha = 0.99f)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                // A specific purple gradient for trophy blend/glow
                                brush = Brush.linearGradient(listOf(DepthFlowPink, DepthFlowPurple)),
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            )
            // Column of texts from screenshot
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val isCancelled = subStatus.contains("Scheduled for cancellation", ignoreCase = true) || 
                                 subStatus.contains("canceled", ignoreCase = true) || 
                                 subStatus.contains("cancelled", ignoreCase = true)
                
                Text(text = "Current Plan", color = TextMuted, fontSize = 12.sp)
                Text(
                    text = "${plan.lowercase().replaceFirstChar { it.uppercase() }} Plan", // Dynamic capitalization
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (renewalDate != "N/A" && renewalDate.isNotEmpty()) {
                    val label = if (isCancelled) "Ends on " else "Renews on "
                    val dateColor = if (isCancelled) Color(0xFFF39C12) else Color(0xFF00C853)
                    Text(text = "$label$renewalDate", color = dateColor, fontSize = 12.sp)
                } else if (subStatus.lowercase().contains("active")) {
                    Text(text = "Subscription Active", color = Color(0xFF00C853), fontSize = 12.sp)
                } else {
                    Text(text = "Subscription Inactive", color = WarningRed, fontSize = 12.sp)
                }
            }
        }

        // Manage Plan Button with arrow, using common outlined glass style
        OutlinedGlassButton(
            text = "Manage Plan",
            onClick = onManageClick,
            isManagePlan = true
        )
    }
}

// Simple data class to hold the menu item details
data class MenuOption(
    val title: String,
    val subtitle: String?, // Nullable for things like Logout
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun ActionMenu(
    onLogout: () -> Unit,
    onNavigateToContactUs: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onNavigateToFAQ: () -> Unit,
    onNavigateToMyProjects: () -> Unit,
    onOpenWhatsNew: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Matches the spacing between cards
    ) {
        // Items with exact data and Outlined icons to match screenshot
        val items = listOf(
            MenuOption("My Projects", "View and manage your projects", Icons.Outlined.Folder, onNavigateToMyProjects),
            MenuOption("Billing & Payments", "Manage your subscriptions and invoices", Icons.Outlined.CreditCard, onNavigateToBilling),
            MenuOption("FAQs", "View FAQs and get answers to most asked questions", Icons.Outlined.QuestionAnswer, onNavigateToFAQ),
            MenuOption("What's New", "See the latest features and updates", Icons.Outlined.AutoAwesome, onOpenWhatsNew),
            MenuOption("Help & Support", "Get help and support", Icons.AutoMirrored.Outlined.HelpOutline, onNavigateToContactUs),
            MenuOption("Logout", null, Icons.AutoMirrored.Outlined.ExitToApp, onLogout)
        )

        items.forEach { item ->
            ActionMenuCard(
                title = item.title,
                subtitle = item.subtitle,
                icon = item.icon,
                action = item.action
            )
        }
    }
}

@Composable
fun ActionMenuCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    action: () -> Unit,
) {
    val isLogout = title == "Logout"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Modifier order matters: Clip first, then clickable for correct ripple, then background/border
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = action,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            )
            .background(cardBackgroundSolidBrush)
            .border(
                width = 1.dp,
                brush = sectionBorderBrush,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {

            // --- Icon Container Matching the Screenshot ---
            val tintColor = if (isLogout) WarningRed else DepthFlowPurple

            Box(
                modifier = Modifier
                    .size(32.dp) // Size of the rounded box holding the icon
                    .clip(RoundedCornerShape(12.dp))
                    .background(tintColor.copy(alpha = 0.08f)) // Faint background matching tint
                    .border(1.dp, tintColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tintColor, // Replaced gradient with solid tint to match cleaner UI image
                    modifier = Modifier.size(16.dp)
                )
            }

            // --- Text Column (Title + Subtitle) ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = if (isLogout) WarningRed else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Only render subtitle if it exists
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // --- Trailing Arrow ---
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = if (isLogout) WarningRed.copy(alpha = 0.6f) else TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- Loading Shimmers and NoInternetCard remain unchanged but updated with refined colors ---
@Composable
fun NoInternetCard(onRetry: () -> Unit) {
    val cardBackgroundBrush = Brush.linearGradient(colors = listOf(CardSurfaceTop, CardSurfaceBottom))
    // Prominent Warning Red glow blend border
    val borderBrush = Brush.linearGradient(colors = listOf(WarningRed.copy(alpha = 0.6f), Color.Transparent, WarningRed.copy(alpha = 0.2f)))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = WarningRed,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "No Internet Connection",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Please check your connection and try again to access your profile.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = DepthFlowPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Try Again", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoadingProfileHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}

@Composable
fun LoadingInfoCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(22.dp))
            .shimmerEffect()
    )
}

@Composable
fun LoadingActionMenu() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}







