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

    private val _targetTemp = MutableStateFlow(37.5)
    val targetTemp: StateFlow<Double> = _targetTemp.asStateFlow()

    private val _targetHumidity = MutableStateFlow(50.0)
    val targetHumidity: StateFlow<Double> = _targetHumidity.asStateFlow()

    private val _enableDrift = MutableStateFlow(false)
    val enableDrift: StateFlow<Boolean> = _enableDrift.asStateFlow()

    private var isSimulatingDisconnect: Boolean = false
    private var tickerJob: Job? = null

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
                    val baseT = _targetTemp.value
                    val baseH = _targetHumidity.value

                    // Only add small natural drift if explicitly enabled by user
                    val tempNoise = if (_enableDrift.value) (Random.nextDouble(-0.04, 0.04) * 10).toInt() / 10.0 else 0.0
                    val humNoise = if (_enableDrift.value) (Random.nextDouble(-0.3, 0.3) * 10).toInt() / 10.0 else 0.0

                    val roundedTemp = ((baseT + tempNoise) * 10).toInt() / 10.0
                    val roundedHum = ((baseH + humNoise) * 10).toInt() / 10.0

                    val reading = SensorReading(
                        temperatureC = roundedTemp,
                        humidityPct = roundedHum,
                        isSimulated = true,
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

                delay(3000)
            }
        }
    }

    override suspend fun stop() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun setEnableDrift(enabled: Boolean) {
        _enableDrift.value = enabled
    }

    override suspend fun setSimulatedCondition(
        targetTemp: Double,
        targetHumidity: Double,
        forceDisconnected: Boolean
    ) {
        _targetTemp.value = ((targetTemp * 10).toInt()) / 10.0
        _targetHumidity.value = ((targetHumidity * 10).toInt()) / 10.0
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
                currentTempC = _targetTemp.value,
                currentHumidityPct = _targetHumidity.value,
                connectionErrorMessage = null
            )
            _latestReading.value = SensorReading(
                temperatureC = _targetTemp.value,
                humidityPct = _targetHumidity.value,
                isSimulated = true,
                sensorSource = "DEMO",
                deviceName = "Demo Precision Sensor",
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
