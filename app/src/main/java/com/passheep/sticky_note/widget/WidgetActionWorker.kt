package com.passheep.sticky_note.widget

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.passheep.sticky_note.data.cloud.CloudTodoStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class WidgetActionWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val action = inputData.getString(KEY_ACTION).orEmpty()
        Log.e(TAG, "WidgetActionWorker start action=$action remoteId=${inputData.getLong(KEY_REMOTE_ID, -1L)}")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WidgetActionWorkerEntryPoint::class.java,
        )
        val cloudTodoStore = entryPoint.cloudTodoStore()
        val widgetUpdater = entryPoint.widgetUpdater()

        return try {
            when (action) {
                ACTION_REFRESH -> {
                    Log.e(TAG, "doWork refresh start")
                    val refreshed = cloudTodoStore.refreshTodos(
                        reason = "widget_action_refresh",
                        clearOnFailure = false,
                    )
                    Log.e(TAG, "doWork refresh success count=${refreshed.size}")
                }

                ACTION_TOGGLE -> {
                    val remoteId = inputData.getLong(KEY_REMOTE_ID, -1L)
                    if (remoteId <= 0L) {
                        Log.w(TAG, "doWork toggle skipped because remoteId is invalid")
                        return Result.failure()
                    }
                    Log.e(TAG, "doWork toggle start remoteId=$remoteId")
                    cloudTodoStore.toggleCompleted(remoteId)
                    Log.e(TAG, "doWork toggle api toggled remoteId=$remoteId")
                }

                else -> {
                    Log.w(TAG, "doWork skipped unknown action=$action")
                    return Result.success()
                }
            }
            widgetUpdater.refreshAll()
            Log.e(TAG, "doWork forced widget refresh action=$action")
            Log.e(TAG, "doWork success action=$action")
            Result.success()
        } catch (error: Throwable) {
            Log.e(TAG, "doWork failed action=$action", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "StickyNoteWidgetWork"
        private const val KEY_ACTION = "widget_action"
        private const val KEY_REMOTE_ID = "widget_remote_id"
        private const val ACTION_REFRESH = "refresh"
        private const val ACTION_TOGGLE = "toggle"
        private const val UNIQUE_REFRESH_WORK = "widget_action_refresh"

        fun enqueueRefresh(context: Context) {
            val request = newRequest(
                action = ACTION_REFRESH,
                remoteId = null,
            )
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_REFRESH_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun enqueueToggle(context: Context, remoteId: Long) {
            val request = newRequest(
                action = ACTION_TOGGLE,
                remoteId = remoteId,
            )
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget_action_toggle_$remoteId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        private fun newRequest(action: String, remoteId: Long?) =
            OneTimeWorkRequestBuilder<WidgetActionWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, action)
                        .apply {
                            if (remoteId != null) {
                                putLong(KEY_REMOTE_ID, remoteId)
                            }
                        }
                        .build(),
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetActionWorkerEntryPoint {
    fun cloudTodoStore(): CloudTodoStore
    fun widgetUpdater(): WidgetUpdater
}
