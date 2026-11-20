package com.akhilflix.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.ui.components.CourseCard
import com.akhilflix.app.ui.viewmodel.HomeViewModel

/** Searches every loaded course by title, channel or category. */
@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    onCourseClick: (Course) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { viewModel.search(query) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search courses…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(results) { course ->
                CourseCard(course, onClick = onCourseClick)
            }
        }
    }
}
