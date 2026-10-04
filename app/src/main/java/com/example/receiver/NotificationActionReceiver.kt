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

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val batchId = intent.getLongExtra(NotificationHelper.EXTRA_BATCH_ID, -1L)
        val notificationHelper = NotificationHelper(context)
        val alarmScheduler = AlarmScheduler(context)
        val database = HatchDatabase.getInstance(context)

        // Dismiss notification upon action
        notificationHelper.cancelTurningNotification()

        CoroutineScope(Dispatchers.IO).launch {
            val targetBatchId = if (batchId > 0) batchId else database.batchDao().getActiveBatch()?.id ?: return@launch
            val schedule = database.turningDao().getSchedule(targetBatchId)

            when (action) {
                NotificationHelper.ACTION_MARK_TURNED -> {
                    val lastLog = database.turningDao().getLogsForBatch(targetBatchId).firstOrNull()
                    val newOrientation = if (lastLog?.orientation == "LEFT") "RIGHT" else "LEFT"

                    database.turningDao().insertLog(
                        TurningLog(
                            batchId = targetBatchId,
                            action = "COMPLETED",
                            orientation = newOrientation,
                            notes = "Confirmed via notification action"
                        )
                    )

                    if (schedule != null) {
                        val intervalHours = schedule.intervalHours.coerceAtLeast(1)
                        val nextTurn = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(intervalHours.toLong())
                        database.turningDao().insertOrUpdateSchedule(
                            schedule.copy(
                                lastTurnTimestamp = System.currentTimeMillis(),
                                nextTurnTimestamp = nextTurn,
                                totalTurnsCompleted = schedule.totalTurnsCompleted + 1
                            )
                        )
                        alarmScheduler.scheduleNextTurn(targetBatchId, nextTurn)
                    }
                }

                NotificationHelper.ACTION_SNOOZE -> {
                    database.turningDao().insertLog(
                        TurningLog(
                            batchId = targetBatchId,
                            action = "SNOOZED",
                            orientation = "PENDING",
                            notes = "Snoozed for 15 minutes"
                        )
                    )

                    val snoozeUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(15)
                    if (schedule != null) {
                        database.turningDao().insertOrUpdateSchedule(
                            schedule.copy(nextTurnTimestamp = snoozeUntil)
                        )
                    }
                    alarmScheduler.scheduleNextTurn(targetBatchId, snoozeUntil)
                }
            }
        }
    }
}
