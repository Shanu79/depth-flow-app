package com.shin.depthflow.feature.happyhour

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.data.HappyHourSession
import com.shin.depthflow.data.HappyHourStatus
import com.shin.depthflow.ui.theme.DepthFlowBlue
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark
import com.shin.depthflow.ui.theme.TextMuted
import com.shin.depthflow.util.HappyHourTime
import com.shin.depthflow.viewmodel.HappyHourViewModel

@Composable
fun HappyHourScreen(
    viewModel: HappyHourViewModel,
    onBack: () -> Unit,
    onNavigateToPricing: () -> Unit,
    onNavigateToWorkspace: () -> Unit
) {
    val status by viewModel.status.collectAsState()
    val secondsLeft by viewModel.secondsLeft.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isActivating by viewModel.isActivating.collectAsState()
    val history by viewModel.history.collectAsState()

    var showConfirm by remember { mutableStateOf(false) }

    // Pushes are silent without this on Android 13+. The in-app inbox works either way.
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* nothing to do: the inbox still has every notice */ }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
        viewModel.loadHistory()
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = SurfaceDark,
            title = { Text("Start Happy Hour?", color = Color.White) },
            text = {
                Text(
                    "This uses 1 token and starts 60 minutes of unlimited generation. " +
                        "It cannot be paused or refunded.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.activate()
                }) { Text("Start", color = DepthFlowPurple, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // No statusBarsPadding: the Scaffold's inner padding already clears the status bar.
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Happy Hour", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        val st = status
        when {
            st == null && isLoading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DepthFlowPurple)
            }

            st == null -> InfoCard("Couldn't load Happy Hour. Check your connection and try again.") {
                TextButton(onClick = { viewModel.refresh() }) { Text("Retry", color = DepthFlowPurple) }
            }

            !st.eligible -> NotEligibleCard(onNavigateToPricing)

            st.active && secondsLeft > 0 -> ActiveCard(st, secondsLeft, onNavigateToWorkspace)

            else -> {
                IdleCard(st, isActivating, onStart = { showConfirm = true })
                // Show why a session can't start right now
                blockedMessage(st)?.let { InfoCard(it) }
            }
        }

        RulesCard()

        if (history.isNotEmpty()) {
            Text("Past sessions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            history.take(10).forEach { SessionRow(it) }
        }
    }
}

private fun blockedMessage(st: HappyHourStatus): String? = when (st.blocked_reason) {
    "NO_TOKENS" -> "No tokens left. Monthly plans get a new token after each renewal."
    "MONTHLY_LIMIT" -> {
        val next = HappyHourTime.formatDate(st.next_activation_available_at)
        "Already used this month." + if (next.isNotEmpty()) " Next activation: $next." else ""
    }
    "SESSION_ACTIVE" -> "A session is already running."
    else -> null
}

@Composable
private fun CardShell(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(DepthFlowPurple.copy(alpha = 0.25f), DepthFlowBlue.copy(alpha = 0.15f))
                )
            )
            .border(1.dp, DepthFlowPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun InfoCard(text: String, action: (@Composable () -> Unit)? = null) {
    CardShell {
        Text(text, color = TextMuted, fontSize = 14.sp)
        action?.invoke()
    }
}

@Composable
private fun NotEligibleCard(onUpgrade: () -> Unit) {
    CardShell {
        Text("Happy Hour is for subscribers", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            "Monthly and Yearly subscribers get Happy Hour tokens: 60 minutes of unlimited 3D generation.",
            color = TextMuted, fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        PrimaryButton("View plans", enabled = true, onClick = onUpgrade)
    }
}

@Composable
private fun ActiveCard(st: HappyHourStatus, secondsLeft: Long, onGenerate: () -> Unit) {
    CardShell {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("Happy Hour is live", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            HappyHourTime.formatCountdown(secondsLeft),
            color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        val used = st.session?.successful_video_generations
        val max = st.session?.max_video_generations
        Text(
            buildString {
                append("${st.remaining_video_allowance} video${if (st.remaining_video_allowance == 1) "" else "s"} left")
                if (used != null && max != null) append(" ($used of $max used)")
            },
            color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Generate now", enabled = true, onClick = onGenerate)
    }
}

@Composable
private fun IdleCard(st: HappyHourStatus, isActivating: Boolean, onStart: () -> Unit) {
    CardShell {
        Text("Unlimited generation for 60 minutes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val plan = st.subscription_type?.replaceFirstChar { it.uppercase() } ?: "Subscription"
        Text(
            "$plan plan: ${st.available_tokens} of ${st.total_allocated_tokens} token" +
                if (st.total_allocated_tokens == 1) " left" else "s left",
            color = TextMuted, fontSize = 14.sp
        )
        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            text = if (isActivating) "Starting..." else "Start Happy Hour",
            enabled = st.can_activate_this_month && !isActivating,
            onClick = onStart
        )
    }
}

@Composable
private fun RulesCard() {
    CardShell {
        Text("How it works", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        listOf(
            "Yearly: 4 tokens per year, at most one activation per calendar month.",
            "Monthly: 1 token per billing period. Unused tokens don't roll over.",
            "A session lasts exactly 60 minutes and has a video limit.",
            "Closing the app doesn't pause or extend the timer."
        ).forEach { Text("• $it", color = TextMuted, fontSize = 13.sp) }
    }
}

@Composable
private fun SessionRow(s: HappyHourSession) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(HappyHourTime.formatDate(s.started_at), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            val used = s.successful_video_generations
            val max = s.max_video_generations
            if (used != null && max != null) {
                Text("$used of $max videos", color = TextMuted, fontSize = 12.sp)
            }
        }
        Text(
            when (s.status) {
                "ACTIVE" -> "Active"
                "EXPIRED" -> "Time up"
                "LIMIT_REACHED" -> "Limit reached"
                "INVALIDATED" -> "Ended"
                else -> s.status.orEmpty()
            },
            color = TextMuted, fontSize = 12.sp
        )
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(DepthFlowPurple, DepthFlowBlue))
                else Brush.horizontalGradient(listOf(Color(0xFF3A3A55), Color(0xFF3A3A55)))
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else TextMuted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
