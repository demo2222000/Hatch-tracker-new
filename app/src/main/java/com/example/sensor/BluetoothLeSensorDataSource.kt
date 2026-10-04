package com.example.sensor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.example.data.model.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Standard Bluetooth SIG Environmental Sensing Service UUIDs
 */
val ESS_SERVICE_UUID: UUID = UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB")
val ESS_TEMP_CHAR_UUID: UUID = UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB")
val ESS_HUM_CHAR_UUID: UUID = UUID.fromString("00002A6F-0000-1000-8000-00805F9B34FB")

class BluetoothLeSensorDataSource(
    private val context: Context,
    private val scope: CoroutineScope
) : IncubatorSensorSource {

    override val sourceType: SensorSourceType = SensorSourceType.BLUETOOTH_LE

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _sensorState = MutableStateFlow(
        SensorState(
            status = SensorConnectionStatus.DISCONNECTED,
            sourceType = SensorSourceType.BLUETOOTH_LE,
            deviceName = "No BLE Sensor Connected",
            deviceAddress = "",
            isSimulated = false,
            currentTempC = 0.0,
            currentHumidityPct = 0.0,
            connectionErrorMessage = "Sensor disconnected"
        )
    )
    override val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _latestReading = MutableStateFlow<SensorReading?>(null)
    override val latestReading: StateFlow<SensorReading?> = _latestReading.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredSensorDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredSensorDevice>> = _discoveredDevices.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: "Incubator Beacon (${device.address.takeLast(5)})"
                val existing = _discoveredDevices.value
                if (existing.none { it.address == device.address }) {
                    _discoveredDevices.value = existing + DiscoveredSensorDevice(
                        name = name,
                        address = device.address,
                        rssi = result.rssi
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Bluetooth is turned off"
            )
            return
        }

        try {
            _discoveredDevices.value = emptyList()
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                isScanning = true
                _sensorState.value = _sensorState.value.copy(status = SensorConnectionStatus.SCANNING)
                scanner.startScan(scanCallback)
            }
        } catch (e: SecurityException) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Bluetooth scan permission required"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {}
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        stopScan()
        val device: BluetoothDevice? = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (_: Exception) {
            null
        }

        if (device == null) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Device not found"
            )
            return
        }

        try {
            activeGatt?.close()
            activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        scope.launch(Dispatchers.Default) {
                            _sensorState.value = _sensorState.value.copy(
                                status = SensorConnectionStatus.CONNECTED_REAL,
                                deviceName = device.name ?: "BLE Incubator Sensor",
                                deviceAddress = device.address,
                                isSimulated = false,
                                connectionErrorMessage = null
                            )
                        }
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        scope.launch(Dispatchers.Default) {
                            _sensorState.value = _sensorState.value.copy(
                                status = SensorConnectionStatus.DISCONNECTED,
                                isSimulated = false,
                                connectionErrorMessage = "Sensor disconnected"
                            )
                            _latestReading.value = null
                        }
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?
                ) {
                    characteristic?.let { handleCharacteristicData(it) }
                }
            })
        } catch (e: SecurityException) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Bluetooth connect permission required"
            )
        }
    }

    private fun handleCharacteristicData(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value ?: return
        if (characteristic.uuid == ESS_TEMP_CHAR_UUID && value.size >= 2) {
            val raw = (value[0].toInt() and 0xFF) or (value[1].toInt() shl 8)
            val tempC = raw / 100.0
            val current = _sensorState.value
            _sensorState.value = current.copy(currentTempC = tempC)
            publishReading(tempC, current.currentHumidityPct)
        } else if (characteristic.uuid == ESS_HUM_CHAR_UUID && value.size >= 2) {
            val raw = (value[0].toInt() and 0xFF) or (value[1].toInt() shl 8)
            val hum = raw / 100.0
            val current = _sensorState.value
            _sensorState.value = current.copy(currentHumidityPct = hum)
            publishReading(current.currentTempC, hum)
        }
    }

    private fun publishReading(tempC: Double, humPct: Double) {
        val reading = SensorReading(
            temperatureC = tempC,
            humidityPct = humPct,
            isSimulated = false,
            sensorSource = "BLE",
            deviceName = _sensorState.value.deviceName,
            timestamp = System.currentTimeMillis()
        )
        _latestReading.value = reading
    }

    override suspend fun start() {
        // Starts BLE monitoring if already paired/connected
    }

    @SuppressLint("MissingPermission")
    override suspend fun stop() {
        stopScan()
        try {
            activeGatt?.close()
            activeGatt = null
        } catch (_: Exception) {}
        _sensorState.value = _sensorState.value.copy(
            status = SensorConnectionStatus.DISCONNECTED,
            connectionErrorMessage = "Sensor disconnected"
        )
        _latestReading.value = null
    }

    override suspend fun setSimulatedCondition(
        targetTemp: Double,
        targetHumidity: Double,
        forceDisconnected: Boolean
    ) {
        // Real BLE source does not generate simulated conditions
    }
}
