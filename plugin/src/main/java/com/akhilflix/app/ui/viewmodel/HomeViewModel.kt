package com.akhilflix.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilflix.app.data.model.AddedPluginRepo
import com.akhilflix.app.data.model.AddedRepo
import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.PluginInfo
import com.akhilflix.app.data.model.SectionConfig
import com.akhilflix.app.data.model.Video
import com.akhilflix.app.data.remote.AppRepository
import com.akhilflix.app.util.RepoResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One CloudStream plugin repo with its loaded plugins, for the UI. */
data class PluginRepoUi(
    val repo: AddedPluginRepo,
    val plugins: List<PluginInfo>
)

data class HomeUiState(
    val loading: Boolean = true,
    val config: AppConfig = AppConfig(),
    // section id -> courses to show
    val sections: Map<String, List<Course>> = emptyMap(),
    val allCourses: List<Course> = emptyList(),
    val error: String? = null,
    // Sections built from repos the user added himself (one row per
    // category of each added repo), rendered after the config sections.
    val dynamicSections: List<SectionConfig> = emptyList(),
    val addedRepos: List<AddedRepo> = emptyList(),
    val addedRepoCounts: Map<String, Int> = emptyMap(),
    // CloudStream plugin repos the user added (repo.json), with their
    // loaded plugins, rendered on Home after the course sections.
    val pluginRepos: List<PluginRepoUi> = emptyList(),
    // Last add/remove result, shown on the My Repos screen.
    val repoStatus: String? = null
)

class HomeViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loadInternal() }
    }

    private suspend fun loadInternal() {
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
        // Repos the user added from GitHub: one home row per category.
        val addedRepos = repository.getAddedRepos()
        val dynamicSections = mutableListOf<SectionConfig>()
        val addedCounts = mutableMapOf<String, Int>()
        for (repo in addedRepos) {
            val courses = repository.loadCatalog(repo.catalogUrl, "repo:${repo.id}")
            addedCounts[repo.id] = courses.size
            all += courses
            val byCategory = courses.groupBy { it.category.ifBlank { "Other" } }
            for ((category, catCourses) in byCategory) {
                val section = SectionConfig(
                    id = "repo-${repo.id}-${category.lowercase().replace(Regex("[^a-z0-9]+"), "-")}",
                    title = "${repo.name} — $category",
                    type = "horizontal_row",
                    source = "repo:${repo.id}",
                    category = category,
                    limit = 20
                )
                dynamicSections += section
                sections[section.id] = catCourses.take(20)
            }
        }
        _uiState.value = HomeUiState(
            loading = false,
            config = config,
            sections = sections,
            allCourses = all.distinctBy { it.id },
            dynamicSections = dynamicSections,
            addedRepos = addedRepos,
            addedRepoCounts = addedCounts,
            pluginRepos = loadPluginRepos(),
            repoStatus = _uiState.value.repoStatus
        )
    }

    /** Load every saved CloudStream plugin repo (cached in the repository). */
    private suspend fun loadPluginRepos(): List<PluginRepoUi> {
        val result = mutableListOf<PluginRepoUi>()
        for (repo in repository.getAddedPluginRepos()) {
            val (name, plugins) = repository.fetchPluginRepo(repo.repoJsonUrl) ?: continue
            result += PluginRepoUi(repo.copy(name = name), plugins)
        }
        return result
    }

    fun refresh() {
        viewModelScope.launch {
            val fresh = repository.refreshConfig()
            if (fresh != null) loadInternal() else {
                _uiState.value = _uiState.value.copy(error = "Refresh failed — showing cached content")
            }
        }
    }

    /** Add any CloudStream repo from a GitHub link the user pastes.
     *  Tries the course catalog first, then the plugin repo (repo.json). */
    fun addRepo(input: String) {
        viewModelScope.launch {
            val resolved = RepoResolver.resolve(input)
            if (resolved == null) {
                _uiState.value = _uiState.value.copy(
                    repoStatus = "Ye link samajh nahi aaya — GitHub repo ka link paste karo (jaise github.com/Akhilesh2412/programming-courses)"
                )
                return@launch
            }
            // 1. Course repo: youtube_courses.json on main, then master.
            val current = repository.getAddedRepos()
            if (current.any { it.id == resolved.id }) {
                _uiState.value = _uiState.value.copy(
                    repoStatus = "${resolved.name} pehle se added hai"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(repoStatus = "${resolved.name} load ho raha hai...")
            for (url in resolved.catalogCandidates) {
                val courses = repository.fetchCatalogRaw(url)
                if (!courses.isNullOrEmpty()) {
                    repository.saveAddedRepos(current + AddedRepo(resolved.id, resolved.name, url))
                    loadInternal()
                    _uiState.value = _uiState.value.copy(
                        repoStatus = "${resolved.name} add ho gaya — ${courses.size} courses"
                    )
                    return@launch
                }
            }
            // 2. No course catalog — try it as a CloudStream plugin repo
            // (repo.json -> pluginLists -> plugins.json).
            val pluginRepos = repository.getAddedPluginRepos()
            if (pluginRepos.any { it.id == resolved.id }) {
                _uiState.value = _uiState.value.copy(
                    repoStatus = "${resolved.name} pehle se added hai"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(repoStatus = "Plugin repo load ho raha hai...")
            for (url in resolved.repoJsonCandidates) {
                val found = repository.fetchPluginRepo(url) ?: continue
                val (repoName, plugins) = found
                if (plugins.isEmpty()) continue
                repository.saveAddedPluginRepos(
                    pluginRepos + AddedPluginRepo(resolved.id, repoName, url)
                )
                loadInternal()
                _uiState.value = _uiState.value.copy(
                    repoStatus = "$repoName add ho gaya — ${plugins.size} plugins"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                repoStatus = "Is repo me na courses ka data (youtube_courses.json) mila, na CloudStream plugins (repo.json) — link check karo"
            )
        }
    }

    /** Remove a plugin repo the user added. */
    fun removePluginRepo(repoId: String) {
        viewModelScope.launch {
            val current = repository.getAddedPluginRepos()
            val removed = current.firstOrNull { it.id == repoId } ?: return@launch
            repository.saveAddedPluginRepos(current.filterNot { it.id == repoId })
            loadInternal()
            _uiState.value = _uiState.value.copy(repoStatus = "${removed.name} hata diya")
        }
    }

    /** Remove a repo the user added. Built-in repos (programming / Dhruv) can't be removed. */
    fun removeRepo(repoId: String) {
        viewModelScope.launch {
            val current = repository.getAddedRepos()
            val removed = current.firstOrNull { it.id == repoId } ?: return@launch
            repository.saveAddedRepos(current.filterNot { it.id == repoId })
            loadInternal()
            _uiState.value = _uiState.value.copy(repoStatus = "${removed.name} hata diya")
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
