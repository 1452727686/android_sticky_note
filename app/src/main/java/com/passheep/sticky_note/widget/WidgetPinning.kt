package com.passheep.sticky_note.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

fun requestPinStickyNoteWidget(context: Context): Boolean {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (!appWidgetManager.isRequestPinAppWidgetSupported) {
        return false
    }
    return appWidgetManager.requestPinAppWidget(
        ComponentName(context, StickyNoteWidgetReceiver::class.java),
        null,
        null,
    )
}
