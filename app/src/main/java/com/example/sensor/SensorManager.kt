package com.example.sensor

import android.content.Context
import com.example.data.dao.AlertDao
import com.example.data.dao.BatchDao
import com.example.data.dao.SensorDao
import com.example.data.model.AlertLog
import com.example.data.model.SensorReading
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorManager(
    private val context: Context,
    private val sensorDao: SensorDao,
    private val batchDao: BatchDao,
    private val alertDao: AlertDao,
    private val notificationHelper: NotificationHelper,
    private val scope: CoroutineScope
) {
    val demoSource = DemoSensorDataSource(scope)
    val bleSource = BluetoothLeSensorDataSource(context, scope)

    private val _activeSourceType = MutableStateFlow(SensorSourceType.DEMO)
    val activeSourceType: StateFlow<SensorSourceType> = _activeSourceType.asStateFlow()

    private val _compositeSensorState = MutableStateFlow(demoSource.sensorState.value)
    val compositeSensorState: StateFlow<SensorState> = _compositeSensorState.asStateFlow()

    private val _latestReading = MutableStateFlow<SensorReading?>(null)
    val latestReading: StateFlow<SensorReading?> = _latestReading.asStateFlow()

    private var monitorJob: Job? = null
    private var persistenceJob: Job? = null

    // Alert cooldown trackers (ms) to prevent notification spam
    private var lastTempAlertTime = 0L
    private var lastHumAlertTime = 0L
    private var lastDisconnectAlertTime = 0L
    private val ALERT_COOLDOWN_MS = 15 * 60 * 1000L // 15 minutes cooldown

    init {
        // Start monitoring active source
        startMonitoring()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch(Dispatchers.Default) {
            // Start demo source initially
            demoSource.start()

            launch {
                demoSource.sensorState.collect { state ->
                    if (_activeSourceType.value == SensorSourceType.DEMO) {
                        _compositeSensorState.value = state
                        checkDisconnectionAlert(state)
                    }
                }
            }

            launch {
                demoSource.latestReading.collect { reading ->
                    if (_activeSourceType.value == SensorSourceType.DEMO && reading != null) {
                        _latestReading.value = reading
                        checkThresholds(reading)
                    }
                }
            }

            launch {
                bleSource.sensorState.collect { state ->
                    if (_activeSourceType.value == SensorSourceType.BLUETOOTH_LE) {
                        _compositeSensorState.value = state
                        checkDisconnectionAlert(state)
                    }
                }
            }

            launch {
                bleSource.latestReading.collect { reading ->
                    if (_activeSourceType.value == SensorSourceType.BLUETOOTH_LE && reading != null) {
                        _latestReading.value = reading
                        val activeBatch = batchDao.getActiveBatch()
                        sensorDao.insertReading(reading.copy(batchId = activeBatch?.id))
                        checkThresholds(reading)
                    }
                }
            }
        }

        // Periodic database save of sensor readings (every 10 seconds)
        persistenceJob?.cancel()
        persistenceJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(10_000)
                val reading = _latestReading.value
                if (reading != null) {
                    val activeBatch = batchDao.getActiveBatch()
                    sensorDao.insertReading(reading.copy(batchId = activeBatch?.id))
                }
            }
        }
    }

    suspend fun switchSource(type: SensorSourceType) {
        _activeSourceType.value = type
        when (type) {
            SensorSourceType.DEMO -> {
                bleSource.stop()
                demoSource.start()
                _compositeSensorState.value = demoSource.sensorState.value
                _latestReading.value = demoSource.latestReading.value
            }
            SensorSourceType.BLUETOOTH_LE -> {
                demoSource.stop()
                bleSource.start()
                _compositeSensorState.value = bleSource.sensorState.value
                _latestReading.value = bleSource.latestReading.value
            }
            SensorSourceType.LOCAL_NETWORK -> {
                // Modular stub
            }
        }
    }

    private suspend fun checkDisconnectionAlert(state: SensorState) {
        if (state.status == SensorConnectionStatus.DISCONNECTED) {
            val now = System.currentTimeMillis()
            if (now - lastDisconnectAlertTime > ALERT_COOLDOWN_MS) {
                lastDisconnectAlertTime = now
                val title = "Sensor Disconnected!"
                val msg = "Connection to ${state.deviceName} was lost. Please verify incubator telemetry."
                val activeBatch = batchDao.getActiveBatch()
                alertDao.insertAlert(
                    AlertLog(
                        batchId = activeBatch?.id,
                        alertType = "SENSOR_DISCONNECTED",
                        title = title,
                        message = msg
                    )
                )
                notificationHelper.showClimateAlertNotification(
                    notificationId = 1003,
                    title = title,
                    message = msg
                )
            }
        }
    }

    private suspend fun checkThresholds(reading: SensorReading) {
        val batch = batchDao.getActiveBatch() ?: return
        val now = System.currentTimeMillis()

        // Temperature checks
        if (reading.temperatureC > batch.maxTempC) {
            if (now - lastTempAlertTime > ALERT_COOLDOWN_MS) {
                lastTempAlertTime = now
                val title = "Temperature Too High! (${String.format("%.1f", reading.temperatureC)}°C)"
                val msg = "Temperature exceeded maximum safety limit of ${batch.maxTempC}°C for ${batch.species}."
                alertDao.insertAlert(
                    AlertLog(
                        batchId = batch.id,
                        alertType = "TEMP_HIGH",
                        title = title,
                        message = msg,
                        recordedValue = reading.temperatureC,
                        thresholdValue = batch.maxTempC
                    )
                )
                notificationHelper.showClimateAlertNotification(1001, title, msg)
            }
        } else if (reading.temperatureC < batch.minTempC) {
            if (now - lastTempAlertTime > ALERT_COOLDOWN_MS) {
                lastTempAlertTime = now
                val title = "Temperature Too Low! (${String.format("%.1f", reading.temperatureC)}°C)"
                val msg = "Temperature dropped below safe threshold of ${batch.minTempC}°C for ${batch.species}."
                alertDao.insertAlert(
                    AlertLog(
                        batchId = batch.id,
                        alertType = "TEMP_LOW",
                        title = title,
                        message = msg,
                        recordedValue = reading.temperatureC,
                        thresholdValue = batch.minTempC
                    )
                )
                notificationHelper.showClimateAlertNotification(1001, title, msg)
            }
        }

        // Humidity checks
        val effectiveMinHum = if (batch.isInLockdown()) batch.lockdownHumidityPct - 5.0 else batch.minHumidityPct
        val effectiveMaxHum = if (batch.isInLockdown()) batch.lockdownHumidityPct + 10.0 else batch.maxHumidityPct

        if (reading.humidityPct > effectiveMaxHum) {
            if (now - lastHumAlertTime > ALERT_COOLDOWN_MS) {
                lastHumAlertTime = now
                val title = "Humidity Too High! (${String.format("%.0f", reading.humidityPct)}%)"
                val msg = "Humidity is above optimal ceiling of ${effectiveMaxHum}%. Add ventilation."
                alertDao.insertAlert(
                    AlertLog(
                        batchId = batch.id,
                        alertType = "HUMIDITY_HIGH",
                        title = title,
                        message = msg,
                        recordedValue = reading.humidityPct,
                        thresholdValue = effectiveMaxHum
                    )
                )
                notificationHelper.showClimateAlertNotification(1002, title, msg)
            }
        } else if (reading.humidityPct < effectiveMinHum) {
            if (now - lastHumAlertTime > ALERT_COOLDOWN_MS) {
                lastHumAlertTime = now
                val title = "Humidity Too Low! (${String.format("%.0f", reading.humidityPct)}%)"
                val msg = "Humidity dropped below minimum of ${effectiveMinHum}%. Replenish water channels."
                alertDao.insertAlert(
                    AlertLog(
                        batchId = batch.id,
                        alertType = "HUMIDITY_LOW",
                        title = title,
                        message = msg,
                        recordedValue = reading.humidityPct,
                        thresholdValue = effectiveMinHum
                    )
                )
                notificationHelper.showClimateAlertNotification(1002, title, msg)
            }
        }
    }
}
