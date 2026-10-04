package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TurningLog
import com.example.data.model.TurningSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface TurningDao {
    @Query("SELECT * FROM turning_schedules WHERE batchId = :batchId LIMIT 1")
    fun getScheduleFlow(batchId: Long): Flow<TurningSchedule?>

    @Query("SELECT * FROM turning_schedules WHERE batchId = :batchId LIMIT 1")
    suspend fun getSchedule(batchId: Long): TurningSchedule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSchedule(schedule: TurningSchedule)

    @Update
    suspend fun updateSchedule(schedule: TurningSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TurningLog): Long

    @Query("SELECT * FROM turning_logs WHERE batchId = :batchId ORDER BY timestamp DESC")
    fun getLogsForBatchFlow(batchId: Long): Flow<List<TurningLog>>

    @Query("SELECT * FROM turning_logs WHERE batchId = :batchId ORDER BY timestamp DESC")
    suspend fun getLogsForBatch(batchId: Long): List<TurningLog>

    @Query("SELECT COUNT(*) FROM turning_logs WHERE batchId = :batchId AND action = 'COMPLETED'")
    fun getCompletedTurnCountFlow(batchId: Long): Flow<Int>

    @Query("SELECT * FROM turning_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogsFlow(): Flow<List<TurningLog>>
}
