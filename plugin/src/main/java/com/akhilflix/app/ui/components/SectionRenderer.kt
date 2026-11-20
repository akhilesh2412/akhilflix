package com.akhilflix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.SectionConfig

/**
 * Renders ONE home section purely from its config.
 * Unknown section types fall back to a simple horizontal row —
 * a bad backend value never crashes the app.
 */
@Composable
fun SectionRenderer(
    section: SectionConfig,
    courses: List<Course>,
    onCourseClick: (Course, String) -> Unit,
    onWatchClick: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    if (courses.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        when (section.type) {
            "banner_carousel" -> BannerSection(section, courses, onWatchClick)
            "grid" -> GridSection(section, courses, onCourseClick)
            else -> RowSection(section, courses, onCourseClick) // horizontal_row + fallback
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun RowSection(
    section: SectionConfig,
    courses: List<Course>,
    onCourseClick: (Course, String) -> Unit
) {
    SectionTitle(section.title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(courses) { course ->
            CourseCard(course, onClick = { onCourseClick(it, section.clickAction) })
        }
    }
}

@Composable
private fun GridSection(
    section: SectionConfig,
    courses: List<Course>,
    onCourseClick: (Course, String) -> Unit
) {
    SectionTitle(section.title)
    // Fixed height so it works inside a vertically scrolling home.
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.height(440.dp)
    ) {
        items(courses) { course ->
            CourseCard(course, modifier = Modifier, onClick = { onCourseClick(it, section.clickAction) })
        }
    }
}

@Composable
private fun BannerSection(
    section: SectionConfig,
    courses: List<Course>,
    onWatchClick: (Course) -> Unit
) {
    val featured = courses.first()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clickable { onWatchClick(featured) }
    ) {
        AsyncImage(
            model = featured.thumbnail.ifBlank { null },
            contentDescription = featured.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = featured.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${featured.channel} • ${featured.videoCount} videos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onWatchClick(featured) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("▶ Watch Now") }
        }
    }
}
