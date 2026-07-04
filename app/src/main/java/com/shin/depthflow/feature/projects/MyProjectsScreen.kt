package com.shin.depthflow.feature.projects

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import com.shin.depthflow.ui.theme.*
import com.shin.depthflow.data.ProjectEntity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.shin.depthflow.feature.workspace.UriVideoPlayer

@Composable
fun MyProjectsScreen(
    viewModel: DepthFlowViewModel,
    onBack: () -> Unit
) {
    val projects by viewModel.localProjects.collectAsState()
    var selectedProject by remember { mutableStateOf<ProjectEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(16.dp)
            .padding(bottom = 100.dp),
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
            Column {
                Text(
                    text = "My Projects",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your 3D generation history.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No projects found yet. Start creating!", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { selectedProject = project },
                        onDelete = { viewModel.deleteLocalProject(project) }
                    )
                }
            }
        }
    }

    // Video Player Dialog
    selectedProject?.let { project ->
        Dialog(onDismissRequest = { selectedProject = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceDark)
            ) {
                UriVideoPlayer(videoUri = Uri.parse(project.videoUrl), modifier = Modifier.fillMaxSize())
                
                IconButton(
                    onClick = { selectedProject = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, sectionBorderBrush),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceTop.copy(alpha = 0.1f))
    ) {
        Box {
            AsyncImage(
                model = project.imageUrl,
                contentDescription = project.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            
            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningRed, modifier = Modifier.size(16.dp))
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(8.dp)
            ) {
                Text(
                    text = project.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}







