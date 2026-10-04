package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.HatchDatabase
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val database = HatchDatabase.getInstance(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val activeBatch = database.batchDao().getActiveBatch() ?: return@launch
                if (!activeBatch.isActive || activeBatch.isInLockdown()) return@launch

                val schedule = database.turningDao().getSchedule(activeBatch.id) ?: return@launch
                if (!schedule.enabled) return@launch

                val now = System.currentTimeMillis()
                val triggerAt = if (schedule.nextTurnTimestamp > now) {
                    schedule.nextTurnTimestamp
                } else {
                    now + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
                }

                database.turningDao().insertOrUpdateSchedule(
                    schedule.copy(nextTurnTimestamp = triggerAt)
                )
                alarmScheduler.scheduleNextTurn(activeBatch.id, triggerAt)
            }
        }
    }
}
