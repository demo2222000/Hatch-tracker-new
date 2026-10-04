package com.example.sensor

import com.example.data.model.SensorReading
import kotlinx.coroutines.flow.StateFlow

interface IncubatorSensorSource {
    val sourceType: SensorSourceType
    val sensorState: StateFlow<SensorState>
    val latestReading: StateFlow<SensorReading?>

    suspend fun start()
    suspend fun stop()
    suspend fun setSimulatedCondition(
        targetTemp: Double,
        targetHumidity: Double,
        forceDisconnected: Boolean = false
    )
}
