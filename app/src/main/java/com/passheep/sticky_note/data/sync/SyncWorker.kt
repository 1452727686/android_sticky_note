package com.passheep.sticky_note.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.passheep.sticky_note.core.settings.SettingsRepository
import com.passheep.sticky_note.data.cloud.CloudTodoStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java,
        )
        val cloudTodoStore = entryPoint.cloudTodoStore()
        val syncScheduler = entryPoint.syncScheduler()
        val settingsRepository = entryPoint.settingsRepository()
        val settings = settingsRepository.settings.first()

        if (!settings.platformEnabled || settings.apiKey.isBlank()) {
            Log.d(TAG, "skip worker sync because platform is disabled or api key is empty")
            return Result.success()
        }

        return try {
            cloudTodoStore.refreshTodos(reason = "worker", clearOnFailure = false)
            Result.success()
        } catch (error: Throwable) {
            Log.e(TAG, "worker sync failed", error)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "StickyNoteWorker"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun cloudTodoStore(): CloudTodoStore
    fun syncScheduler(): SyncScheduler
    fun settingsRepository(): SettingsRepository
}
