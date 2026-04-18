package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.passheep.sticky_note.MainActivity

class StickyNoteWidgetReceiver : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.e(TAG, "onEnabled enqueue refresh")
        WidgetActionWorker.enqueueRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.e(TAG, "onUpdate enqueue refresh ids=${appWidgetIds.size}")
        WidgetActionWorker.enqueueRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_REFRESH -> {
                Log.e(TAG, "onReceive refresh")
                WidgetActionWorker.enqueueRefresh(context)
            }

            ACTION_WIDGET_TOGGLE -> {
                val remoteId = intent.getLongExtra(EXTRA_REMOTE_ID, -1L)
                Log.e(TAG, "onReceive toggle remoteId=$remoteId")
                if (remoteId > 0L) {
                    WidgetActionWorker.enqueueToggle(context, remoteId)
                }
            }

            ACTION_WIDGET_ITEM -> {
                when (intent.getStringExtra(EXTRA_ITEM_ACTION)) {
                    ITEM_ACTION_TOGGLE -> {
                        val remoteId = intent.getLongExtra(EXTRA_REMOTE_ID, -1L)
                        Log.e(TAG, "onReceive item toggle remoteId=$remoteId")
                        if (remoteId > 0L) {
                            WidgetActionWorker.enqueueToggle(context, remoteId)
                        }
                    }

                    ITEM_ACTION_OPEN -> {
                        val localId = intent.getStringExtra(EXTRA_LOCAL_ID).orEmpty()
                        if (localId.isBlank()) return
                        Log.e(TAG, "onReceive item open localId=$localId")
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(MainActivity.EXTRA_OPEN_TODO_ID, localId)
                            },
                        )
                    }
                }
            }

            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_SCREEN_ON,
            -> {
                if (hasWidgetInstances(context) && shouldTriggerForegroundRefresh()) {
                    Log.e(TAG, "onReceive foreground signal action=${intent.action} enqueue refresh")
                    WidgetActionWorker.enqueueRefresh(context)
                }
            }
        }
    }

    private fun hasWidgetInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, StickyNoteWidgetReceiver::class.java),
        )
        return ids.isNotEmpty()
    }

    private fun shouldTriggerForegroundRefresh(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastForegroundSignalRefreshAt < FOREGROUND_SIGNAL_REFRESH_GAP_MILLIS) {
            return false
        }
        lastForegroundSignalRefreshAt = now
        return true
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.passheep.sticky_note.widget.ACTION_WIDGET_REFRESH"
        const val ACTION_WIDGET_TOGGLE = "com.passheep.sticky_note.widget.ACTION_WIDGET_TOGGLE"
        const val ACTION_WIDGET_ITEM = "com.passheep.sticky_note.widget.ACTION_WIDGET_ITEM"
        const val EXTRA_ITEM_ACTION = "extra_item_action"
        const val EXTRA_REMOTE_ID = "extra_remote_id"
        const val EXTRA_LOCAL_ID = "extra_local_id"
        const val ITEM_ACTION_TOGGLE = "toggle"
        const val ITEM_ACTION_OPEN = "open"
        private const val FOREGROUND_SIGNAL_REFRESH_GAP_MILLIS = 20_000L
        @Volatile private var lastForegroundSignalRefreshAt: Long = 0L
        private const val TAG = "StickyNoteWidgetRcvr"
    }
}
