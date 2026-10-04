package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AlertLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<AlertLog>>

    @Query("SELECT * FROM alert_logs WHERE batchId = :batchId ORDER BY timestamp DESC")
    suspend fun getAlertsForBatch(batchId: Long): List<AlertLog>

    @Query("SELECT COUNT(*) FROM alert_logs WHERE isAcknowledged = 0")
    fun getUnacknowledgedCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertLog): Long

    @Query("UPDATE alert_logs SET isAcknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeAlert(id: Long)

    @Query("UPDATE alert_logs SET isAcknowledged = 1")
    suspend fun acknowledgeAllAlerts()

    @Query("DELETE FROM alert_logs")
    suspend fun clearAllAlerts()
}
