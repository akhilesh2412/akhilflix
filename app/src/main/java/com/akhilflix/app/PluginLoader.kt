package com.akhilflix.app

import android.content.Context
import com.akhilflix.api.AkhilFlixPlugin
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads the whole AkhilFlix app from the GitHub repo, CloudStream-style.
 *
 * Flow: repo.json -> download plugin DEX (only when the version changed)
 * -> DexClassLoader -> AkhilFlixPlugin instance. The DEX is cached in the
 * app's private files dir, so the app also opens offline with the last
 * downloaded plugin.
 */
object PluginLoader {

    /** repo.json is published by CI to the builds branch (see .github/workflows). */
    const val REPO_URL =
        "https://raw.githubusercontent.com/Akhilesh2412/akhilflix/builds/repo.json"

    private const val DEX_FILE = "akhilflix-plugin.dex"
    private const val PREFS = "akhilflix_host"
    private const val KEY_VERSION = "plugin_version"

    sealed class Result {
        data class Ready(val plugin: AkhilFlixPlugin, val version: Int) : Result()
        data class Failed(val message: String) : Result()
    }

    /** Fetch repo.json, download the DEX if needed, load the plugin. */
    suspend fun load(context: Context): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cachedVersion = prefs.getInt(KEY_VERSION, -1)
            val dexFile = File(context.filesDir, DEX_FILE)

            val repo = fetchRepo()
                ?: return@withContext Result.Failed(
                    "repo.json nahi mila. GitHub repo 'akhilflix' me code push karo " +
                        "taaki CI plugin build kare, phir retry karo."
                )

            if (repo.version != cachedVersion || !dexFile.exists()) {
                download(repo.pluginUrl, dexFile)
                prefs.edit().putInt(KEY_VERSION, repo.version).apply()
            }

            val plugin = loadPlugin(context, dexFile, repo.pluginClass)
                ?: return@withContext Result.Failed("Plugin load nahi hua (class: ${repo.pluginClass}).")
            Result.Ready(plugin, repo.version)
        } catch (e: Throwable) {
            // Catch Throwable, not just Exception: plugin loading can throw
            // Errors (NoClassDefFoundError, VerifyError) that must show an
            // error screen instead of crashing the app.
            Result.Failed(
                "Plugin load failed: ${e::class.java.simpleName}: ${e.message ?: "unknown"}"
            )
        }
    }

    private fun fetchRepo(): PluginRepo? {
        val text = httpGet(REPO_URL) ?: return null
        val json = JSONObject(text)
        return PluginRepo(
            name = json.optString("name", "AkhilFlix"),
            version = json.optInt("version", 0),
            pluginClass = json.optString(
                "pluginClass",
                "com.akhilflix.app.AkhilFlixPluginImpl"
            ),
            pluginUrl = json.optString("pluginUrl", ""),
            appName = json.optString("appName", "AkhilFlix")
        )
    }

    private fun download(url: String, dest: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Download failed: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun loadPlugin(
        context: Context,
        dexFile: File,
        pluginClass: String
    ): AkhilFlixPlugin? {
        val optimizedDir = File(context.filesDir, "dex-out").apply { mkdirs() }
        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            PluginLoader::class.java.classLoader
        )
        val clazz = classLoader.loadClass(pluginClass)
        val instance = clazz.getDeclaredConstructor().newInstance()
        return instance as? AkhilFlixPlugin
    }

    private fun httpGet(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
