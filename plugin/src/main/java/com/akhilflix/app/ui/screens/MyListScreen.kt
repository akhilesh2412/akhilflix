package com.akhilflix.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.remote.AppRepository
import com.akhilflix.app.ui.components.CourseCard
import com.akhilflix.app.ui.viewmodel.HomeViewModel

/** Courses the user saved locally (DataStore). */
@Composable
fun MyListScreen(
    viewModel: HomeViewModel,
    repository: AppRepository,
    onCourseClick: (Course) -> Unit
) {
    val savedIds by repository.myListIds.collectAsState(initial = emptySet())
    val saved = viewModel.uiState.collectAsState().value.allCourses
        .filter { savedIds.contains(it.id) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("My List", style = MaterialTheme.typography.headlineMedium)
        if (saved.isEmpty()) {
            Text(
                "Nothing saved yet. Open a course and tap + My List.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(saved) { course -> CourseCard(course, onClick = onCourseClick) }
        }
    }
}
