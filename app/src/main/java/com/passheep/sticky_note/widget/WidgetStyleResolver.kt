package com.passheep.sticky_note.widget

import android.content.Context
import android.content.res.Configuration
import com.passheep.sticky_note.core.settings.AppSettings
import com.passheep.sticky_note.core.settings.ThemeMode
import kotlin.math.roundToInt

internal data class WidgetUiStyle(
    val isDark: Boolean,
    val backgroundAlpha: Float,
    val colorfulTextEnabled: Boolean,
    val accentColor: Int,
    val dividerColor: Int,
    val primaryTextColor: Int,
    val secondaryTextColor: Int,
    val pendingColor: Int,
    val completedColor: Int,
)

internal object WidgetStyleResolver {

    fun resolve(context: Context, settings: AppSettings): WidgetUiStyle {
        val isDark = when (settings.widgetThemeMode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> {
                val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }
        val backgroundAlpha = (1f - settings.widgetTransparency.coerceIn(0f, 0.92f)).coerceIn(0.08f, 1f)
        val chromeAlpha = (0.35f + backgroundAlpha * 0.65f).coerceIn(0.35f, 1f)
        return if (isDark) {
            WidgetUiStyle(
                isDark = true,
                backgroundAlpha = backgroundAlpha,
                colorfulTextEnabled = settings.widgetColorfulTextEnabled,
                accentColor = 0xFF7CC6FF.toInt(),
                dividerColor = withAlpha(0xFF2A3344.toInt(), chromeAlpha),
                primaryTextColor = 0xFFF7F9FC.toInt(),
                secondaryTextColor = 0xFF9AA4B2.toInt(),
                pendingColor = 0xFFEAB308.toInt(),
                completedColor = 0xFF34D399.toInt(),
            )
        } else {
            WidgetUiStyle(
                isDark = false,
                backgroundAlpha = backgroundAlpha,
                colorfulTextEnabled = settings.widgetColorfulTextEnabled,
                accentColor = 0xFF1D7CF2.toInt(),
                dividerColor = withAlpha(0xFFE5E7EB.toInt(), chromeAlpha),
                primaryTextColor = 0xFF111827.toInt(),
                secondaryTextColor = 0xFF6B7280.toInt(),
                pendingColor = 0xFFB45309.toInt(),
                completedColor = 0xFF047857.toInt(),
            )
        }
    }

    private fun withAlpha(argb: Int, alpha: Float): Int {
        val clamped = alpha.coerceIn(0f, 1f)
        val alphaInt = (clamped * 255f).roundToInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (alphaInt shl 24)
    }

    fun todoTextColor(style: WidgetUiStyle, completed: Boolean): Int {
        if (!style.colorfulTextEnabled) {
            return style.primaryTextColor
        }
        return if (completed) style.completedColor else style.pendingColor
    }
}
