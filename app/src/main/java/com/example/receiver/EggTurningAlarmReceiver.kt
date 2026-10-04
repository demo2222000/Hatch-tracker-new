package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.HatchDatabase
import com.example.data.model.TurningLog
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class EggTurningAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val batchId = intent?.getLongExtra(AlarmScheduler.EXTRA_BATCH_ID, -1L) ?: -1L
        val notificationHelper = NotificationHelper(context)
        val alarmScheduler = AlarmScheduler(context)
        val database = HatchDatabase.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            val batch = if (batchId > 0) database.batchDao().getBatchById(batchId) else database.batchDao().getActiveBatch()

            if (batch == null || !batch.isActive) {
                return@launch
            }

            // Check if batch has entered lockdown (no more turning!)
            if (batch.isInLockdown()) {
                notificationHelper.showMilestoneNotification(
                    title = "Lockdown Day Reached: ${batch.name}",
                    message = "Turning is now completed! Do not open the incubator. Maintain lockdown humidity (${batch.lockdownHumidityPct.toInt()}%)."
                )
                return@launch
            }

            val schedule = database.turningDao().getSchedule(batch.id)
            if (schedule != null && schedule.enabled) {
                // Determine orientation to turn to (alternate Left / Right)
                val lastLog = database.turningDao().getLogsForBatch(batch.id).firstOrNull()
                val nextOrientation = if (lastLog?.orientation == "LEFT") "RIGHT" else "LEFT"

                // Show loud notification
                notificationHelper.showEggTurningNotification(
                    batchId = batch.id,
                    batchName = batch.name,
                    targetOrientation = nextOrientation
                )

                // Schedule subsequent turn
                val nextTimestamp = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(schedule.intervalHours.toLong())
                database.turningDao().insertOrUpdateSchedule(
                    schedule.copy(nextTurnTimestamp = nextTimestamp)
                )
                alarmScheduler.scheduleNextTurn(batch.id, nextTimestamp)
            }
        }
    }
}
