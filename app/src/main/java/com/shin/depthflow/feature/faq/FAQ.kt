package com.shin.depthflow.feature.faq


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.*

data class FAQItem(val question: String, val answer: String, val category: String)

@Composable
fun FAQScreen(
    onBack: () -> Unit,
    onNavigateToContact: () -> Unit
) {
    val faqData = remember {
        listOf(
            // --- General Category ---
            FAQItem(
                category = "General",
                question = "What is Depthflow AI?",
                answer = "Depthflow is an AI-powered platform that converts 2D images into realistic 3D depth and motion renders. You can create engaging 3D-style videos from a single image in just a few clicks."
            ),
            FAQItem(
                category = "General",
                question = "What output format do I get?",
                answer = "All renders are delivered as MP4 videos, making them easy to share on social media, websites, and presentations."
            ),
            FAQItem(
                category = "General",
                question = "Are my uploaded images safe and private?",
                answer = "Yes. Your images are used only for processing and are not shared or sold. We take data privacy and security seriously."
            ),

            // --- Pricing Category ---
            FAQItem(
                category = "Pricing",
                question = "How does the credit system work?",
                answer = "Depthflow AI uses a credit-based system. 1 image conversion = 20 credits. Credits are deducted only when a render is successfully generated."
            ),
            FAQItem(
                category = "Pricing",
                question = "Can I use the generated renders for commercial projects?",
                answer = "Yes. Commercial usage is allowed on Basic and Pro plans. The Free plan is for personal and testing purposes only."
            ),
            FAQItem(
                category = "Pricing",
                question = "Do my unused credits expire?",
                answer = "Monthly plan credits reset at the end of each billing cycle. Credits cannot be carried forward to the next month. Trial credits are limited and meant for preview use only."
            ),
            FAQItem(
                category = "Pricing",
                question = "Can I upgrade or downgrade my plan anytime?",
                answer = "Yes. You can upgrade or change your plan anytime from your account dashboard. Upgrades take effect immediately without losing access."
            ),
            FAQItem(
                category = "Pricing",
                question = "Are there any hidden fees or extra charges?",
                answer = "No. Depthflow follows transparent pricing. You only pay for the plan you choose — there are no hidden fees or surprise charges."
            ),
            FAQItem(
                category = "Pricing",
                question = "Is my payment secure on Depthflow?",
                answer = "Yes. All payments on Depthflow are processed through secure and trusted payment gateways. We do not store your card or payment details on our servers."
            ),
            FAQItem(
                category = "Pricing",
                question = "Can I pay from any country or in any currency?",
                answer = "Yes. Depthflow supports global payments, allowing users from most countries to pay using their local currency and available payment methods."
            ),
            FAQItem(
                category = "Pricing",
                question = "Can I cancel my subscription anytime?",
                answer = "Absolutely. You can cancel your subscription anytime from your account dashboard. Your access will remain active until the end of your current billing period."
            )
        )
    }

    val categories = listOf("General", "Pricing")
    var selectedCategory by remember { mutableStateOf("General") }

    val filteredFaq = remember(selectedCategory) {
        faqData.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp)
    ) {
        // --- Header ---
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardSurfaceTop.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "FAQs",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Everything you need to know.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // --- Category Selection ---
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isSelected) DepthFlowPurple else CardSurfaceTop.copy(alpha = 0.5f))
                            .border(
                                1.dp,
                                if (isSelected) DepthFlowPurple else Border.copy(alpha = 0.2f),
                                RoundedCornerShape(100.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else TextMuted,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- FAQ List ---
        items(filteredFaq, key = { it.question }) { item ->
            FAQCard(item)
        }

        // --- Support CTA ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBackgroundSolidBrush)
                    .border(1.dp, sectionBorderBrush, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DepthFlowPurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QuestionAnswer,
                        contentDescription = null,
                        tint = DepthFlowPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Still have questions?",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Can't find the answer you're looking for? Please chat to our friendly team.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onNavigateToContact,
                    colors = ButtonDefaults.buttonColors(containerColor = DepthFlowPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.8f)
                ) {
                    Text("Get in Touch", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FAQCard(item: FAQItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            if (isExpanded) DepthFlowPurple.copy(alpha = 0.5f) else Border.copy(alpha = 0.3f),
            Color.Transparent
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundSolidBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.question,
                color = if (isExpanded) DepthFlowPurple else TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (isExpanded) DepthFlowPurple else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        if (isExpanded) {
            Text(
                text = item.answer,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}







