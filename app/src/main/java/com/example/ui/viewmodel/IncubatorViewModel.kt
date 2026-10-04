package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HatchMasterApplication
import com.example.data.model.AlertLog
import com.example.data.model.IncubationBatch
import com.example.data.model.SensorReading
import com.example.data.model.TurningLog
import com.example.data.model.TurningSchedule
import com.example.sensor.DiscoveredSensorDevice
import com.example.sensor.SensorSourceType
import com.example.sensor.SensorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class HistoryRange(val label: String, val durationMs: Long) {
    LAST_6_HOURS("6h", TimeUnit.HOURS.toMillis(6)),
    LAST_24_HOURS("24h", TimeUnit.HOURS.toMillis(24)),
    LAST_7_DAYS("7d", TimeUnit.DAYS.toMillis(7)),
    ALL("All", TimeUnit.DAYS.toMillis(365))
}

@OptIn(ExperimentalCoroutinesApi::class)
class IncubatorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HatchMasterApplication
    private val repository = app.repository
    private val sensorManager = app.sensorManager
    private val alarmScheduler = app.alarmScheduler

    // Preference: Temperature Unit (°C vs °F)
    private val prefs = application.getSharedPreferences("hatchmaster_prefs", Context.MODE_PRIVATE)
    private val _isFahrenheit = MutableStateFlow(prefs.getBoolean("is_fahrenheit", false))
    val isFahrenheit: StateFlow<Boolean> = _isFahrenheit.asStateFlow()

    fun toggleTempUnit(useFahrenheit: Boolean) {
        _isFahrenheit.value = useFahrenheit
        prefs.edit().putBoolean("is_fahrenheit", useFahrenheit).apply()
    }

    fun formatTemperature(tempC: Double): String {
        return if (_isFahrenheit.value) {
            val f = (tempC * 9.0 / 5.0) + 32.0
            String.format(java.util.Locale.US, "%.1f°F", f)
        } else {
            String.format(java.util.Locale.US, "%.1f°C", tempC)
        }
    }

    // Active Batch
    val activeBatch: StateFlow<IncubationBatch?> = repository.activeBatchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allBatches: StateFlow<List<IncubationBatch>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Turning Schedule for Active Batch
    val turningSchedule: StateFlow<TurningSchedule?> = activeBatch.flatMapLatest { batch ->
        if (batch != null) repository.getTurningScheduleFlow(batch.id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Turning Logs for Active Batch
    val turningLogs: StateFlow<List<TurningLog>> = activeBatch.flatMapLatest { batch ->
        if (batch != null) repository.getTurningLogsFlow(batch.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sensor State and Latest Reading
    val sensorState: StateFlow<SensorState> = sensorManager.compositeSensorState
    val latestReading: StateFlow<SensorReading?> = sensorManager.latestReading
    val activeSensorType: StateFlow<SensorSourceType> = sensorManager.activeSourceType
    val discoveredBleDevices: StateFlow<List<DiscoveredSensorDevice>> = sensorManager.bleSource.discoveredDevices

    // History filter & readings
    private val _historyRange = MutableStateFlow(HistoryRange.LAST_24_HOURS)
    val historyRange: StateFlow<HistoryRange> = _historyRange.asStateFlow()

    fun setHistoryRange(range: HistoryRange) {
        _historyRange.value = range
    }

    val sensorReadings: StateFlow<List<SensorReading>> = _historyRange.flatMapLatest { range ->
        val cutoff = System.currentTimeMillis() - range.durationMs
        repository.getReadingsSinceFlow(cutoff)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Alerts
    val alerts: StateFlow<List<AlertLog>> = repository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unacknowledgedAlertsCount: StateFlow<Int> = repository.unacknowledgedAlertCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Batch Management
    fun createBatch(batch: IncubationBatch, intervalHours: Int = 2, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.createBatch(batch, intervalHours)
            onCreated(newId)
        }
    }

    fun updateBatch(batch: IncubationBatch) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBatch(batch)
        }
    }

    fun recordRottedEggRemoved(batchId: Long, count: Int = 1, timestamp: Long = System.currentTimeMillis(), notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getBatchById(batchId) ?: return@launch
            val updated = current.copy(
                rottedEggsRemoved = current.rottedEggsRemoved + count,
                lastEggRottedTimestamp = timestamp,
                eggRottedNotes = if (notes.isNotBlank()) notes else current.eggRottedNotes
            )
            repository.updateBatch(updated)
        }
    }

    fun setActiveBatch(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveBatch(id)
        }
    }

    fun deleteBatch(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBatch(id)
        }
    }

    // Egg Turning Actions
    fun recordTurnNow(batchId: Long, orientation: String, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordTurn(batchId, orientation, "COMPLETED", notes)
        }
    }

    fun updateTurningSchedule(schedule: TurningSchedule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTurningSchedule(schedule)
        }
    }

    // Sensor Controls
    fun setSensorSource(type: SensorSourceType) {
        viewModelScope.launch {
            sensorManager.switchSource(type)
        }
    }

    fun updateDemoSimulation(targetTemp: Double, targetHum: Double, forceDisconnected: Boolean = false) {
        viewModelScope.launch {
            sensorManager.demoSource.setSimulatedCondition(targetTemp, targetHum, forceDisconnected)
        }
    }

    fun startBleScan() {
        sensorManager.bleSource.startScan()
    }

    fun stopBleScan() {
        sensorManager.bleSource.stopScan()
    }

    fun connectBleDevice(address: String) {
        sensorManager.bleSource.connectToDevice(address)
    }

    // Alerts Management
    fun acknowledgeAlert(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.acknowledgeAlert(id)
        }
    }

    fun acknowledgeAllAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.acknowledgeAllAlerts()
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAlerts()
        }
    }

    // PDF Report
    fun generatePdfReport(batchId: Long, onReady: (Intent?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = repository.generatePdfReport(batchId)
            launch(Dispatchers.Main) {
                onReady(intent)
            }
        }
    }

    // Alarm Permission Guide
    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()
    fun getExactAlarmSettingIntent(): Intent = alarmScheduler.createExactAlarmSettingIntent()
}
