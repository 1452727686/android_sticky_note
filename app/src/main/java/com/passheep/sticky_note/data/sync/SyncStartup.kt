package com.passheep.sticky_note.data.sync

import com.passheep.sticky_note.core.dispatchers.ApplicationScope
import com.passheep.sticky_note.core.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class SyncStartup @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun start() {
        applicationScope.launch {
            settingsRepository.settings
                .map { it.platformEnabled to it.syncIntervalSeconds }
                .distinctUntilChanged()
                .collect { (enabled, interval) ->
                    if (enabled) {
                        syncScheduler.scheduleRecurringSync(interval)
                    } else {
                        syncScheduler.cancelRecurringSync()
                    }
                }
        }
        applicationScope.launch {
            if (settingsRepository.settings.map { it.platformEnabled }.first()) {
                syncScheduler.enqueueImmediateSync()
            }
        }
    }
}
