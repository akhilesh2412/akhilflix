package com.akhilflix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.akhilflix.app.data.model.ThemeConfig

/**
 * App theme. Colors come from the backend config (akhilflix.json -> theme),
 * so the whole look can change without an app update. Falls back to the
 * AkhilFlix dark theme when the config is missing values.
 */
@Composable
fun AkhilFlixTheme(
    theme: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = colorFromHex(theme.primary, NetflixRed),
        background = colorFromHex(theme.background, AppBackground),
        surface = colorFromHex(theme.surface, AppSurface),
        onPrimary = AppText,
        onBackground = colorFromHex(theme.text, AppText),
        onSurface = colorFromHex(theme.text, AppText),
        onSurfaceVariant = colorFromHex(theme.muted, AppMuted)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
