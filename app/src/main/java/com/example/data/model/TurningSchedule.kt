package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turning_schedules")
data class TurningSchedule(
    @PrimaryKey
    val batchId: Long,
    val enabled: Boolean = true,
    val intervalHours: Int = 2,
    val is24Hours: Boolean = true,
    val startHour: Int = 7,  // 07:00
    val endHour: Int = 23,   // 23:00
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val nextTurnTimestamp: Long = 0L,
    val lastTurnTimestamp: Long = 0L,
    val totalTurnsCompleted: Int = 0
) {
    val dailyTurnsTarget: Int
        get() = if (is24Hours) {
            24 / intervalHours
        } else {
            val activeHours = (endHour - startHour).coerceAtLeast(1)
            activeHours / intervalHours
        }
}
