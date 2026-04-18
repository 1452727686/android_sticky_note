package com.passheep.sticky_note.data.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager? by lazy {
        context.getSystemService(AlarmManager::class.java)
    }

    fun schedule(intervalSeconds: Int) {
        val safeInterval = intervalSeconds.coerceIn(60, 600)
        val triggerAtMillis = System.currentTimeMillis() + safeInterval * 1000L
        val pendingIntent = alarmPendingIntent()
        alarmManager?.cancel(pendingIntent)
        val canScheduleExact = canScheduleExactAlarms()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExact) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.e(TAG, "schedule exact interval=${safeInterval}s triggerAt=$triggerAtMillis")
            } else {
                scheduleInexact(triggerAtMillis, pendingIntent)
                Log.e(TAG, "schedule inexact interval=${safeInterval}s triggerAt=$triggerAtMillis")
            }
        } catch (security: SecurityException) {
            Log.e(TAG, "setExact denied, fallback to inexact alarm", security)
            scheduleInexact(triggerAtMillis, pendingIntent)
            Log.e(TAG, "schedule fallback inexact interval=${safeInterval}s triggerAt=$triggerAtMillis")
        }
    }

    fun cancel() {
        alarmManager?.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent {
        val intent = Intent(context, SyncAlarmReceiver::class.java).apply {
            action = SyncAlarmReceiver.ACTION_SYNC_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun scheduleInexact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private companion object {
        const val REQUEST_CODE = 62031
        const val TAG = "StickyNoteSyncAlarm"
    }
}
