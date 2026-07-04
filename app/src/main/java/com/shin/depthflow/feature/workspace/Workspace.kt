package com.shin.depthflow.feature.workspace

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.shin.depthflow.data.PipelineState
import com.shin.depthflow.feature.profile.NoInternetCard
import com.shin.depthflow.feature.profile.shimmerEffect
import com.shin.depthflow.ui.components.GlassmorphicButton
import com.shin.depthflow.ui.components.GradientOutlineButton
import com.shin.depthflow.ui.navigation.Routes
import com.shin.depthflow.ui.theme.*
import com.shin.depthflow.util.NotificationHelper
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import kotlinx.coroutines.launch
import java.io.File

private val NeonPurple = Color(0xFF8344FF)
private val NeonBlue = Color(0xFF1BE5FF)
private val BorderDark = Color(0xFF2E2452)
private val TextMuted = Color(0xFFBBB8CF)
private val NeonIndigo = Color(0xFF8C52FF)


@Composable
fun WorkspaceScreen(
    viewModel: DepthFlowViewModel,
    navController: NavController,
    userCredits: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val motionPath by viewModel.motionPath.collectAsState()
    val depthIntensity by viewModel.depthIntensity.collectAsState()
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val activeSettingsTab by viewModel.activeSettingsTab.collectAsState()

    val speedValue by viewModel.speedValue.collectAsState()
    val lengthValue by viewModel.lengthValue.collectAsState()
    val ssaaValue by viewModel.ssaaValue.collectAsState()
    val fpsValue by viewModel.fpsValue.collectAsState()
    val cameraStyle by viewModel.cameraStyle.collectAsState()
    val focusValue by viewModel.focusValue.collectAsState()
    val cinematicIntensity by viewModel.cinematicIntensity.collectAsState()
    val bokehQuality by viewModel.bokehQuality.collectAsState()
    val motionMode by viewModel.motionMode.collectAsState()

    var activeSegmentTab by remember { mutableStateOf("Input") }

    val context = LocalContext.current

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required to show download alerts", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create 3D Image", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
            }
            Text("Transform your ideas into stunning 3D visuals with AI", color = TextMuted, fontSize = 13.sp)
        }

        if (!isNetworkAvailable) {
            NoInternetCard(onRetry = { viewModel.fetchUserProfile() })
        } else if (isProfileLoading) {
            LoadingWorkspaceArea()
            LoadingWorkspaceSettings()
        } else {
            WorkspaceUploadArea(
                activeSegmentTab = activeSegmentTab,
                onTabSelected = { activeSegmentTab = it },
                selectedImageUri = selectedImageUri,
                onUploadClick = { imageLauncher.launch("image/*") },
                uiState = uiState,
            )

            WorkspaceSettingsArea(
                depthIntensity = depthIntensity,
                onIntensityChange = { viewModel.setDepthIntensity(it) },
                motionPath = motionPath,
                onMotionPathChange = { viewModel.setMotionPath(it) },
                speedValue = speedValue,
                onSpeedChange = { viewModel.setSpeedValue(it) },
                lengthValue = lengthValue,
                onLengthChange = { viewModel.setLengthValue(it) },
                ssaaValue = ssaaValue,
                onSsaaChange = { viewModel.setSsaaValue(it) },
                fpsValue = fpsValue,
                onFpsChange = { viewModel.setFpsValue(it) },
                cameraStyle = cameraStyle,
                onStyleChange = { viewModel.setCameraStyle(it) },
                focusValue = focusValue,
                onFocusChange = { viewModel.setFocusValue(it) },
                cinematicIntensity = cinematicIntensity,
                onCinematicIntensityChange = { viewModel.setCinematicIntensity(it) },
                bokehQuality = bokehQuality,
                onBokehChange = { viewModel.setBokehQuality(it) },
                motionMode = motionMode,
                onModeChange = { viewModel.setMotionMode(it) },
                userCredits = userCredits,
                onGenerateClick = {
                    activeSegmentTab = "Result"
                    viewModel.generate3DMotion()
                },
                onBuyCreditsClick = {
                    // Navigate to billing/pricing screen
                     navController.navigate(Routes.PRICING)
                },
                isGenerating = uiState is PipelineState.Uploading || uiState is PipelineState.Processing,
                activeTab = activeSettingsTab,
                onTabChange = { viewModel.setActiveSettingsTab(it) }
            )
        }
    }
}

@Composable
fun LoadingWorkspaceArea() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(28.dp))
            .shimmerEffect()
    )
}

@Composable
fun LoadingWorkspaceSettings() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(24.dp))
            .shimmerEffect()
    )
}

@Composable
fun WorkspaceUploadArea(
    activeSegmentTab: String,
    onTabSelected: (String) -> Unit,
    selectedImageUri: Uri?,
    onUploadClick: () -> Unit,
    uiState: PipelineState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Tab Switcher (Input vs Result)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF16162C))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            listOf("Input", "Result").forEach { tab ->
                val isSelected = activeSegmentTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Brush.horizontalGradient(listOf(NeonIndigo, NeonBlue)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 36.dp, vertical = 10.dp)
                ) {
                    Text(tab, color = if (isSelected) Color.White else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2. Main Dropzone Area
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    drawRoundRect(color = Color.White.copy(alpha = 0.2f), style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect), cornerRadius = CornerRadius(24.dp.toPx()))
                }
                .clip(RoundedCornerShape(24.dp))
                .clickable { if (activeSegmentTab == "Input") onUploadClick() },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = activeSegmentTab, label = "tabFade") { tab ->
                when (tab) {
                    "Input" -> {
                        if (selectedImageUri != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(model = selectedImageUri, contentDescription = "Source", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                Box(
                                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1D1B3E).copy(alpha = 0.5f))
                                        .border(1.dp, Brush.linearGradient(listOf(NeonBlue.copy(alpha = 0.8f), NeonPurple.copy(alpha = 0.8f))), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Drag & drop your image here", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text("or click to browse", color = NeonIndigo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(30.dp)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp)).padding(horizontal = 20.dp, vertical = 8.dp)) {
                                    Text("Supports: JPG, PNG, WebP (Max 10MB)", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    "Result" -> {
                        val state = uiState
                        when (state) {
                            is PipelineState.Success -> {
                                UriVideoPlayer(videoUri = state.videoUri, modifier = Modifier.fillMaxSize())
                            }
                            is PipelineState.Uploading, is PipelineState.Processing -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (selectedImageUri != null) {
                                        AsyncImage(model = selectedImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.15f })
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        CircularProgressIndicator(color = NeonBlue, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                                        Text(if (state is PipelineState.Uploading) "Uploading to Cloud..." else "Rendering 3D Depth Map...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            is PipelineState.Error -> { /* Error UI */ }
                            is PipelineState.Idle -> { /* Idle UI */ }
                        }
                    }
                }
            }
        }

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        // Strictly grabbing the result video URI
        val currentState = uiState
        val resultVideoUri = if (currentState is PipelineState.Success) currentState.videoUri else null

        // 3. Action Buttons (Download / Share)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientOutlineButton(
                text = "Download",
                icon = Icons.Outlined.FileDownload,
                onClick = {
                    resultVideoUri?.let { uri ->
                        coroutineScope.launch {
                            val success = saveVideoToGallery(context, uri)
                            if (success) {
                                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } ?: Toast.makeText(context, "No 3D result to download yet.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )
            GradientOutlineButton(
                text = "Share",
                icon = Icons.Outlined.Share,
                onClick = {
                    resultVideoUri?.let { uri ->
                        coroutineScope.launch {
                            shareVideo(context, uri)
                        }
                    } ?: Toast.makeText(context, "No 3D result to share yet.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun UriVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    repeat: Boolean = true,
    muted: Boolean = false
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            playWhenReady = autoPlay
            repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}

suspend fun saveVideoToGallery(context: Context, videoUri: Uri): Boolean {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val filename = "DepthFlow_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/DepthFlow")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val contentResolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = contentResolver.insert(collection, contentValues)

        try {
            itemUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }

                // Show notification
                NotificationHelper.showDownloadNotification(context, uri)

                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun shareVideo(context: Context, videoUri: Uri) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val videoFile = File(videoUri.path ?: return@withContext)
            
            // If the URI is a file URI, we might need to use FileProvider to share it
            val shareUri = if (videoUri.scheme == "file") {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    videoFile
                )
            } else {
                videoUri
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, shareUri)
                    putExtra(Intent.EXTRA_TEXT, "I just brought this photo to life in 3D using DepthFlow AI! ✨ Download the app to try it yourself! 💜")
                    type = "video/mp4"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share DepthFlow 3D Video"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                Toast.makeText(context, "Error sharing video", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun WorkspaceSettingsArea(
    depthIntensity: Float,
    onIntensityChange: (Float) -> Unit,
    motionPath: String,
    onMotionPathChange: (String) -> Unit,
    speedValue: Float,
    onSpeedChange: (Float) -> Unit,
    lengthValue: Float,
    onLengthChange: (Float) -> Unit,
    ssaaValue: String,
    onSsaaChange: (String) -> Unit,
    fpsValue: String,
    onFpsChange: (String) -> Unit,
    cameraStyle: String,
    onStyleChange: (String) -> Unit,
    focusValue: Float,
    onFocusChange: (Float) -> Unit,
    cinematicIntensity: Float,
    onCinematicIntensityChange: (Float) -> Unit,
    bokehQuality: String,
    onBokehChange: (String) -> Unit,
    motionMode: String,
    onModeChange: (String) -> Unit,
    userCredits: Int,
    onGenerateClick: () -> Unit,
    onBuyCreditsClick: () -> Unit,
    isGenerating: Boolean,
    activeTab: String,
    onTabChange: (String) -> Unit
) {
    var showInsufficientCreditsModal by remember { mutableStateOf(false) }

    val calculatedCost = when {
        lengthValue > 20f -> 30
        lengthValue > 10f -> 25
        else -> 20
    }

    // Credits Modal Trigger

        InsufficientCreditsDialog(
            requiredCredits = calculatedCost,
            currentCredits = userCredits,
            onDismiss = { showInsufficientCreditsModal = false },
            onBuyCredits = {
                showInsufficientCreditsModal = false
                onBuyCreditsClick()
            },
            isVisible = showInsufficientCreditsModal
        )


    val cardBackgroundBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF2A1C54), Color(0xFF0A0A16)),
        start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(NeonPurple.copy(alpha = 0.6f), Color.Transparent),
        start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackgroundBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // --- TAB HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Basic", "Advanced").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onTabChange(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tab, color = if (isSelected) Color.White else TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (activeTab == "Basic") {
                BasicSettingsContent(depthIntensity, onIntensityChange, speedValue, onSpeedChange, lengthValue, onLengthChange, motionPath, onMotionPathChange)
            } else {
                AdvancedSettingsContent(lengthValue, onLengthChange, ssaaValue, onSsaaChange, fpsValue, onFpsChange, cameraStyle, onStyleChange, motionMode, onModeChange, speedValue, onSpeedChange, focusValue, onFocusChange, cinematicIntensity, onCinematicIntensityChange, bokehQuality, onBokehChange)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderDark))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔥", fontSize = 14.sp)
                    Text("Cost Estimation", color = TextMuted, fontSize = 13.sp)
                }
                Text("$calculatedCost Credits", color = NeonPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            GlassmorphicButton(
                text = if (isGenerating) "Generating..." else "Generate 3D Image",
                icon = Icons.Default.AutoAwesome,
                onClick = {
                    if (userCredits < calculatedCost) {
                        showInsufficientCreditsModal = true
                    } else {
                        onGenerateClick()
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
private fun BasicSettingsContent(
    depthIntensity: Float,
    onIntensityChange: (Float) -> Unit,
    speedValue: Float,
    onSpeedChange: (Float) -> Unit,
    lengthValue: Float,
    onLengthChange: (Float) -> Unit,
    motionPath: String,
    onMotionPathChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // --- Render Section ---
        SettingsSection(title = "Render") {
            AdvancedSliderRow(
                label = "Motion Amplitude",
                value = depthIntensity,
                range = 0.1f..1.5f,
                onValueChange = onIntensityChange,
                valueDisplay = String.format("%.1f", depthIntensity)
            )

            AdvancedSliderRow(
                label = "Motion Speed",
                value = speedValue,
                range = 0.5f..3f,
                onValueChange = onSpeedChange,
                valueDisplay = "${speedValue.toInt()}x"
            )

            AdvancedSliderRow(
                label = "Video Length",
                value = lengthValue,
                range = 3f..30f,
                onValueChange = onLengthChange,
                valueDisplay = "${lengthValue.toInt()}s"
            )
        }

        // --- Camera Motion Grid ---
        SettingsSection(title = "Camera Motion") {
            val motions = listOf(
                Triple("Dolly", Icons.Default.OpenWith, "Dolly"),
                Triple("Orbit", Icons.Default.Sync, "Circular Orbit"),
                Triple("Zoom", Icons.Default.ZoomIn, "Zoom In"),
                Triple("Horizontal", Icons.Default.CompareArrows, "Horizontal Pan"),
                Triple("Vertical", Icons.Default.Height, "Vertical Pan"),
                Triple("Circle", Icons.Default.ChangeHistory, "Wave / Sway")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    motions.take(3).forEach { (label, icon, mappedPath) ->
                        CameraMotionCard(
                            label = label,
                            icon = icon,
                            isSelected = motionPath == mappedPath,
                            onClick = { onMotionPathChange(mappedPath) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    motions.drop(3).forEach { (label, icon, mappedPath) ->
                        CameraMotionCard(
                            label = label,
                            icon = icon,
                            isSelected = motionPath == mappedPath,
                            onClick = { onMotionPathChange(mappedPath) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedSettingsContent(
    lengthValue: Float, onLengthChange: (Float) -> Unit,
    ssaaValue: String, onSsaaChange: (String) -> Unit,
    fpsValue: String, onFpsChange: (String) -> Unit,
    cameraStyle: String, onStyleChange: (String) -> Unit,
    motionMode: String, onModeChange: (String) -> Unit,
    speedValue: Float, onSpeedChange: (Float) -> Unit,
    focusValue: Float, onFocusChange: (Float) -> Unit,
    cinematicIntensity: Float, onCinematicIntensityChange: (Float) -> Unit,
    bokehQuality: String, onBokehChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // --- Render Section ---
        SettingsSection(title = "Render") {
            AdvancedSliderRow("Duration", lengthValue, 3f..30f, onLengthChange, "${lengthValue.toInt()}s")
            SettingsDropdown("SSAA", ssaaValue, listOf("1.0x", "2.0x", "4.0x"), onSsaaChange)
            SettingsDropdown("FPS", fpsValue, listOf("24p", "30p", "60p"), onFpsChange)
        }

        // --- Camera & Motions Section ---
        SettingsSection(title = "Camera & Motions") {
            SettingsDropdown("Style", cameraStyle, listOf("Dolly", "Orbit", "Zoom", "Pan"), onStyleChange)

            // Motion Mode Buttons (Reverse, Smooth, Loop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Reverse", "Smooth", "Loop").forEach { mode ->
                    val isSelected = motionMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) NeonPurple else Color(0xFF16162C))
                            .border(1.dp, if (isSelected) NeonPurple else BorderDark, RoundedCornerShape(10.dp))
                            .clickable { onModeChange(mode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mode, color = if (isSelected) Color.White else TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            AdvancedSliderRow("Speed", speedValue, 0.1f..5f, onSpeedChange, speedValue.toInt().toString())
            AdvancedSliderRow("Focus", focusValue, -10f..10f, onFocusChange, focusValue.toInt().toString())
        }

        // --- Cinematic Effects Section ---
        SettingsSection(title = "Cinematic Effects") {
            AdvancedSliderRow("Intensity", cinematicIntensity, 0f..2f, onCinematicIntensityChange, cinematicIntensity.toInt().toString())
            SettingsDropdown("Bokeh Quality", bokehQuality, listOf("Low (50)", "Medium (100)", "High (200)"), onBokehChange)
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
private fun AdvancedSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextMuted, fontSize = 13.sp)
            Box(
                modifier = Modifier
                    .background(Color.Black, RoundedCornerShape(6.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(valueDisplay, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = NeonPurple,
                inactiveTrackColor = BorderDark
            ),
            modifier = Modifier.height(20.dp)
        )
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))

        // Wrapper box to anchor the DropdownMenu
        Box(modifier = Modifier.weight(2f)) {
            // The visible button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(Color(0xFF16162C), RoundedCornerShape(10.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selected, color = Color.White, fontSize = 13.sp)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }

            // The actual popup menu
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF1D1B3E))
                    .border(1.dp, BorderDark)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraMotionCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.linearGradient(colors = listOf(Color(0xFF5A1EBD), Color(0xFF3B108B)))
                } else {
                    Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected) NeonPurple else BorderDark,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InsufficientCreditsDialog(
    requiredCredits: Int,
    currentCredits: Int,
    onDismiss: () -> Unit,
    onBuyCredits: () -> Unit,
    isVisible: Boolean
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF131024))
                    .border(1.dp, Brush.verticalGradient(listOf(NeonPurple.copy(alpha = 0.5f), BorderDark)), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF9034EA).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0xFFB42BFE).copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(32.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Insufficient Credits", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "You need $requiredCredits credits to generate this render, but your balance is only $currentCredits.",
                            color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onBuyCredits,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                        ) {
                            Text("Buy Credits", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}







