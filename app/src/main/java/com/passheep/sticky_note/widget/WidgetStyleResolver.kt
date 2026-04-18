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
            if (!completed) {
                return style.primaryTextColor
            }
            val ratio = if (style.isDark) 0.58f else 0.42f
            return blendArgb(style.primaryTextColor, style.secondaryTextColor, ratio)
        }
        return if (completed) style.completedColor else style.pendingColor
    }

    private fun blendArgb(colorA: Int, colorB: Int, ratioB: Float): Int {
        val t = ratioB.coerceIn(0f, 1f)
        val aA = (colorA ushr 24) and 0xFF
        val rA = (colorA ushr 16) and 0xFF
        val gA = (colorA ushr 8) and 0xFF
        val bA = colorA and 0xFF

        val aB = (colorB ushr 24) and 0xFF
        val rB = (colorB ushr 16) and 0xFF
        val gB = (colorB ushr 8) and 0xFF
        val bB = colorB and 0xFF

        val a = (aA + ((aB - aA) * t)).roundToInt().coerceIn(0, 255)
        val r = (rA + ((rB - rA) * t)).roundToInt().coerceIn(0, 255)
        val g = (gA + ((gB - gA) * t)).roundToInt().coerceIn(0, 255)
        val b = (bA + ((bB - bA) * t)).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
