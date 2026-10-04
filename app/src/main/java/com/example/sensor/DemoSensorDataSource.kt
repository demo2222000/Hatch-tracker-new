package com.example.sensor

import com.example.data.model.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class DemoSensorDataSource(
    private val scope: CoroutineScope
) : IncubatorSensorSource {

    override val sourceType: SensorSourceType = SensorSourceType.DEMO

    private val _sensorState = MutableStateFlow(
        SensorState(
            status = SensorConnectionStatus.CONNECTED_DEMO,
            sourceType = SensorSourceType.DEMO,
            deviceName = "Demo Precision Sensor",
            deviceAddress = "SIM-INC-88",
            batteryPct = 96,
            lastSeenTimestamp = System.currentTimeMillis(),
            isSimulated = true,
            currentTempC = 37.5,
            currentHumidityPct = 50.0
        )
    )
    override val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _latestReading = MutableStateFlow<SensorReading?>(null)
    override val latestReading: StateFlow<SensorReading?> = _latestReading.asStateFlow()

    private var tickerJob: Job? = null

    private var baseTargetTemp: Double = 37.5
    private var baseTargetHumidity: Double = 50.0
    private var isSimulatingDisconnect: Boolean = false

    override suspend fun start() {
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                if (isSimulatingDisconnect) {
                    _sensorState.value = _sensorState.value.copy(
                        status = SensorConnectionStatus.DISCONNECTED,
                        connectionErrorMessage = "Sensor disconnected (Demo simulation mode)"
                    )
                    _latestReading.value = null
                } else {
                    // Small natural drift around setpoint (±0.08°C, ±0.6% humidity)
                    val tempNoise = (Random.nextDouble(-0.08, 0.08) * 10).toInt() / 10.0
                    val humNoise = (Random.nextDouble(-0.6, 0.6) * 10).toInt() / 10.0

                    val roundedTemp = ((baseTargetTemp + tempNoise) * 10).toInt() / 10.0
                    val roundedHum = ((baseTargetHumidity + humNoise) * 10).toInt() / 10.0

                    val reading = SensorReading(
                        temperatureC = roundedTemp,
                        humidityPct = roundedHum,
                        isSimulated = true, // ALWAYS TRUE for demo
                        sensorSource = "DEMO",
                        deviceName = "Demo Precision Sensor",
                        timestamp = System.currentTimeMillis()
                    )

                    _sensorState.value = _sensorState.value.copy(
                        status = SensorConnectionStatus.CONNECTED_DEMO,
                        currentTempC = roundedTemp,
                        currentHumidityPct = roundedHum,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        isSimulated = true,
                        connectionErrorMessage = null
                    )
                    _latestReading.value = reading
                }

                delay(3000) // update every 3 seconds for responsive demo experience
            }
        }
    }

    override suspend fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override suspend fun setSimulatedCondition(
        targetTemp: Double,
        targetHumidity: Double,
        forceDisconnected: Boolean
    ) {
        this.baseTargetTemp = targetTemp
        this.baseTargetHumidity = targetHumidity
        this.isSimulatingDisconnect = forceDisconnected

        if (forceDisconnected) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Sensor disconnected"
            )
            _latestReading.value = null
        } else {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.CONNECTED_DEMO,
                currentTempC = targetTemp,
                currentHumidityPct = targetHumidity,
                connectionErrorMessage = null
            )
        }
    }
}
