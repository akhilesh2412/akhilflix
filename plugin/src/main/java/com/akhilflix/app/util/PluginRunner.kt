package com.akhilflix.app.util

import android.content.Context
import com.akhilflix.app.data.model.PluginInfo
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.plugins.BasePlugin
import dalvik.system.PathClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * The plugin engine: runs a real CloudStream .cs3 plugin inside AkhilFlix.
 *
 * Mirrors how CloudStream itself loads plugins (PluginManager.loadPlugin):
 * the .cs3 is a zip with manifest.json + classes.dex. It is loaded with a
 * PathClassLoader whose parent is the app classloader — so the plugin's
 * code resolves MainAPI, BasePlugin, extractors, etc. from the
 * CloudStream API library bundled in the host app. The plugin class
 * (from manifest.json) is instantiated, its load() registers its
 * providers into APIHolder, and those providers are what the UI drives
 * (getMainPage / search / load / loadLinks).
 *
 * Loaded providers are cached per plugin so tapping the same plugin
 * again does not re-download or re-load it.
 */
object PluginRunner {

    private val providersByPlugin = mutableMapOf<String, List<MainAPI>>()
    private val client = OkHttpClient()

    /** Load a plugin from its .cs3 (downloaded + cached) and return its providers. */
    suspend fun loadProviders(
        context: Context,
        plugin: PluginInfo
    ): Result<List<MainAPI>> = withContext(Dispatchers.IO) {
        runCatching {
            val key = plugin.internalName?.ifBlank { null } ?: plugin.displayName()
            providersByPlugin[key]?.let { return@runCatching it }

            val file = downloadCs3(context, plugin)
            val loader = PathClassLoader(file.absolutePath, context.classLoader)
            val manifestJson = loader.getResourceAsStream("manifest.json")
                ?.bufferedReader()?.readText()
                ?: throw IllegalStateException("manifest.json nahi mila — ye valid .cs3 nahi hai")
            val manifest = JSONObject(manifestJson)
            if (manifest.optBoolean("requiresResources", false)) {
                throw IllegalStateException("Is plugin ko resources chahiye — abhi support nahi hai")
            }
            val className = manifest.optString("pluginClassName").ifBlank { null }
                ?: throw IllegalStateException("manifest me pluginClassName nahi hai")
            val clazz = loader.loadClass(className) as Class<out BasePlugin>
            val instance = clazz.getDeclaredConstructor().newInstance()
            instance.filename = file.absolutePath
            instance.load()
            APIHolder.initAll()
            val providers = APIHolder.allProviders.filter { it.sourcePlugin == file.absolutePath }
            if (providers.isEmpty()) {
                throw IllegalStateException("Plugin load hua par koi provider nahi mila")
            }
            providersByPlugin[key] = providers
            providers
        }
    }

    /** Download the plugin's .cs3 into filesDir/plugins, reusing the cached copy for the same version. */
    private fun downloadCs3(context: Context, plugin: PluginInfo): File {
        val dir = File(context.filesDir, "plugins").apply { mkdirs() }
        val safeName = (plugin.internalName?.ifBlank { null } ?: plugin.displayName())
            .replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(dir, "$safeName-v${plugin.version}.cs3")
        if (file.exists() && file.length() > 0) return file
        val url = plugin.cs3Url?.ifBlank { null }
            ?: throw IllegalStateException("Is plugin ka .cs3 link nahi hai")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException(".cs3 download fail (HTTP ${resp.code})")
            }
            val body = resp.body
                ?: throw IllegalStateException(".cs3 khaali aaya")
            file.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        return file
    }
}
