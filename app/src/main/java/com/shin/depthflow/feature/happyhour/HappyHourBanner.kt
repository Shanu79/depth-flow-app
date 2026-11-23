package com.shin.depthflow.feature.happyhour

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.theme.DepthFlowBlue
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.TextMuted
import com.shin.depthflow.util.HappyHourTime
import com.shin.depthflow.viewmodel.HappyHourViewModel

/**
 * Compact entry point shown on Home and Workspace. It only appears when it is actionable:
 * a session is running, or the user can start one. Everything shown comes from the server status.
 */
@Composable
fun HappyHourBanner(
    viewModel: HappyHourViewModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsState()
    val secondsLeft by viewModel.secondsLeft.collectAsState()

    val st = status ?: return
    if (!st.eligible) return
    val isActive = st.active && secondsLeft > 0
    if (!isActive && !st.can_activate_this_month) return

    val title = if (isActive) "Happy Hour is live" else "Happy Hour is available"
    val subtitle = if (isActive) {
        "Unlimited: ${HappyHourTime.formatCountdown(secondsLeft)} left, ${st.remaining_video_allowance} videos left"
    } else {
        "Tap to start (${st.available_tokens} token${if (st.available_tokens == 1) "" else "s"} left)"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(DepthFlowPurple.copy(alpha = 0.35f), DepthFlowBlue.copy(alpha = 0.25f))
                )
            )
            .border(1.dp, DepthFlowPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onOpen() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
