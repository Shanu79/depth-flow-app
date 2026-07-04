package com.shin.depthflow.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.R
import com.shin.depthflow.viewmodel.DepthFlowViewModel

private val BackgroundDark = Color(0xFF0B0B14)
private val SurfaceCard = Color(0xFF151529)
private val NeonPurple = Color(0xFF9034EA)
private val BorderDark = Color(0xFF2E2452)
private val TextMuted = Color(0xFFBBB8CF)

@Composable
fun OtpVerificationScreen(
    viewModel: DepthFlowViewModel,
    onVerificationSuccess: (token: String) -> Unit,
    onBackToRegister: () -> Unit,
) {
    var otp by remember { mutableStateOf("") }
    val email by viewModel.registrationEmail.collectAsState()

    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val loginToken by viewModel.loginToken.collectAsState()

    LaunchedEffect(loginToken) {
        if (loginToken != null) {
            onVerificationSuccess(loginToken!!)
            viewModel.clearLoginToken()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- Back Button ---
        IconButton(
            onClick = onBackToRegister,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.depthflow_logo),
                contentDescription = "DepthFlow AI Logo",
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Verify your email", color = TextMuted, fontSize = 14.sp)
            Text(text = email, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    if (authError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFF4B4B).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(authError!!, color = Color(0xFFFF4B4B), fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it },
                        label = { Text("6-Digit OTP", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.verifyOtp(email, otp,
                                onSuccess = {
                                    // Navigation handled by loginToken observer
                                },
                                onError = {
                                    // Error handled by StateFlow
                                }
                            )
                        },
                        enabled = !isLoading && otp.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(NeonPurple, Color(0xFFB42BFE))),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Verify Email", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Didn't receive the code?", color = TextMuted, fontSize = 14.sp)
                Text("Resend", color = NeonPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { 
                    // Call register again to resend OTP
                })
            }
        }
    }
}







