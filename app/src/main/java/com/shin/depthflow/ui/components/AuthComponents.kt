package com.shin.depthflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.R

// --- Shared Theme Colors ---
val BackgroundDark = Color(0xFF0B0B14)
val SurfaceCard = Color(0xFF151529)
val NeonPurple = Color(0xFF9034EA)
val BorderDark = Color(0xFF2E2452)
val TextMuted = Color(0xFFBBB8CF)

@Composable
fun AuthHeader(subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.depthflow_logo),
            contentDescription = "DepthFlow AI Logo",
            modifier = Modifier.height(48.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = subtitle, color = TextMuted, fontSize = 14.sp)
    }
}

@Composable
fun AuthCard(
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .border(1.dp, BorderDark, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF4B4B).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(error, color = Color(0xFFFF4B4B), fontSize = 12.sp)
                }
            }
            content()
        }
    }
}

@Composable
fun AuthTextField(
    label: String,
    leadingIcon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = TextMuted) },
        trailingIcon = if (isPassword) {
            {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = TextMuted)
                }
            }
        } else null,
        modifier = modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
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
}

@Composable
fun AuthActionButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                brush = Brush.horizontalGradient(listOf(NeonPurple, Color(0xFFB42BFE))),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AuthOrSeparator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark, thickness = 1.dp)
        Text(
            text = "OR",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderDark, thickness = 1.dp)
    }
}

@Composable
fun AuthFooter(text: String, actionText: String, onActionClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, color = TextMuted, fontSize = 14.sp)
        Text(
            text = actionText,
            color = NeonPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onActionClick() }
        )
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Branding Sign In",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}







