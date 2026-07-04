package com.shin.depthflow.feature.home.components

import com.shin.depthflow.util.Logger
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.net.toUri
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.foundation.Image // Ensure this is imported
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ShowcaseVideo(
    val title: String,
    val time: String? = null,
    @RawRes val videoResId: Int,
)

@Composable
fun BundledVideoPlayer(
    @RawRes videoResId: Int,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    showControls: Boolean = false, // Set to true if you want standard play/pause buttons
    isLooping: Boolean = true      // Great for seamless background videos
) {
    val context = LocalContext.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Construct the exact URI path to your raw resource
            val uri = "android.resource://${context.packageName}/$videoResId".toUri()

            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            playWhenReady = autoPlay
            prepare()
        }
    }

    // Clean up the ExoPlayer when this composable leaves the screen
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Wrap the standard PlayerView in a Compose AndroidView
    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = showControls

                // Optional: If it's a background video, hide the buffering spinner
                // setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            }
        },
        modifier = modifier
    )
}

@Composable
fun VideoShowcaseCard(
    video: ShowcaseVideo,
    onVideoClick: (ShowcaseVideo) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false // New parameter
) {
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF3F466E).copy(alpha = 0.6f), Color.Transparent)
    )

    // FIX: Fetch the thumbnail natively, bypassing Coil
    val thumbnailBitmap = rememberRawVideoThumbnail(videoResId = video.videoResId)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF141426))
            .border(width = 1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(16.dp))
            .clickable { onVideoClick(video) }
    ) {
        // Thumbnail Area Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
        ) {
            if (isExpanded) {
                // Show the actual video player when expanded
                BundledVideoPlayer(
                    videoResId = video.videoResId,
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = true,
                    showControls = false,
                    isLooping = true
                )
            } else {
                // 1. Base Layer: The Native Thumbnail
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback background while the frame is extracting
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E38))
                    )
                }

                // 2. Middle Layer: Dark overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )

                // 3. Top Layer: Play Icon centered
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Preview",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            // 4. Top Layer: 360 Badge aligned to Top-Right
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF9042FF), Color(0xFF6711FF))))
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled._360,
                    contentDescription = "360 View",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Bottom Text Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if(video.title == "Recent Creations") video.time?.let {
                    Text(
                        text = it,
                        color = Color(0xFF8A8A9D),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if(video.title == "Recent Creations") Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF8A8A9D),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun rememberRawVideoThumbnail(videoResId: Int): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(videoResId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(videoResId) {
        withContext(Dispatchers.IO) { // Run on background thread to prevent UI stutter
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                // Use AssetFileDescriptor to get exact byte offsets inside the APK
                val afd = context.resources.openRawResourceFd(videoResId)
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

                // Extract frame
                val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    bitmap = frame.asImageBitmap()
                }
                afd.close()
            } catch (e: Exception) {
                Logger.e("VideoThumbnail", "Failed to extract frame for $videoResId", e)
            } finally {
                retriever?.release()
            }
        }
    }

    return bitmap
}








