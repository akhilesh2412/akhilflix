package com.akhilflix.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Parse a "#RRGGBB" / "#AARRGGBB" hex string from the backend config. */
fun colorFromHex(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val argb = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return fallback
        }
        Color(argb.toLong(16))
    } catch (e: Exception) {
        fallback
    }
}

val NetflixRed = Color(0xFFE50914)
val AppBackground = Color(0xFF141414)
val AppSurface = Color(0xFF1F1F1F)
val AppText = Color(0xFFFFFFFF)
val AppMuted = Color(0xFFB3B3B3)
