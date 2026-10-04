package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "turning_logs")
data class TurningLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String = "COMPLETED", // "COMPLETED", "SNOOZED", "MISSED", "MANUAL"
    val orientation: String = "RIGHT", // "LEFT", "RIGHT", "CENTER", "AUTO"
    val notes: String = ""
)
