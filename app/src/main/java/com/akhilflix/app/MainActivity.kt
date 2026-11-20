package com.akhilflix.app

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.io.File

/**
 * HOST SHELL. The whole AkhilFlix app is compiled into this APK
 * (monolithic build — no DEX download at runtime, so it opens reliably
 * on every phone). Content, theme, sections and buttons still come from
 * the backend JSON (akhilflix.json), so the app updates without an APK
 * update for content. The plugin DEX is still built by CI for the repo,
 * but the installed app does not depend on it.
 *
 * Crash reporter: if the previous launch crashed, AkhilFlixApp saved the
 * stack trace to filesDir/crash-report.txt. Show it here with plain
 * Android views (no Compose, so it renders even if Compose is what
 * crashed) so the user can screenshot it. The button clears the report
 * and opens the app normally.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashFile = File(filesDir, "crash-report.txt")
        if (crashFile.exists()) {
            val report = try { crashFile.readText() } catch (e: Exception) { "Could not read report: $e" }
            val scroll = ScrollView(this)
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }
            val title = TextView(this).apply {
                text = "App last time crash hua tha. Ye report screenshot karke bhejo:"
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
            }
            val body = TextView(this).apply {
                text = report
                textSize = 12f
                setTextIsSelectable(true)
                setPadding(0, 24, 0, 24)
            }
            val clear = Button(this).apply {
                text = "Report clear karo aur app kholo"
                setOnClickListener {
                    crashFile.delete()
                    recreate()
                }
            }
            layout.addView(title)
            layout.addView(body)
            layout.addView(clear)
            scroll.addView(layout)
            setContentView(scroll)
            return
        }
        setContent {
            AkhilFlixPluginImpl().Content()
        }
    }
}
