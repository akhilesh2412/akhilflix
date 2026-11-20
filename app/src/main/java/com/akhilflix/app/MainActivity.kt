package com.akhilflix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * HOST SHELL. The whole AkhilFlix app is compiled into this APK
 * (monolithic build — no DEX download at runtime, so it opens reliably
 * on every phone). Content, theme, sections and buttons still come from
 * the backend JSON (akhilflix.json), so the app updates without an APK
 * update for content. The plugin DEX is still built by CI for the repo,
 * but the installed app does not depend on it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AkhilFlixPluginImpl().Content()
        }
    }
}
