package com.shin.depthflow.feature.pricing

import com.shin.depthflow.util.Logger
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.feature.home.components.outerGlow
import com.shin.depthflow.feature.profile.shimmerEffect
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.viewmodel.ContentViewModel
import com.shin.depthflow.ui.promo.LinkRouter
import com.shin.depthflow.ui.promo.OfferBanner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shin.depthflow.ui.components.PricingButton
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.cardBackgroundBrush

// --- Local Theme Colors ---
private val BackgroundDark = Color(0xFF0B0B14)
private val SurfaceCard = Color(0xFF151529)
private val NeonPurple = Color(0xFF9034EA)
private val NeonLightPurple = Color(0xFFB42BFE)
private val BorderDark = Color(0xFF2E2452)
private val TextMuted = Color(0xFFBBB8CF)

@Composable
fun PricingScreen(
    viewModel: DepthFlowViewModel, // Added viewModel
    contentViewModel: ContentViewModel? = null,
    onNavigateToLogin: () -> Unit, // Added callback for login navigation
    // ADDED: Callback to trigger the purchase flow in MainActivity
    onInitiatePurchase: (planId: String, isYearly: Boolean) -> Unit = { _, _ -> }
) {
    var isYearly by remember { mutableStateOf(true) }
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val currentPlan by viewModel.userPlan.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Offer banner: refetch on every visit and on resume (no tracking, no caching beyond that).
    val context = LocalContext.current
    val banner by (contentViewModel?.banner ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) })
        .collectAsState()
    DisposableEffect(contentViewModel, context) {
        val owner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) contentViewModel?.refreshBanner()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp)
            // Extra bottom padding to clear the floating bottom navigation bar
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Admin-managed offer banner. Nothing is composed (so no gap) when there is none.
        banner?.let { item ->
            OfferBanner(
                item = item,
                skewMillis = contentViewModel?.skewMillis ?: 0L,
                onCta = {
                    // Already on Pricing: an in-app "/pricing" link needs no action.
                    LinkRouter.handle(context, item.button_link) { }
                }
            )
        }

        // Header UI is here...
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Pricing Plans",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Choose the perfect plan for your creative needs.",
                color = TextMuted,
                fontSize = 14.sp
            )
        }

        // --- Monthly / Yearly Toggle ---
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(SurfaceCard, RoundedCornerShape(32.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    "Monthly",
                    color = if (!isYearly) Color.White else TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = isYearly,
                    onCheckedChange = { isYearly = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BorderDark,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Yearly",
                        color = if (isYearly) Color.White else TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 10% OFF Discount Badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF19322A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("10% OFF", color = Color(0xFF33C19C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- Pricing Cards ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                val isFreePlan = isLoggedIn && (currentPlan.lowercase() == "free" || currentPlan.isEmpty())

                if (isProfileLoading && isFreePlan) {
                    LoadingPricingCard()
                } else {
                    PricingPlanCard(
                        title = "Free",
                        category = "FOR TESTING",
                        price = "$0",
                        credits = "20 Credits",
                        creditsSub = "One-time",
                        features = listOf("Watermarked result", "Slow processing queue", "No commercial usage"),
                        ctaText = if (isFreePlan) "Current Plan" else "Get Started",
                        isPopular = false,
                        buttonColor = SurfaceCard,
                        onPurchaseClick = {
                            if (!isLoggedIn) {
                                onNavigateToLogin()
                            } else {
                                // Already on free or select free logic
                            }
                        }
                    )
                }

                val isBasicPlan = isLoggedIn && currentPlan.lowercase().trim() == "basic"
                if (isProfileLoading && isBasicPlan) {
                    LoadingPricingCard()
                } else {
                    PricingPlanCard(
                        title = "Basic",
                        category = "FOR INDIVIDUALS",
                        price = if (isYearly) "$10" else "$12",
                        credits = "600 Credits",
                        creditsSub = "per month",
                        features = listOf("2D to 3D Depth Motion", "No watermark", "Standard processing", "Commercial use allowed"),
                        ctaText = if (isBasicPlan) "Current Plan" else "Get Started",
                        isPopular = false,
                        buttonColor = if (isBasicPlan) Color.Gray.copy(alpha = 0.5f) else DepthFlowPurple,
                        // ADDED: Trigger purchase callback
                        onPurchaseClick = {
                            if (!isLoggedIn) {
                                onNavigateToLogin()
                            } else if (!isBasicPlan) {
                                Logger.d("BillingTest", "1. UI Button Clicked! isYearly: $isYearly")
                                onInitiatePurchase("Basic Plan", isYearly)
                            }
                        }
                    )
                }

                // VIP Pro Plan (Glowing Border & Ribbon)
                val isProPlan = isLoggedIn && currentPlan.lowercase().trim() == "pro"
                if (isProfileLoading && isProPlan) {
                    LoadingPricingCard()
                } else {
                    PricingPlanCard(
                        title = "Pro",
                        category = "FOR PROFESSIONALS",
                        price = if (isYearly) "$22" else "$25",
                        credits = "1,500 Credits",
                        creditsSub = "per month",
                        features = listOf("2D to 3D Depth Motion", "MP4 up to 1080p quality", "Fast processing queue", "Fully Commercial use", "Premium Support"),
                        ctaText = if (isProPlan) "Current Plan" else "Upgrade to Pro",
                        isPopular = true,
                        buttonColor = if (isProPlan) Color.Gray.copy(alpha = 0.5f) else DepthFlowPurple,
                        // ADDED: Trigger purchase callback
                        onPurchaseClick = {
                            if (!isLoggedIn) {
                                onNavigateToLogin()
                            } else if (!isProPlan) {
                                onInitiatePurchase("Pro Plan", isYearly)
                            }
                        }
                    )
                }
            }

        // --- Trust Badge Footer ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(16.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Secure",
                    tint = Color(0xFF33C19C),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text("Secure & Hassle-free", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Cancel anytime. No hidden charges.", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
@Composable
fun LoadingPricingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}

@Composable
fun PricingPlanCard(
    title: String,
    category: String,
    price: String,
    credits: String,
    creditsSub: String,
    features: List<String>,
    ctaText: String,
    isPopular: Boolean,
    buttonColor: Color,
    modifier: Modifier = Modifier,
    // ADDED: Click listener parameter
    onPurchaseClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundBrush)
            .border(
                width = if (isPopular) 1.5.dp else 1.dp,
                brush = if (isPopular) {
                    Brush.verticalGradient(listOf(NeonLightPurple.copy(alpha = 0.8f), NeonPurple.copy(alpha = 0.2f)))
                } else {
                    Brush.verticalGradient(listOf(BorderDark, BorderDark))
                },
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // -- Header Row --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2C194D).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(category, color = NeonLightPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isPopular) {
                    // NEW: Glowing "MOST POPULAR" Pill Tag
                    Box(
                        modifier = Modifier
                            .outerGlow(NeonPurple.copy(alpha = 0.5f), cornerRadius = 20.dp, blurRadius = 12.dp)
                            .background(NeonPurple.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .border(1.dp, NeonPurple, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "MOST POPULAR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // -- Pricing & Credits Row --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = price,
                    color = NeonLightPurple,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(credits, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(creditsSub, color = TextMuted, fontSize = 12.sp)
                }
            }

            // -- Features List --
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(Color(0xFF2C194D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonLightPurple,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(feat, color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            // -- Action Button (Bottom Right) --
            Box(modifier = Modifier
                .fillMaxWidth(),
            ) {
                PricingButton(
                    text = ctaText,
                    onClick = onPurchaseClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(140.dp),
                    blendColor = buttonColor
                )
            }
        }
    }
}








