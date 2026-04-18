package com.passheep.sticky_note

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.passheep.sticky_note.core.settings.ThemeMode
import com.passheep.sticky_note.ui.theme.AccentBlue
import com.passheep.sticky_note.ui.theme.AccentBlueLight
import com.passheep.sticky_note.ui.theme.AccentTeal
import com.passheep.sticky_note.ui.theme.AccentTealLight
import com.passheep.sticky_note.ui.theme.DarkCharcoal
import com.passheep.sticky_note.ui.theme.DeepBlack
import com.passheep.sticky_note.ui.theme.GlassSurfaceDark
import com.passheep.sticky_note.ui.theme.GlassSurfaceLight

private val DarkScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentTeal,
    background = DeepBlack,
    surface = GlassSurfaceDark,
    surfaceContainer = DarkCharcoal,
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF131B2A),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF163A69),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF123D48),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF06242A),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE7F1FF),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFD5F7FF),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF5F8FF),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF5F8FF),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC2CBDB),
    outline = androidx.compose.ui.graphics.Color(0xFF526178),
)

private val LightScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentTeal,
    background = GlassSurfaceLight,
    surface = GlassSurfaceLight,
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEAF2FF),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = AccentBlueLight,
    secondaryContainer = AccentTealLight,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF0A264E),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0B3F46),
    onBackground = androidx.compose.ui.graphics.Color(0xFF0D1320),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0D1320),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF5C687D),
    outline = androidx.compose.ui.graphics.Color(0xFFC9D4E5),
)

@Composable
fun StickyNoteTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = themeMode.resolveColorScheme(),
        content = content,
    )
}

@Composable
private fun ThemeMode.resolveColorScheme(): ColorScheme {
    val useDark = when (this) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    return if (useDark) DarkScheme else LightScheme
}
