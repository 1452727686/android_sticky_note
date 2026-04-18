package com.passheep.sticky_note.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncAlarmScheduler: SyncAlarmScheduler,
) {
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    fun scheduleRecurringSync(intervalSeconds: Int) {
        scheduleNextTick(intervalSeconds)
        schedulePeriodicFallback(intervalSeconds)
    }

    fun scheduleNextTick(intervalSeconds: Int) {
        val safeInterval = intervalSeconds.coerceIn(60, 600)
        syncAlarmScheduler.schedule(safeInterval)
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(recurringConstraints())
            .setInitialDelay(safeInterval.toLong(), TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            SyncWorkNames.SCHEDULED_SYNC,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun schedulePeriodicFallback(intervalSeconds: Int) {
        val fallbackMinutes = (intervalSeconds / 60).coerceAtLeast(15)
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            fallbackMinutes.toLong(),
            TimeUnit.MINUTES,
        )
            .setConstraints(recurringConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SyncWorkNames.PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun enqueueImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(immediateConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork(
            SyncWorkNames.IMMEDIATE_SYNC,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelRecurringSync() {
        workManager.cancelUniqueWork(SyncWorkNames.SCHEDULED_SYNC)
        workManager.cancelUniqueWork(SyncWorkNames.PERIODIC_SYNC)
        syncAlarmScheduler.cancel()
    }

    private fun recurringConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun immediateConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
