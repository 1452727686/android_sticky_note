package com.passheep.sticky_note.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.cloud.CloudTodoStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SyncAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SYNC_ALARM) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SyncAlarmEntryPoint::class.java,
                )
                val settingsRepository = entryPoint.settingsRepository()
                val syncScheduler = entryPoint.syncScheduler()
                val settings = settingsRepository.settings.first()
                if (!settings.platformEnabled || settings.apiKey.isBlank()) {
                    Log.e(TAG, "skip alarm sync because platform disabled or apiKey empty")
                    syncScheduler.cancelRecurringSync()
                    return@runCatching
                }
                syncScheduler.scheduleNextTick(settings.syncIntervalSeconds)
                Log.e(TAG, "alarm tick refresh start interval=${settings.syncIntervalSeconds}")
                entryPoint.cloudTodoStore().refreshTodos(
                    reason = "alarm",
                    clearOnFailure = false,
                )
                Log.e(TAG, "alarm tick refresh success")
            }.onFailure { error ->
                Log.e(TAG, "alarm tick failed", error)
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_SYNC_ALARM = "com.passheep.sticky_note.sync.ACTION_SYNC_ALARM"
        private const val TAG = "StickyNoteSyncAlarm"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncAlarmEntryPoint {
    fun syncScheduler(): SyncScheduler
    fun settingsRepository(): SettingsRepository
    fun cloudTodoStore(): CloudTodoStore
}
