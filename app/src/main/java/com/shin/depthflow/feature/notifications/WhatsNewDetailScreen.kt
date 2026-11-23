package com.shin.depthflow.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shin.depthflow.ui.promo.LinkRouter
import com.shin.depthflow.ui.promo.WhatsNewButtons
import com.shin.depthflow.ui.promo.WhatsNewFeatureList
import com.shin.depthflow.ui.promo.WhatsNewLilac
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.TextMuted
import com.shin.depthflow.viewmodel.ContentViewModel

/**
 * Full page for one What's New release, opened from its banner in Notifications. Shows the badge,
 * title, subtitle, every feature and the release's buttons. Opening it marks the announcement seen.
 *
 * Button links: `https://` opens externally, `/path` goes to that app screen, an empty link goes back.
 */
@Composable
fun WhatsNewDetailScreen(
    releaseId: Int,
    contentViewModel: ContentViewModel,
    onBack: () -> Unit,
    onInAppPath: (String) -> Unit
) {
    val releases by contentViewModel.releases.collectAsState()
    val release = releases.firstOrNull { it.id == releaseId }

    // Process death or a cold deep link: the list may not be loaded yet.
    LaunchedEffect(releaseId) {
        if (release == null) contentViewModel.refreshWhatsNew()
        contentViewModel.markWhatsNewSeen()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
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
            Text("What's New", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (release == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (releases.isEmpty()) {
                    CircularProgressIndicator(color = DepthFlowPurple)
                } else {
                    Text("This update is no longer available.", color = TextMuted, fontSize = 14.sp)
                }
            }
        } else {
            WhatsNewDetailBody(release, onBack, onInAppPath)
        }
    }
}

@Composable
private fun WhatsNewDetailBody(
    release: com.shin.depthflow.data.WhatsNewRelease,
    onBack: () -> Unit,
    onInAppPath: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!release.badge_text.isNullOrBlank()) {
                Text(
                    text = release.badge_text,
                    color = WhatsNewLilac,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DepthFlowPurple.copy(alpha = 0.25f))
                        .border(1.dp, DepthFlowPurple.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Text(
                text = release.title.orEmpty(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 34.sp
            )
            if (!release.subtitle.isNullOrBlank()) {
                Text(release.subtitle, color = TextMuted, fontSize = 15.sp, lineHeight = 22.sp)
            }
            WhatsNewFeatureList(release, Modifier.padding(top = 6.dp))
            WhatsNewButtons(
                release = release,
                onLink = { link ->
                    if (link.isNullOrBlank()) onBack()
                    else LinkRouter.handle(context, link, onInAppPath)
                },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
