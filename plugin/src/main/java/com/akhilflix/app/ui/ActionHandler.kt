package com.akhilflix.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.akhilflix.app.data.model.AppAction
import com.akhilflix.app.data.model.Course

/**
 * Executes the actions a backend JSON can ask for.
 *
 * The JSON can only pick actions that exist in this file. That is the one
 * hard limit of server-driven UI: content, layout, theme and WHICH action
 * fires are all backend-controlled, but a brand-new action type (download,
 * picture-in-picture, casting, ...) needs its code added here first,
 * i.e. an app update. Everything listed below is fully JSON-driven.
 */
object ActionHandler {

    /** What tapping a course card does. `action` comes from SectionConfig.clickAction. */
    fun courseClick(course: Course, action: String?, navController: NavController) {
        // Null-safe: a null/unknown action opens the detail screen.
        when (action?.lowercase()) {
            // Play the course's first video straight away (no detail screen).
            "play" -> course.videos.firstOrNull()?.let {
                navController.navigate("player/${it.id}")
            }
            // Default + any unknown value: open the course detail screen.
            else -> navController.navigate("detail/${course.id}")
        }
    }

    /** A standalone action, e.g. a Home button from AppConfig.homeButtons. */
    fun perform(action: AppAction, navController: NavController, context: Context) {
        when (action.type.lowercase()) {
            // target = YouTube video id
            "play" -> navController.navigate("player/${action.target}")
            // target = course id
            "detail" -> navController.navigate("detail/${action.target}")
            // target = full https:// URL, opened in the browser
            "open_url" -> openUrl(action.target, context)
            // target = YouTube video id, channel handle (@name) or full URL
            "open_youtube" -> openUrl(youtubeUrlFor(action.target), context)
            // target = text to share
            "share" -> shareText(action.target, context)
        }
    }

    private fun youtubeUrlFor(target: String): String {
        val t = target.trim()
        return when {
            t.startsWith("http") -> t
            t.startsWith("@") -> "https://www.youtube.com/$t"
            else -> "https://www.youtube.com/watch?v=$t"
        }
    }

    private fun openUrl(url: String, context: Context) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun shareText(text: String, context: Context) {
        runCatching {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
