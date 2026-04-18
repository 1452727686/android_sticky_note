package com.passheep.sticky_note.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.passheep.sticky_note.MainActivity
import com.passheep.sticky_note.R
import dagger.hilt.android.EntryPointAccessors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

object StickyNoteRemoteWidgetRenderer {

    suspend fun refreshAllFromCloud(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, StickyNoteWidgetReceiver::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isEmpty()) {
            Log.e(TAG, "refreshAllFromCloud skipped no_widget")
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val settings = entryPoint.settingsRepository().settings.first()
        val style = WidgetStyleResolver.resolve(context, settings)
        val hasCloudConfig = settings.platformEnabled && settings.apiKey.isNotBlank()
        val lastSyncLabel = settings.lastSyncAtMillis.toWidgetLastSyncLabel()

        Log.e(
            TAG,
            "refreshAllFromCloud render widgets=${widgetIds.size} dark=${style.isDark} alpha=${settings.widgetTransparency} colorful=${settings.widgetColorfulTextEnabled}",
        )

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_todo_list)

        widgetIds.forEach { widgetId ->
            val views = buildWidgetViews(
                context = context,
                widgetId = widgetId,
                hasCloudConfig = hasCloudConfig,
                lastSyncLabel = lastSyncLabel,
                style = style,
            )
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildWidgetViews(
        context: Context,
        widgetId: Int,
        hasCloudConfig: Boolean,
        lastSyncLabel: String,
        style: WidgetUiStyle,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.sticky_note_widget_layout)
        views.setTextViewText(R.id.widget_add_button, "+")
        views.setTextViewText(R.id.widget_refresh_button, "\u21BB")
        views.setTextViewText(R.id.widget_last_sync_text, "\u4e0a\u6b21\u66f4\u65b0 $lastSyncLabel")
        views.setTextViewText(
            R.id.widget_empty_text,
            if (hasCloudConfig) "\u6682\u65e0\u5f85\u529e" else "\u8bf7\u5148\u914d\u7f6e\u5e73\u53f0",
        )

        views.setInt(
            R.id.widget_bg_layer,
            "setBackgroundResource",
            if (style.isDark) R.drawable.sticky_note_widget_bg_dark else R.drawable.sticky_note_widget_bg_light,
        )
        views.setFloat(R.id.widget_bg_layer, "setAlpha", style.backgroundAlpha)
        views.setInt(R.id.widget_divider, "setBackgroundColor", style.dividerColor)
        views.setTextColor(R.id.widget_add_button, style.accentColor)
        views.setTextColor(R.id.widget_refresh_button, style.secondaryTextColor)
        views.setTextColor(R.id.widget_last_sync_text, style.secondaryTextColor)
        views.setTextColor(R.id.widget_empty_text, style.primaryTextColor)

        views.setOnClickPendingIntent(
            R.id.widget_add_button,
            quickCreatePendingIntent(context = context, widgetId = widgetId),
        )
        views.setOnClickPendingIntent(
            R.id.widget_refresh_button,
            refreshPendingIntent(context = context, widgetId = widgetId),
        )
        views.setOnClickPendingIntent(
            R.id.widget_last_sync_text,
            openHomePendingIntent(context = context, widgetId = widgetId),
        )

        val listIntent = Intent(context, StickyNoteWidgetListService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/list/$widgetId")
        }
        views.setRemoteAdapter(R.id.widget_todo_list, listIntent)
        views.setEmptyView(R.id.widget_todo_list, R.id.widget_empty_text)
        views.setPendingIntentTemplate(
            R.id.widget_todo_list,
            itemTemplatePendingIntent(context = context, widgetId = widgetId),
        )
        return views
    }

    private fun refreshPendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, StickyNoteWidgetReceiver::class.java).apply {
            action = StickyNoteWidgetReceiver.ACTION_WIDGET_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/refresh/$widgetId")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("refresh", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun itemTemplatePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, StickyNoteWidgetReceiver::class.java).apply {
            action = StickyNoteWidgetReceiver.ACTION_WIDGET_ITEM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("stickynote://widget/item_template/$widgetId")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode("item_template", widgetId),
            intent,
            pendingIntentTemplateFlags(),
        )
    }

    private fun openHomePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data = Uri.parse("stickynote://widget/open/home/$widgetId")
        }
        return PendingIntent.getActivity(
            context,
            requestCode("open_home", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun quickCreatePendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN_QUICK_EDITOR, true)
            data = Uri.parse("stickynote://widget/open/quick/$widgetId")
        }
        return PendingIntent.getActivity(
            context,
            requestCode("open_quick", widgetId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(key: String, widgetId: Int, extra: Any? = null): Int {
        var result = 17
        result = 31 * result + key.hashCode()
        result = 31 * result + widgetId
        result = 31 * result + (extra?.hashCode() ?: 0)
        return result
    }

    private fun pendingIntentTemplateFlags(): Int {
        val baseFlags = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            baseFlags or PendingIntent.FLAG_MUTABLE
        } else {
            baseFlags
        }
    }

    private fun Long?.toWidgetLastSyncLabel(): String {
        if (this == null) return "\u6682\u672a\u540c\u6b65"
        return Instant.ofEpochMilli(this)
            .atZone(BEIJING_ZONE_ID)
            .toLocalDateTime()
            .format(LAST_SYNC_FORMATTER)
    }

    private val BEIJING_ZONE_ID: ZoneId = ZoneId.of("Asia/Shanghai")
    private val LAST_SYNC_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val TAG = "StickyNoteWidgetRndr"
}
