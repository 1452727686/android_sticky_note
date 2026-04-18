package com.passheep.sticky_note.widget

import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.cloud.CloudTodoStore
import com.passheep.sticky_note.data.sync.SyncScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun cloudTodoStore(): CloudTodoStore
    fun settingsRepository(): SettingsRepository
    fun syncScheduler(): SyncScheduler
    fun widgetUpdater(): WidgetUpdater
}
