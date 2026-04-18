package com.passheep.sticky_note.widget

import android.content.Context
import android.util.Log
import com.passheep.sticky_note.core.dispatchers.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    suspend fun refreshAll() {
        Log.e(TAG, "refreshAll start")
        StickyNoteRemoteWidgetRenderer.refreshAllFromCloud(context)
        Log.e(TAG, "refreshAll done")
    }

    fun refreshAllAsync() {
        applicationScope.launch {
            runCatching { refreshAll() }
                .onFailure { Log.e(TAG, "refreshAllAsync failed", it) }
        }
    }
}

private const val TAG = "StickyNoteWidgetUpdater"
