package com.example.sensor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.example.data.model.SensorReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Standard Bluetooth SIG Environmental Sensing Service UUIDs
 */
val ESS_SERVICE_UUID: UUID = UUID.fromString("0000181A-0000-1000-8000-00805F9B34FB")
val ESS_TEMP_CHAR_UUID: UUID = UUID.fromString("00002A6E-0000-1000-8000-00805F9B34FB")
val ESS_HUM_CHAR_UUID: UUID = UUID.fromString("00002A6F-0000-1000-8000-00805F9B34FB")

/**
 * Standard Client Characteristic Configuration Descriptor (CCCD) for enabling notifications
 */
val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

/**
 * Common Custom ESP32 Incubator Sensor Service UUIDs (for DIY Arduino/ESP32 incubators)
 */
val ESP32_INCUBATOR_SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
val ESP32_CLIMATE_CHAR_UUID: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

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
    private var lastConnectedAddress: String? = null

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
                connectionErrorMessage = "Bluetooth is turned off on device"
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
        lastConnectedAddress = address

        val device: BluetoothDevice? = try {
            bluetoothAdapter?.getRemoteDevice(address)
        } catch (_: Exception) {
            null
        }

        if (device == null) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Device $address not found"
            )
            return
        }

        try {
            activeGatt?.close()
            activeGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            _sensorState.value = _sensorState.value.copy(
                status = SensorConnectionStatus.DISCONNECTED,
                connectionErrorMessage = "Bluetooth connect permission required"
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val device = gatt?.device
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                scope.launch(Dispatchers.Default) {
                    _sensorState.value = _sensorState.value.copy(
                        status = SensorConnectionStatus.CONNECTED_REAL,
                        deviceName = device?.name ?: "BLE Incubator Sensor",
                        deviceAddress = device?.address ?: "",
                        isSimulated = false,
                        connectionErrorMessage = null
                    )
                }
                // Discover GATT services on the connected sensor
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

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                scope.launch(Dispatchers.Default) {
                    _sensorState.value = _sensorState.value.copy(
                        connectionErrorMessage = "Failed to discover GATT services ($status)"
                    )
                }
                return
            }

            scope.launch(Dispatchers.Default) {
                var foundEnvironmentalService = false

                // 1. Check for standard Bluetooth SIG Environmental Sensing Service (0x181A)
                val essService = gatt.getService(ESS_SERVICE_UUID)
                if (essService != null) {
                    foundEnvironmentalService = true
                    val tempChar = essService.getCharacteristic(ESS_TEMP_CHAR_UUID)
                    val humChar = essService.getCharacteristic(ESS_HUM_CHAR_UUID)

                    tempChar?.let { subscribeCharacteristic(gatt, it) }
                    humChar?.let { subscribeCharacteristic(gatt, it) }
                }

                // 2. Check for custom ESP32 Incubator Service (0xFFE0)
                val esp32Service = gatt.getService(ESP32_INCUBATOR_SERVICE_UUID)
                if (esp32Service != null) {
                    foundEnvironmentalService = true
                    val climateChar = esp32Service.getCharacteristic(ESP32_CLIMATE_CHAR_UUID)
                    climateChar?.let { subscribeCharacteristic(gatt, it) }
                }

                // 3. Fallback: Search all discovered services for temperature / humidity characteristics
                if (!foundEnvironmentalService) {
                    for (service in gatt.services) {
                        for (characteristic in service.characteristics) {
                            if (characteristic.uuid == ESS_TEMP_CHAR_UUID ||
                                characteristic.uuid == ESS_HUM_CHAR_UUID ||
                                characteristic.uuid == ESP32_CLIMATE_CHAR_UUID
                            ) {
                                foundEnvironmentalService = true
                                subscribeCharacteristic(gatt, characteristic)
                            }
                        }
                    }
                }

                if (!foundEnvironmentalService) {
                    _sensorState.value = _sensorState.value.copy(
                        connectionErrorMessage = "Connected, but no Environmental Sensing characteristics found"
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun subscribeCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            // Enable notifications locally
            gatt.setCharacteristicNotification(characteristic, true)

            // Write to the Client Characteristic Configuration Descriptor (CCCD 0x2902)
            val descriptor = characteristic.getDescriptor(CCCD_DESCRIPTOR_UUID)
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
            }

            // Also trigger an immediate read so data appears right away
            gatt.readCharacteristic(characteristic)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.let {
                @Suppress("DEPRECATION")
                handleCharacteristicData(it.uuid, it.value)
            }
        }

        // Android 13+ (API 33+) callback signature
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicData(characteristic.uuid, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                @Suppress("DEPRECATION")
                handleCharacteristicData(characteristic.uuid, characteristic.value)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic.uuid, value)
            }
        }
    }

    private fun handleCharacteristicData(uuid: UUID, value: ByteArray?) {
        if (value == null || value.isEmpty()) return

        when (uuid) {
            // Standard ESS Temperature (UUID 0x2A6E): 16-bit signed int in 0.01 °C
            ESS_TEMP_CHAR_UUID -> {
                if (value.size >= 2) {
                    val raw = (value[0].toInt() and 0xFF) or (value[1].toInt() shl 8)
                    val signedRaw = raw.toShort()
                    val tempC = signedRaw / 100.0
                    val current = _sensorState.value
                    _sensorState.value = current.copy(
                        currentTempC = tempC,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    publishReading(tempC, current.currentHumidityPct)
                }
            }

            // Standard ESS Humidity (UUID 0x2A6F): 16-bit unsigned int in 0.01 %
            ESS_HUM_CHAR_UUID -> {
                if (value.size >= 2) {
                    val raw = (value[0].toInt() and 0xFF) or ((value[1].toInt() and 0xFF) shl 8)
                    val humPct = raw / 100.0
                    val current = _sensorState.value
                    _sensorState.value = current.copy(
                        currentHumidityPct = humPct,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    publishReading(current.currentTempC, humPct)
                }
            }

            // Custom ESP32 Incubator Format: parses string like "T:37.5,H:50.0" or binary
            ESP32_CLIMATE_CHAR_UUID -> {
                val text = String(value, StandardCharsets.UTF_8).trim()
                var parsedTemp: Double? = null
                var parsedHum: Double? = null

                if (text.contains("T:") || text.contains("H:")) {
                    // String protocol
                    val parts = text.split(",")
                    for (part in parts) {
                        val trimmed = part.trim()
                        if (trimmed.startsWith("T:")) {
                            parsedTemp = trimmed.substring(2).toDoubleOrNull()
                        } else if (trimmed.startsWith("H:")) {
                            parsedHum = trimmed.substring(2).toDoubleOrNull()
                        }
                    }
                } else if (value.size >= 4) {
                    // Binary protocol: 2 bytes temp (x100), 2 bytes hum (x100)
                    val rawTemp = ((value[0].toInt() and 0xFF) or (value[1].toInt() shl 8)).toShort()
                    val rawHum = (value[2].toInt() and 0xFF) or ((value[3].toInt() and 0xFF) shl 8)
                    parsedTemp = rawTemp / 100.0
                    parsedHum = rawHum / 100.0
                }

                if (parsedTemp != null || parsedHum != null) {
                    val current = _sensorState.value
                    val finalTemp = parsedTemp ?: current.currentTempC
                    val finalHum = parsedHum ?: current.currentHumidityPct

                    _sensorState.value = current.copy(
                        currentTempC = finalTemp,
                        currentHumidityPct = finalHum,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    publishReading(finalTemp, finalHum)
                }
            }
        }
    }

    private fun publishReading(tempC: Double, humPct: Double) {
        val reading = SensorReading(
            temperatureC = tempC,
            humidityPct = humPct,
            isSimulated = false, // REAL PHYSICAL SENSOR DATA
            sensorSource = "BLE",
            deviceName = _sensorState.value.deviceName,
            timestamp = System.currentTimeMillis()
        )
        _latestReading.value = reading
    }

    override suspend fun start() {
        // If an address was previously connected, attempt auto-reconnect
        lastConnectedAddress?.let { address ->
            connectToDevice(address)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stop() {
        stopScan()
        try {
            activeGatt?.disconnect()
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
