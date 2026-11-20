package com.akhilflix.api

import androidx.compose.runtime.Composable

/**
 * The contract between the AkhilFlix host app and the plugin DEX that
 * comes from the GitHub repo. The host knows ONLY this interface; every
 * screen, player, network call and feature lives in the plugin.
 */
interface AkhilFlixPlugin {
    fun appName(): String

    /** The plugin's whole UI — the host calls this inside its own setContent. */
    @Composable
    fun Content()
}
