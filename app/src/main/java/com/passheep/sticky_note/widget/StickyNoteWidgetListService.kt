package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.passheep.sticky_note.R
import com.passheep.sticky_note.core.model.Todo
import dagger.hilt.android.EntryPointAccessors
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class StickyNoteWidgetListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return Factory(applicationContext, widgetId)
    }
}

private class Factory(
    private val context: android.content.Context,
    private val widgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private val todos = mutableListOf<WidgetRenderTodo>()
    private var style: WidgetUiStyle = WidgetUiStyle(
        isDark = false,
        backgroundAlpha = 1f,
        colorfulTextEnabled = true,
        accentColor = 0xFF1D7CF2.toInt(),
        dividerColor = 0xFFE5E7EB.toInt(),
        primaryTextColor = 0xFF111827.toInt(),
        secondaryTextColor = 0xFF6B7280.toInt(),
        pendingColor = 0xFFB45309.toInt(),
        completedColor = 0xFF047857.toInt(),
    )

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        runCatching {
            runBlocking {
                val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
                val settings = entryPoint.settingsRepository().settings.first()
                style = WidgetStyleResolver.resolve(context, settings)
                val cloudReady = settings.platformEnabled && settings.apiKey.isNotBlank()
                val latest = if (cloudReady) {
                    entryPoint.cloudTodoStore().snapshotTodos(limit = 80).map { it.toRenderTodo() }
                } else {
                    emptyList()
                }
                todos.clear()
                todos.addAll(latest)
                val preview = latest.take(5).joinToString(",") { "${it.remoteId}:${it.completed}" }
                Log.e(
                    TAG,
                    "onDataSetChanged widgetId=$widgetId count=${latest.size} preview=[$preview]",
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "onDataSetChanged failed widgetId=$widgetId", error)
            todos.clear()
        }
    }

    override fun onDestroy() {
        todos.clear()
    }

    override fun getCount(): Int = todos.size

    override fun getViewAt(position: Int): RemoteViews? {
        val todo = todos.getOrNull(position) ?: return null
        val views = RemoteViews(context.packageName, R.layout.sticky_note_widget_item)
        val textColor = WidgetStyleResolver.todoTextColor(style, todo.completed)
        val rowOpenIntent = Intent().apply {
            putExtra(StickyNoteWidgetReceiver.EXTRA_ITEM_ACTION, StickyNoteWidgetReceiver.ITEM_ACTION_OPEN)
            putExtra(StickyNoteWidgetReceiver.EXTRA_LOCAL_ID, todo.localId)
        }

        views.setTextViewText(R.id.widget_item_radio, if (todo.completed) "\u2713" else "\u25CB")
        views.setTextColor(R.id.widget_item_radio, textColor)
        views.setTextViewText(R.id.widget_item_title, todo.title.withOptionalStrike(todo.completed))
        views.setTextColor(R.id.widget_item_title, textColor)
        views.setOnClickFillInIntent(R.id.widget_item_root, rowOpenIntent)
        views.setOnClickFillInIntent(R.id.widget_item_title, rowOpenIntent)

        if (todo.remoteId != null) {
            views.setOnClickFillInIntent(
                R.id.widget_item_radio,
                Intent().apply {
                    putExtra(StickyNoteWidgetReceiver.EXTRA_ITEM_ACTION, StickyNoteWidgetReceiver.ITEM_ACTION_TOGGLE)
                    putExtra(StickyNoteWidgetReceiver.EXTRA_REMOTE_ID, todo.remoteId)
                },
            )
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val todo = todos.getOrNull(position) ?: return position.toLong()
        val base = (todo.remoteId ?: todo.localId.hashCode().toLong()) and Long.MAX_VALUE
        val completedBit = if (todo.completed) 1L else 0L
        return (base shl 1) xor completedBit
    }

    override fun hasStableIds(): Boolean = true

    private fun Todo.toRenderTodo(): WidgetRenderTodo = WidgetRenderTodo(
        localId = localId,
        remoteId = remoteId,
        title = title,
        completed = completed,
    )

    private data class WidgetRenderTodo(
        val localId: String,
        val remoteId: Long?,
        val title: String,
        val completed: Boolean,
    )

    private fun String.withOptionalStrike(completed: Boolean): CharSequence {
        if (!completed || isBlank()) return this
        return SpannableString(this).apply {
            setSpan(StrikethroughSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private companion object {
        const val TAG = "StickyNoteWidgetList"
    }
}
