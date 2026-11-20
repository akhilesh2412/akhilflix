package com.akhilflix.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.akhilflix.app.data.model.AddedPluginRepo
import com.akhilflix.app.data.model.AddedRepo
import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.CourseCatalog
import com.akhilflix.app.data.model.PluginInfo
import com.akhilflix.app.util.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "akhilflix")

/**
 * Single source of truth for everything the UI shows.
 *
 * Load order for the config:
 *  1. Backend (CONFIG_URL) — fresh, server-driven.
 *  2. Local file cache — last good config, works offline.
 *  3. Bundled assets/akhilflix.json — first-launch fallback.
 *
 * Catalogs (courses) are fetched per source URL from the config and
 * cached in memory for the session.
 */
class AppRepository(private val context: Context) {

    private val gson = Gson()
    private val api = ApiFactory.api
    private val myListKey = stringSetPreferencesKey("my_list_course_ids")
    private val addedReposKey = stringPreferencesKey("added_repos_json")
    private val addedPluginReposKey = stringPreferencesKey("added_plugin_repos_json")

    // In-memory plugin-repo cache: repo.json URL -> (display name, plugins)
    private val pluginRepoCache = mutableMapOf<String, Pair<String, List<PluginInfo>>>()

    // In-memory catalog cache: source key -> courses
    private val catalogCache = mutableMapOf<String, List<Course>>()

    /** Load the server-driven config with offline fallbacks. Never throws. */
    suspend fun loadConfig(): AppConfig {
        // 1. Try backend.
        runCatching { api.fetchConfig(Constants.CONFIG_URL) }.onSuccess { config ->
            saveConfigCache(config)
            return config
        }
        // 2. Try local file cache.
        runCatching { readConfigCache() }.onSuccess { return it }
        // 3. Bundled fallback.
        runCatching {
            val json = context.assets.open("akhilflix.json")
                .bufferedReader().use { it.readText() }
            return gson.fromJson(json, AppConfig::class.java)
        }
        // 4. Empty default — app still opens, shows empty home.
        return AppConfig()
    }

    /** Force a fresh fetch (pull-to-refresh). Returns null on failure. */
    suspend fun refreshConfig(): AppConfig? {
        return runCatching {
            val config = api.fetchConfig(Constants.CONFIG_URL)
            saveConfigCache(config)
            config
        }.getOrNull()
    }

    private fun saveConfigCache(config: AppConfig) {
        runCatching {
            File(context.filesDir, Constants.CONFIG_CACHE_FILE)
                .writeText(gson.toJson(config))
        }
    }

    private fun readConfigCache(): AppConfig {
        val file = File(context.filesDir, Constants.CONFIG_CACHE_FILE)
        return gson.fromJson(file.readText(), AppConfig::class.java)
    }

    /** Fetch a course catalog for a source key (URL resolved from config). */
    suspend fun loadCatalog(sourceUrl: String, sourceKey: String): List<Course> {
        catalogCache[sourceKey]?.let { return it }
        val courses = runCatching { api.fetchCatalog(sourceUrl).courses }
            .getOrElse { emptyList() }
        catalogCache[sourceKey] = courses
        return courses
    }

    /** Resolve a source key to its URL using the config, with built-in defaults. */
    fun resolveSourceUrl(config: AppConfig, sourceKey: String): String {
        config.sources[sourceKey]?.let { return it }
        return when (sourceKey) {
            "dhruv" -> Constants.DHRUV_CATALOG_URL
            else -> Constants.PROGRAMMING_CATALOG_URL
        }
    }

    /** Fetch a catalog without caching — used to validate a repo the user adds. Null on any failure. */
    suspend fun fetchCatalogRaw(url: String): List<Course>? {
        return runCatching { api.fetchCatalog(url).courses }.getOrNull()
    }

    // --- User-added CloudStream repos (local only, DataStore) ---

    val addedRepos: Flow<List<AddedRepo>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[addedReposKey] ?: return@map emptyList<AddedRepo>()
            runCatching {
                gson.fromJson<List<AddedRepo>>(
                    json,
                    object : TypeToken<List<AddedRepo>>() {}.type
                ) ?: emptyList()
            }.getOrElse { emptyList() }
        }

    suspend fun getAddedRepos(): List<AddedRepo> = addedRepos.first()

    suspend fun saveAddedRepos(repos: List<AddedRepo>) {
        context.dataStore.edit { prefs ->
            prefs[addedReposKey] = gson.toJson(repos)
        }
    }

    // --- User-added CloudStream plugin repos (local only, DataStore) ---

    val addedPluginRepos: Flow<List<AddedPluginRepo>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[addedPluginReposKey] ?: return@map emptyList<AddedPluginRepo>()
            runCatching {
                gson.fromJson<List<AddedPluginRepo>>(
                    json,
                    object : TypeToken<List<AddedPluginRepo>>() {}.type
                ) ?: emptyList()
            }.getOrElse { emptyList() }
        }

    suspend fun getAddedPluginRepos(): List<AddedPluginRepo> = addedPluginRepos.first()

    suspend fun saveAddedPluginRepos(repos: List<AddedPluginRepo>) {
        context.dataStore.edit { prefs ->
            prefs[addedPluginReposKey] = gson.toJson(repos)
        }
    }

    /**
     * Fetch a CloudStream plugin repo: repo.json -> pluginLists -> plugins.json.
     * Returns (display name, plugins) or null when the URL is not a plugin
     * repo / has no plugins. If the URL itself is a plugin list (a JSON
     * array), that is used directly.
     */
    suspend fun fetchPluginRepo(repoJsonUrl: String): Pair<String, List<PluginInfo>>? {
        pluginRepoCache[repoJsonUrl]?.let { return it }
        // 1. The URL itself might be a plugin list (a JSON array).
        val direct = runCatching { api.fetchPlugins(repoJsonUrl) }.getOrNull()
        if (!direct.isNullOrEmpty()) {
            val result = "Plugins" to direct
            pluginRepoCache[repoJsonUrl] = result
            return result
        }
        // 2. Otherwise treat it as a repo manifest (repo.json).
        val result = runCatching {
            val manifest = api.fetchRepoJson(repoJsonUrl)
            val name = manifest.name?.ifBlank { null } ?: "Plugin Repo"
            val all = mutableListOf<PluginInfo>()
            for (listUrl in manifest.pluginLists ?: emptyList()) {
                runCatching { api.fetchPlugins(listUrl) }.onSuccess { all += it }
            }
            if (all.isEmpty()) null else name to all
        }.getOrNull()
        if (result != null) pluginRepoCache[repoJsonUrl] = result
        return result
    }

    // --- My List (local only) ---

    val myListIds: Flow<Set<String>> =
        context.dataStore.data.map { it[myListKey] ?: emptySet() }

    suspend fun isInMyList(courseId: String): Boolean =
        myListIds.first().contains(courseId)

    suspend fun toggleMyList(courseId: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[myListKey] ?: emptySet()).toMutableSet()
            if (current.contains(courseId)) current.remove(courseId)
            else current.add(courseId)
            prefs[myListKey] = current
        }
    }
}
