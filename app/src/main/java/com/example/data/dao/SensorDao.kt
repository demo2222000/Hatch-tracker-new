package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SensorReading
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReadingFlow(): Flow<SensorReading?>

    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getReadingsSinceFlow(sinceTimestamp: Long): Flow<List<SensorReading>>

    @Query("SELECT * FROM sensor_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getReadingsSince(sinceTimestamp: Long): List<SensorReading>

    @Query("SELECT * FROM sensor_readings WHERE batchId = :batchId ORDER BY timestamp ASC")
    suspend fun getReadingsForBatch(batchId: Long): List<SensorReading>

    @Query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadingsFlow(limit: Int = 100): Flow<List<SensorReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SensorReading): Long

    @Query("DELETE FROM sensor_readings WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOldReadings(cutoffTimestamp: Long)
}
