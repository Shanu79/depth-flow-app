package com.shin.depthflow.feature.billing

import com.shin.depthflow.util.Logger
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.*
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import java.util.Locale

@Composable
fun BillingScreen(
    viewModel: DepthFlowViewModel,
    onBack: () -> Unit,
    onViewPricing: () -> Unit
) {
    val credits by viewModel.credits.collectAsState()
    val plan by viewModel.userPlan.collectAsState()
    val paymentHistory by viewModel.paymentHistory.collectAsState()
    val subStatus by viewModel.subscriptionStatus.collectAsState()
    val billingInfo by viewModel.billingInfo.collectAsState()

    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 5
    val totalPages = (paymentHistory.size + pageSize - 1) / pageSize

    val context = LocalContext.current
    var isCancelling by remember { mutableStateOf(false) }
    var isResubscribing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchPaymentHistory()
        viewModel.fetchBillingInfo()
    }

    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(BlendGlowColor.copy(alpha = 0.4f), Color.Transparent, BlendGlowColor.copy(alpha = 0.1f))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Header ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Billing & Subscription",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your plan, credits, and payments.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // --- Overview Section ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Overview",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Credits Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackgroundBrush)
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF39C12).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFFF39C12),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "AVAILABLE CREDITS",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = credits.toString(),
                                color = TextPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "credits remaining",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Usage Meter (percentage of 1000 credits)
                            val progress = (credits.toFloat() / 1000f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(100.dp)),
                                color = DepthFlowPurple,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )
                            Text(
                                text = "Keep creating! ✨",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Plan Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackgroundBrush)
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DepthFlowPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = DepthFlowPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            val isScheduledForCancellation = subStatus.contains("Scheduled for cancellation", ignoreCase = true)

                            if (isScheduledForCancellation) {
                                Surface(
                                    color = Color(0xFFF39C12).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF39C12).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFFF39C12),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "Ends End of Cycle",
                                            color = Color(0xFFF39C12),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color(0xFF00C853).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Active",
                                        color = Color(0xFF00C853),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            val planName = if (plan.isEmpty()) "Basic Plan" else "${plan.replaceFirstChar { it.uppercase() }} Plan"
                            Text(
                                text = planName,
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (planName.contains("Pro", ignoreCase = true)) "$19.99 / year" else "$9.99 / year",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Billed yearly",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            
                            val isScheduledForCancellation = subStatus.contains("Scheduled for cancellation", ignoreCase = true)
                            if (isScheduledForCancellation) {
                                val endDate = subStatus.substringAfterLast(" ").trim().removeSuffix(".")
                                Text(
                                    text = "Your plan cancels on $endDate. Reactivate now to keep your credits.",
                                    color = Color(0xFFF39C12).copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isScheduledForCancellation = subStatus.contains("Scheduled for cancellation", ignoreCase = true)
                            val isAppSubscription = billingInfo?.platform?.is_active == true
                            val isWebSubscription = billingInfo?.api?.is_active == true

                            if (isAppSubscription) {
                                if (isScheduledForCancellation) {
                                    if (isResubscribing) {
                                        Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00C853))
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                isResubscribing = true
                                                viewModel.resubscribeSubscription(
                                                    planType = "workspace",
                                                    onSuccess = { message ->
                                                        isResubscribing = false
                                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { error ->
                                                        isResubscribing = false
                                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Sync,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text("Resubscribe", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = onViewPricing,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DepthFlowPurple),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Upgrade to Pro", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (isCancelling) {
                                    Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TextMuted)
                                    }
                                } else if (!isScheduledForCancellation) {
                                    Text(
                                        text = "Cancel Plan",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.cancelSubscription(
                                                    planType = "workspace",
                                                    onSuccess = { message ->
                                                        isCancelling = false
                                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                    },
                                                    onError = { error ->
                                                        isCancelling = false
                                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else if (isWebSubscription) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Managed via Web. Visit depthflow.io",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // Default button for free users
                                Button(
                                    onClick = onViewPricing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DepthFlowPurple),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Upgrade to Pro", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Payment History Section ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Payment History",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // History List / Empty State
            if (paymentHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackgroundBrush)
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DepthFlowPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = DepthFlowPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "No payment history yet",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "When you make a payment, it will appear here.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = onViewPricing,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DepthFlowPurple.copy(alpha = 0.5f))
                        ) {
                            Text("View Pricing Plans", color = DepthFlowPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                val pagedHistory = paymentHistory.drop(currentPage * pageSize).take(pageSize)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBackgroundBrush)
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val headers = listOf("Invoice ID", "Amt", "Status", "Src", "Date")
                        val weights = listOf(0.35f, 0.15f, 0.15f, 0.15f, 0.2f)
                        
                        headers.forEachIndexed { index, title ->
                            Text(
                                text = title,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(weights[index]),
                                textAlign = if (index == 4) TextAlign.End else TextAlign.Start,
                                maxLines = 1
                            )
                        }
                    }

                    pagedHistory.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.id.take(8) + "...",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.35f)
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", item.amount)}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.15f)
                            )
                            
                            // Status Badge
                            Box(modifier = Modifier.weight(0.15f)) {
                                val statusColor = when (item.status.lowercase()) {
                                    "success", "succeeded" -> Color(0xFF00C853)
                                    "pending" -> Color(0xFFFFAB00)
                                    else -> WarningRed
                                }
                                Surface(
                                    color = statusColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = if (item.status.length > 5) item.status.take(4) + "." else item.status.replaceFirstChar { it.uppercase() },
                                        color = statusColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Source Badge
                            Box(modifier = Modifier.weight(0.15f)) {
                                val isWeb = item.source?.lowercase() == "web" || item.id.startsWith("pay_")
                                Surface(
                                    color = (if (isWeb) Color(0xFF23D3EE) else DepthFlowPurple).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, (if (isWeb) Color(0xFF23D3EE) else DepthFlowPurple).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isWeb) Icons.Outlined.Language else Icons.Outlined.Shop,
                                            contentDescription = null,
                                            tint = if (isWeb) Color(0xFF23D3EE) else DepthFlowPurple,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Text(
                                            text = if (isWeb) "Web" else "App",
                                            color = if (isWeb) Color(0xFF23D3EE) else DepthFlowPurple,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = item.created_at.substringBefore(" ").substringBefore("T"), // Just the date part
                                color = TextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(0.2f),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    // Pagination Controls
                    if (totalPages > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = if (currentPage > 0) TextPrimary else TextMuted.copy(alpha = 0.3f)
                                )
                            }

                            Text(
                                text = "Page ${currentPage + 1} of $totalPages",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = if (currentPage < totalPages - 1) TextPrimary else TextMuted.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Features / Trust Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrustItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Security,
                title = "Secure Payments",
                desc = "Payments are safe."
            )
            TrustItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Sync,
                title = "Cancel Anytime",
                desc = "Cancel anytime."
            )
            TrustItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.HeadsetMic,
                title = "Need Help?",
                desc = "Contact support."
            )
        }
    }
}

@Composable
fun TrustItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    desc: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DepthFlowPurple,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}







