package com.shin.depthflow.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.R
import com.shin.depthflow.ui.components.*
import com.shin.depthflow.viewmodel.DepthFlowViewModel

@Composable
fun ResetPasswordScreen(
    viewModel: DepthFlowViewModel,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val email by viewModel.registrationEmail.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Back Button ---
        IconButton(
            onClick = onBack,
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
                .verticalScroll(rememberScrollState())
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
            Text(text = "Reset your password", color = TextMuted, fontSize = 14.sp)
            Text(text = email, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(40.dp))

            AuthCard(error = authError) {
                AuthTextField(
                    label = "6-Digit OTP",
                    leadingIcon = Icons.Default.VpnKey,
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                AuthTextField(
                    label = "New Password",
                    leadingIcon = Icons.Default.Lock,
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    isPassword = true
                )

                AuthTextField(
                    label = "Confirm New Password",
                    leadingIcon = Icons.Default.Lock,
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                AuthActionButton(
                    text = "Update Password",
                    onClick = {
                        if (newPassword != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@AuthActionButton
                        }
                        viewModel.resetPassword(otp, newPassword,
                            onSuccess = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                onResetSuccess()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    isLoading = isLoading,
                    enabled = otp.length == 6 && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                )
            }
        }
    }
}







