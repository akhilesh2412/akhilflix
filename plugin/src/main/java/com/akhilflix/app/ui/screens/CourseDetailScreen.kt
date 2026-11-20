package com.akhilflix.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.Video
import com.akhilflix.app.data.remote.AppRepository
import com.akhilflix.app.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

/** Course detail: backdrop, meta, description, episode/video list. */
@Composable
fun CourseDetailScreen(
    viewModel: HomeViewModel,
    repository: AppRepository,
    courseId: String,
    onVideoClick: (Video) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val course: Course? = viewModel.courseById(courseId)
    val scope = rememberCoroutineScope()
    val savedIds by repository.myListIds.collectAsState(initial = emptySet())

    if (course == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
            Text("Course not found. Pull to refresh on Home and try again.")
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                AsyncImage(
                    model = course.thumbnail.ifBlank { null },
                    contentDescription = course.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${course.channel} • ${course.category} • ${course.videoCount} videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (course.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    course.videos.firstOrNull()?.let { first ->
                        Button(onClick = { onVideoClick(first) }) { Text("▶ Play First Video") }
                    }
                    Spacer(Modifier.weight(1f))
                    val inList = savedIds.contains(course.id)
                    Button(onClick = {
                        scope.launch { repository.toggleMyList(course.id) }
                    }) { Text(if (inList) "✓ In My List" else "+ My List") }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Videos",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        items(course.videos) { video ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVideoClick(video) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AsyncImage(
                    model = video.thumbnail.ifBlank { null },
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 120.dp, height = 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(video.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    Text(
                        video.durationLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
        }
    }
}
