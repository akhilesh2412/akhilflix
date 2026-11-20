package com.akhilflix.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.Video
import com.akhilflix.app.data.remote.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val config: AppConfig = AppConfig(),
    // section id -> courses to show
    val sections: Map<String, List<Course>> = emptyMap(),
    val allCourses: List<Course> = emptyList(),
    val error: String? = null
)

class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val config = repository.loadConfig()
            val sections = mutableMapOf<String, List<Course>>()
            val all = mutableListOf<Course>()
            for (section in config.homeSections) {
                val url = repository.resolveSourceUrl(config, section.source)
                val courses = repository.loadCatalog(url, section.source)
                val filtered = if (section.category.isBlank()) {
                    courses
                } else {
                    courses.filter { it.category.equals(section.category, ignoreCase = true) }
                }.take(section.limit)
                sections[section.id] = filtered
                all += courses
            }
            _uiState.value = HomeUiState(
                loading = false,
                config = config,
                sections = sections,
                allCourses = all.distinctBy { it.id }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val fresh = repository.refreshConfig()
            if (fresh != null) load() else {
                _uiState.value = _uiState.value.copy(error = "Refresh failed — showing cached content")
            }
        }
    }

    /** All courses matching a query (title / channel / category). */
    fun search(query: String): List<Course> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return _uiState.value.allCourses
        return _uiState.value.allCourses.filter {
            it.title.lowercase().contains(q) ||
                it.channel.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
        }
    }

    fun courseById(courseId: String): Course? =
        _uiState.value.allCourses.firstOrNull { it.id == courseId }

    /** Find any video by its id across all loaded catalogs. */
    fun videoById(videoId: String): Video? =
        _uiState.value.allCourses
            .flatMap { it.videos }
            .firstOrNull { it.id == videoId }
}
