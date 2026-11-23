package com.shin.depthflow.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.data.InboxItem
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark
import com.shin.depthflow.ui.theme.TextMuted
import com.shin.depthflow.util.HappyHourTime
import com.shin.depthflow.viewmodel.HappyHourViewModel
import com.shin.depthflow.viewmodel.ContentViewModel
import com.shin.depthflow.ui.promo.WhatsNewCard

/**
 * In-app inbox. Also the fallback when a push is missed or notification permission is off.
 * The admin's What's New releases are listed on top as banners; tapping one opens its detail page.
 */
@Composable
fun NotificationsScreen(
    viewModel: HappyHourViewModel,
    contentViewModel: ContentViewModel,
    onBack: () -> Unit,
    onOpenHappyHour: () -> Unit,
    onOpenWhatsNew: (releaseId: Int) -> Unit
) {
    val items by viewModel.inbox.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()
    val releases by contentViewModel.releases.collectAsState()
    val hasUnseenWhatsNew by contentViewModel.hasUnseenWhatsNew.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInbox()
        contentViewModel.refreshWhatsNew()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // No statusBarsPadding: the Scaffold's inner padding already clears the status bar.
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                Text("Notifications", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (unread > 0) {
                TextButton(onClick = { viewModel.markAllRead() }) {
                    Text("Mark all read", color = DepthFlowPurple, fontSize = 13.sp)
                }
            }
        }

        if (items.isEmpty() && releases.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications yet", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (releases.isNotEmpty()) {
                    item(key = "whats_new_header") { SectionHeader("What's New") }
                    items(releases.size, key = { "release_${releases[it].id}" }) { index ->
                        val release = releases[index]
                        WhatsNewCard(
                            release = release,
                            isLatest = index == 0,
                            // The "new" announcement is the latest release.
                            isNew = hasUnseenWhatsNew && index == 0,
                            onClick = { onOpenWhatsNew(release.id) }
                        )
                    }
                }
                if (items.isNotEmpty()) {
                    if (releases.isNotEmpty()) item(key = "inbox_header") { SectionHeader("Notifications") }
                    items(items, key = { it.id }) { item ->
                        NotificationRow(item) {
                            if (item.read_at == null) viewModel.markRead(item.id)
                            if (item.data?.get("route") == "happy_hour") onOpenHappyHour()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: InboxItem, onClick: () -> Unit) {
    val isUnread = item.read_at == null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isUnread) DepthFlowPurple else Color.Transparent)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title.orEmpty(), color = Color.White, fontSize = 15.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
            )
            Text(item.body.orEmpty(), color = TextMuted, fontSize = 13.sp)
            Text(HappyHourTime.formatDate(item.created_at), color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
    )
}
