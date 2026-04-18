package com.passheep.sticky_note.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.passheep.sticky_note.MainActivity

object WidgetActionKeys {
    val TodoId = ActionParameters.Key<String>("todo_id")
    val RemoteTodoId = ActionParameters.Key<String>("remote_todo_id")
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Log.d(TAG, "RefreshWidgetAction start")
        WidgetActionWorker.enqueueRefresh(context)
        Log.d(TAG, "RefreshWidgetAction enqueued")
    }
}

class OpenHomeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Log.d(TAG, "OpenHomeAction")
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
        )
    }
}

class OpenQuickCreateAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        Log.d(TAG, "OpenQuickCreateAction")
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_QUICK_EDITOR, true)
            },
        )
    }
}

class OpenSettingsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
            },
        )
    }
}

class OpenTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val localId = parameters[WidgetActionKeys.TodoId] ?: return
        Log.d(TAG, "OpenTodoAction localId=$localId")
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(MainActivity.EXTRA_OPEN_TODO_ID, localId)
            },
        )
    }
}

class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val remoteId = parameters[WidgetActionKeys.RemoteTodoId]
            ?.toLongOrNull()
            ?: return
        Log.d(TAG, "ToggleTodoAction start remoteId=$remoteId")
        WidgetActionWorker.enqueueToggle(context, remoteId)
        Log.d(TAG, "ToggleTodoAction enqueued remoteId=$remoteId")
    }
}

private const val TAG = "StickyNoteWidget"
