package com.akhilflix.app

/**
 * The repo manifest (repo.json) hosted on GitHub. The host reads this to
 * find the plugin DEX — exactly like a CloudStream repo.json.
 *
 * Example:
 * {
 *   "name": "AkhilFlix",
 *   "version": 3,
 *   "pluginClass": "com.akhilflix.app.AkhilFlixPluginImpl",
 *   "pluginUrl": "https://raw.githubusercontent.com/Akhilesh2412/akhilflix/builds/akhilflix-plugin.dex",
 *   "appName": "AkhilFlix"
 * }
 */
data class PluginRepo(
    val name: String,
    val version: Int,
    val pluginClass: String,
    val pluginUrl: String,
    val appName: String
)
