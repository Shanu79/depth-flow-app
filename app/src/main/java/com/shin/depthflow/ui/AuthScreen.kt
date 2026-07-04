package com.shin.depthflow.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.ui.components.*

@Composable
fun AuthScreen(
    viewModel: DepthFlowViewModel,
    initialIsLogin: Boolean = true,
    onLoginSuccess: (token: String) -> Unit,
    onRegisterSuccess: () -> Unit,
    onForgotPasswordRequestSuccess: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onClose: () -> Unit = {}
) {
    var mode by remember { mutableStateOf(if (initialIsLogin) "login" else "register") }
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val loginToken by viewModel.loginToken.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginToken) {
        if ((loginToken != null) && (mode == "login")) {
            onLoginSuccess(loginToken!!)
            viewModel.clearLoginToken()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Close Button ---
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthHeader(
                subtitle = when (mode) {
                    "login" -> "Sign in to access your workspace"
                    "register" -> "Create your account"
                    else -> "Reset your password"
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            AuthCard(error = authError) {
                if (mode == "register") {
                    AuthTextField(
                        label = "Full Name",
                        leadingIcon = Icons.Default.Person,
                        value = fullName,
                        onValueChange = { fullName = it }
                    )
                }

                AuthTextField(
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    value = email,
                    onValueChange = { email = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                )

                if (mode != "forgot") {
                    AuthTextField(
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        value = password,
                        onValueChange = { password = it },
                        isPassword = true
                    )
                    
                    if (mode == "login") {
                        Text(
                            text = "Forgot Password?",
                            color = NeonPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { mode = "forgot" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                AuthActionButton(
                    text = when (mode) {
                        "login" -> "Sign In"
                        "register" -> "Create Account"
                        else -> "Request Reset"
                    },
                    onClick = {
                        when (mode) {
                            "login" -> viewModel.login(email, password)
                            "register" -> viewModel.register(fullName, email, password,
                                onSuccess = { onRegisterSuccess() },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                            else -> viewModel.requestPasswordReset(email,
                                onSuccess = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    onForgotPasswordRequestSuccess()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    isLoading = isLoading,
                    enabled = email.isNotBlank() && (mode == "forgot" || (password.isNotBlank() && (mode == "login" || fullName.isNotBlank())))
                )

                if (mode == "login") {
                    AuthOrSeparator()

                    GoogleSignInButton(
                        onClick = onGoogleSignInClick,
                        isLoading = isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthFooter(
                text = when (mode) {
                    "login" -> "Don't have an account?"
                    "register" -> "Already have an account?"
                    else -> "Remember your password?"
                },
                actionText = when (mode) {
                    "login" -> "Register"
                    else -> "Login"
                },
                onActionClick = { 
                    mode = if (mode == "login") "register" else "login"
                }
            )
        }
    }
}







