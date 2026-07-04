package com.shin.depthflow.feature.home

// ---> ADD THESE IMPORTS <---

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shin.depthflow.R
import com.shin.depthflow.feature.home.components.BundledVideoPlayer
import com.shin.depthflow.feature.home.components.FeatureCardsSection
import com.shin.depthflow.feature.home.components.ShowcaseVideo
import com.shin.depthflow.feature.home.components.UpgradeBannerCard
import com.shin.depthflow.feature.home.components.VideoLibraryScreen
import com.shin.depthflow.feature.home.components.innerGlow
import com.shin.depthflow.feature.home.components.outerGlow
import com.shin.depthflow.ui.components.GlassmorphicButton
import com.shin.depthflow.ui.theme.BackgroundDark
import com.shin.depthflow.ui.theme.BorderHighlight
import com.shin.depthflow.ui.theme.DepthFlowBlue
import com.shin.depthflow.ui.theme.DepthFlowLightPurple
import com.shin.depthflow.ui.theme.DepthFlowPurple
import com.shin.depthflow.ui.theme.SurfaceDark
import com.shin.depthflow.ui.theme.TextPrimary
import com.shin.depthflow.ui.theme.TextSecondary
import com.shin.depthflow.ui.theme.sectionBorderBrush
import com.shin.depthflow.ui.theme.sectionContainerInside
import com.shin.depthflow.ui.theme.sectionContainerOuter
import com.shin.depthflow.viewmodel.DepthFlowViewModel


@Composable
fun DepthFlowHomeScreen(
    onNavigateToWorkspace: () -> Unit,
    viewModel: DepthFlowViewModel = viewModel()
) {
    // Use a Box as the root
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { HeroActionSection(onNavigateToWorkspace) }
                item { FeatureCardsSection() }
                item {
                    VideoLibraryScreen(
                        onVideoSelect = { _ ->
                            // The expansion is now handled internally in VideoLibraryScreen
                        }
                    )
                }
                item {
                    UpgradeBannerCard(onClick = {})
                }
                item {
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
fun HeroActionSection(onNavigateToWorkspace: () -> Unit, modifier: Modifier = Modifier) {
    // Outer border catch-light for the main glassmorphic card frame
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            BorderHighlight.copy(alpha = 0.5f),
            Color.Transparent
        )
    )

    // Outer container
    Box(
        modifier = modifier
            .fillMaxWidth()
            .outerGlow( // 1. Outer glow goes BEFORE the clip
                color = DepthFlowPurple.copy(.5f),
                cornerRadius = 24.dp,
                blurRadius = 14.dp
            )
            .clip(RoundedCornerShape(24.dp)) // 2. Clip is applied
            .background(sectionContainerInside) // 3. Background is applied
            .innerGlow( // 4. Inner glow goes AFTER the clip
                color = sectionContainerOuter,
                cornerRadius = 24.dp,
                blurRadius = 16.dp
            )
            .border(
                width = 1.dp,
                brush = sectionBorderBrush,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        // 1. Full-Bleed Background Image
        Image(
            painter = painterResource(id = R.drawable.depthflow_hero2), // Ensure your image resource name matches
            contentDescription = "Hero Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 2. Glassmorphic Diagonal Gradient Overlay
        // Sweeps from a deep dark bottom-left up to a completely transparent top-right
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            BackgroundDark.copy(alpha = 0.95f), // Solid dark behind the bottom text/button
                            SurfaceDark.copy(alpha = 0.75f),    // Mid transition
                            Color.Transparent                   // Fades out so the image pops on top right
                        ),
                        start = Offset(0f, Float.POSITIVE_INFINITY), // Bottom Left
                        end = Offset(Float.POSITIVE_INFINITY, 0f)    // Top Right
                    )
                )
        )

        // 3. Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Text Container (Constrained width so it wraps nicely)
            Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                Text(
                    text = buildAnnotatedString {
                        append("Transform Images\n")
                        withStyle(style = SpanStyle(color = DepthFlowLightPurple)) {
                            append("to 3D Depth\n")
                        }
                        append("Instantly.")
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp
                    )
                )

                Text(
                    text = "AI-powered platform to convert static images into immersive 3D depth motion visuals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(vertical = 8.dp)
                )
            }

            // Glassmorphic Button
            GlassmorphicButton(
                text = "Create 3D Image",
                icon = Icons.Default.Add,
                onClick = onNavigateToWorkspace,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
    }
}








