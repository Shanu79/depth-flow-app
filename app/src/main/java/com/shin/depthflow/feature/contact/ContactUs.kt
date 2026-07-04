package com.shin.depthflow.feature.contact

import com.shin.depthflow.util.Logger
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.*

@Composable
fun ContactUsScreen(onBack: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- Custom Header ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
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
                    text = "Contact Us",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "We're here to help.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        // --- Left Section Content (Website Style) ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "If you have any questions about our platform, pricing, features, or technical support, feel free to reach out to us. Our team aims to respond as quickly as possible.",
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            // Direct Email Card
            ContactInfoCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DepthFlowPurple.copy(alpha = 0.1f))
                            .border(1.dp, DepthFlowPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = Color(0xFF23D3EE), // Cyan tint matching screenshot
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(text = "Email us directly at", color = TextMuted, fontSize = 12.sp)
                        Text(text = "support@depthflow.io", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Assistance Guidelines Card
            ContactInfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = DepthFlowPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = "For faster assistance:", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Please include the following details in your message to help us serve you better:",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    
                    val guidelines = listOf("Your registered email address", "A brief description of your issue or request")
                    guidelines.forEach { guide ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF23D3EE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = guide, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Text(
                text = "\"We value your feedback and are always open to suggestions that help us improve Depthflow AI\"",
                color = TextSecondary,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp
            )
        }

        // --- Right Section (Form) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardBackgroundSolidBrush)
                .border(1.dp, sectionBorderBrush, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Send us a message",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            ContactTextField(
                label = "Full Name",
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "John Doe",
                icon = Icons.Outlined.Person
            )

            ContactTextField(
                label = "Email Address",
                value = email,
                onValueChange = { email = it },
                placeholder = "john@example.com",
                icon = Icons.Outlined.Email
            )

            ContactTextField(
                label = "Message",
                value = message,
                onValueChange = { message = it },
                placeholder = "How can we help you?",
                isMultiline = true
            )

            Button(
                onClick = { /* Handle Send Action */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF23D3EE), DepthFlowPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Send Message",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInfoCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackgroundSolidBrush)
            .border(1.dp, sectionBorderBrush, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun ContactTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector? = null,
    isMultiline: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isMultiline) Modifier.height(120.dp) else Modifier),
            placeholder = { Text(text = placeholder, color = TextMuted, fontSize = 14.sp) },
            leadingIcon = icon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardSurfaceTop.copy(alpha = 0.2f),
                unfocusedContainerColor = CardSurfaceTop.copy(alpha = 0.2f),
                focusedBorderColor = DepthFlowPurple.copy(alpha = 0.5f),
                unfocusedBorderColor = Border.copy(alpha = 0.3f),
                cursorColor = DepthFlowPurple,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = !isMultiline
        )
    }
}







