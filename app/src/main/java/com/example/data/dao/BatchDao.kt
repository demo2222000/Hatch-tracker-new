package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IncubationBatch
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchDao {
    @Query("SELECT * FROM incubation_batches ORDER BY isActive DESC, startDate DESC")
    fun getAllBatches(): Flow<List<IncubationBatch>>

    @Query("SELECT * FROM incubation_batches WHERE isActive = 1 LIMIT 1")
    fun getActiveBatchFlow(): Flow<IncubationBatch?>

    @Query("SELECT * FROM incubation_batches WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBatch(): IncubationBatch?

    @Query("SELECT * FROM incubation_batches WHERE id = :id")
    fun getBatchByIdFlow(id: Long): Flow<IncubationBatch?>

    @Query("SELECT * FROM incubation_batches WHERE id = :id")
    suspend fun getBatchById(id: Long): IncubationBatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: IncubationBatch): Long

    @Update
    suspend fun updateBatch(batch: IncubationBatch)

    @Query("UPDATE incubation_batches SET isActive = 0 WHERE id != :activeId")
    suspend fun deactivateOtherBatches(activeId: Long)

    @Query("DELETE FROM incubation_batches WHERE id = :id")
    suspend fun deleteBatch(id: Long)
}
