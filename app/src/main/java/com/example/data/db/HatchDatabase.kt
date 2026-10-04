package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlertDao
import com.example.data.dao.BatchDao
import com.example.data.dao.SensorDao
import com.example.data.dao.TurningDao
import com.example.data.model.AlertLog
import com.example.data.model.IncubationBatch
import com.example.data.model.SensorReading
import com.example.data.model.TurningLog
import com.example.data.model.TurningSchedule

@Database(
    entities = [
        IncubationBatch::class,
        SensorReading::class,
        TurningSchedule::class,
        TurningLog::class,
        AlertLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HatchDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun sensorDao(): SensorDao
    abstract fun turningDao(): TurningDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: HatchDatabase? = null

        fun getInstance(context: Context): HatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HatchDatabase::class.java,
                    "hatchmaster_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
