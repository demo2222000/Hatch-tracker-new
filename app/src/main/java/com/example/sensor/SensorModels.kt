package com.example.sensor

enum class SensorConnectionStatus {
    CONNECTED_REAL,
    CONNECTED_DEMO,
    DISCONNECTED,
    SCANNING
}

enum class SensorSourceType {
    DEMO,
    BLUETOOTH_LE,
    LOCAL_NETWORK
}

data class DiscoveredSensorDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isSupported: Boolean = true
)

data class SensorState(
    val status: SensorConnectionStatus = SensorConnectionStatus.CONNECTED_DEMO,
    val sourceType: SensorSourceType = SensorSourceType.DEMO,
    val deviceName: String = "Demo Simulator Sensor",
    val deviceAddress: String = "DEMO-001",
    val batteryPct: Int? = 94,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = true,
    val currentTempC: Double = 37.5,
    val currentHumidityPct: Double = 50.0,
    val connectionErrorMessage: String? = null
) {
    val currentTempF: Double
        get() = (currentTempC * 9.0 / 5.0) + 32.0
}
