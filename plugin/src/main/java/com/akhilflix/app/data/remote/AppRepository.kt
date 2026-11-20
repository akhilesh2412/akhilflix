package com.akhilflix.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.Course
import com.akhilflix.app.data.model.CourseCatalog
import com.akhilflix.app.util.Constants
import com.google.gson.Gson
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
