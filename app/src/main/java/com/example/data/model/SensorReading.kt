package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_readings")
data class SensorReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Double,
    val humidityPct: Double,
    /**
     * CRITICAL: Clearly distinguishes real sensor data from demo / test simulation data.
     */
    val isSimulated: Boolean,
    val sensorSource: String = "DEMO", // "DEMO", "BLE", "LOCAL_NETWORK", "MANUAL"
    val deviceName: String = "Internal Demo Sensor"
) {
    val temperatureF: Double
        get() = (temperatureC * 9.0 / 5.0) + 32.0
}
