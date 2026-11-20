package com.akhilflix.app

import androidx.compose.runtime.Composable
import com.akhilflix.api.AkhilFlixPlugin

/**
 * Entry point of the plugin DEX. The host app loads this class with
 * DexClassLoader and calls Content() — the whole AkhilFlix UI.
 */
class AkhilFlixPluginImpl : AkhilFlixPlugin {
    override fun appName(): String = "AkhilFlix"

    @Composable
    override fun Content() {
        AkhilFlixRoot()
    }
}
