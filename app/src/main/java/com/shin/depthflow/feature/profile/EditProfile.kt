package com.shin.depthflow.feature.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shin.depthflow.R
import com.shin.depthflow.data.ChangePasswordRequest
import com.shin.depthflow.ui.theme.*
import com.shin.depthflow.viewmodel.DepthFlowViewModel

@Composable
fun EditProfileScreen(
    viewModel: DepthFlowViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val currentFullName by viewModel.userFullName.collectAsState()
    val currentEmail by viewModel.userEmail.collectAsState()
    val currentProfilePic by viewModel.userProfilePic.collectAsState()

    var fullName by remember { mutableStateOf(currentFullName) }
    var email by remember { mutableStateOf(currentEmail) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var isSaving by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 140.dp),
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
            Text(
                text = "Edit Profile",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.BottomEnd
        ) {
            val displayImage: Any = selectedImageUri ?: currentProfilePic ?: R.drawable.profile_icon_male
            
            AsyncImage(
                model = displayImage,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, DepthFlowPurple.copy(alpha = 0.5f), CircleShape)
                    .clickable { imageLauncher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
            
            IconButton(
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier
                    .size(32.dp)
                    .background(DepthFlowPurple, CircleShape)
                    .border(2.dp, Surface, CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Change Picture", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // --- Default Icon Selection ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Or choose a default icon",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    R.drawable.profile_icon_male to "Male",
                    R.drawable.profile_icon_female to "Female"
                ).forEach { (resId, label) ->
                    val iconUri = Uri.parse("android.resource://${context.packageName}/$resId")
                    val isSelected = selectedImageUri == iconUri

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { selectedImageUri = iconUri }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) DepthFlowPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) DepthFlowPurple else Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(if (isSelected) 2.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = label,
                            color = if (isSelected) DepthFlowPurple else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // --- General Info ---
        EditSection(title = "General Information") {
            EditTextField(
                label = "Full Name",
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = "John Doe",
                icon = Icons.Default.Person
            )
            EditTextField(
                label = "Email Address",
                value = email,
                onValueChange = { email = it },
                placeholder = "john@example.com",
                icon = Icons.Default.Email
            )
            
            Button(
                onClick = {
                    isSaving = true
                    viewModel.updateProfile(
                        fullName = fullName,
                        email = email,
                        imageUri = selectedImageUri,
                        onSuccess = {
                            isSaving = false
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            isSaving = false
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DepthFlowPurple)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Password Section ---
        EditSection(title = "Security") {
            EditTextField(
                label = "Current Password",
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = "••••••••",
                icon = Icons.Default.Lock,
                isPassword = true
            )
            EditTextField(
                label = "New Password",
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = "••••••••",
                icon = Icons.Default.VpnKey,
                isPassword = true
            )
            EditTextField(
                label = "Confirm New Password",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "••••••••",
                icon = Icons.Default.VpnKey,
                isPassword = true
            )
            
            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isChangingPassword = true
                    viewModel.changePassword(
                        request = ChangePasswordRequest(oldPassword, newPassword),
                        onSuccess = {
                            isChangingPassword = false
                            oldPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            isChangingPassword = false
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                enabled = !isChangingPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                if (isChangingPassword) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = TextMuted, fontSize = 14.sp) },
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = DepthFlowPurple.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = DepthFlowPurple,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )
    }
}







