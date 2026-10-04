package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val alertType: String, // "TEMP_HIGH", "TEMP_LOW", "HUMIDITY_HIGH", "HUMIDITY_LOW", "SENSOR_DISCONNECTED", "LOCKDOWN", "HATCH_DAY"
    val title: String,
    val message: String,
    val recordedValue: Double? = null,
    val thresholdValue: Double? = null,
    val isAcknowledged: Boolean = false
)
