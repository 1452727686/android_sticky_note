package com.passheep.sticky_note.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.widget.WidgetActionWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SyncStartupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SyncStartupReceiverEntryPoint::class.java,
                )
                val settings = entryPoint.settingsRepository().settings.first()
                if (!settings.platformEnabled || settings.apiKey.isBlank()) {
                    entryPoint.syncScheduler().cancelRecurringSync()
                    Log.e(TAG, "skip bootstrap action=$action enabled=${settings.platformEnabled} apiKeyBlank=${settings.apiKey.isBlank()}")
                    return@runCatching
                }
                entryPoint.syncScheduler().scheduleRecurringSync(settings.syncIntervalSeconds)
                entryPoint.syncScheduler().enqueueImmediateSync()
                WidgetActionWorker.enqueueRefresh(context.applicationContext)
                Log.e(TAG, "bootstrap synced action=$action interval=${settings.syncIntervalSeconds}s")
            }.onFailure { error ->
                Log.e(TAG, "bootstrap failed action=$action", error)
            }
            pendingResult.finish()
        }
    }

    private companion object {
        const val TAG = "StickyNoteSyncBoot"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncStartupReceiverEntryPoint {
    fun settingsRepository(): SettingsRepository
    fun syncScheduler(): SyncScheduler
}
