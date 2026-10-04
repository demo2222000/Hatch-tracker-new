package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.DiscoveredSensorDevice
import com.example.sensor.SensorConnectionStatus
import com.example.sensor.SensorSourceType
import com.example.ui.components.SensorStatusBadge
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusDemo
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(
    viewModel: IncubatorViewModel,
    modifier: Modifier = Modifier
) {
    val sensorState by viewModel.sensorState.collectAsState()
    val latestReading by viewModel.latestReading.collectAsState()
    val activeSourceType by viewModel.activeSensorType.collectAsState()
    val discoveredDevices by viewModel.discoveredBleDevices.collectAsState()
    val isFahrenheit by viewModel.isFahrenheit.collectAsState()

    var simTemp by remember { mutableDoubleStateOf(37.5) }
    var simHum by remember { mutableDoubleStateOf(50.0) }
    var forceDisconnect by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sensor Telemetry & Architecture", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Selector Tabs: Demo vs BLE Physical Sensor
            item {
                TabRow(
                    selectedTabIndex = if (activeSourceType == SensorSourceType.DEMO) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeSourceType == SensorSourceType.DEMO,
                        onClick = { viewModel.setSensorSource(SensorSourceType.DEMO) },
                        text = { Text("Demo Simulator Mode", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeSourceType == SensorSourceType.BLUETOOTH_LE,
                        onClick = { viewModel.setSensorSource(SensorSourceType.BLUETOOTH_LE) },
                        text = { Text("Physical BLE Sensor", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Current Telemetry Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT TELEMETRY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            SensorStatusBadge(
                                status = sensorState.status,
                                isSimulated = sensorState.isSimulated
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (sensorState.status == SensorConnectionStatus.DISCONNECTED) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StatusCritical.copy(alpha = 0.1f))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.WifiOff, contentDescription = null, tint = StatusCritical)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Sensor Disconnected", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = StatusCritical)
                                    Text(sensorState.connectionErrorMessage ?: "No telemetry packet received. Real sensor is offline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            val temp = latestReading?.temperatureC ?: sensorState.currentTempC
                            val hum = latestReading?.humidityPct ?: sensorState.currentHumidityPct

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Temperature", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = viewModel.formatTemperature(temp),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = AmberPrimaryLight
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Humidity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${hum.toInt()}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = TealSecondaryLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Source: ${sensorState.deviceName} (${if (sensorState.isSimulated) "Simulated Data" else "Real Hardware"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Demo Simulator Controls (when Demo mode is active)
            if (activeSourceType == SensorSourceType.DEMO) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DEMO SIMULATOR CONTROLS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = StatusDemo
                                )
                                Text("Test Alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Simulated Temperature: ${String.format(Locale.US, "%.1f", simTemp)}°C", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Slider(
                                value = simTemp.toFloat(),
                                onValueChange = {
                                    simTemp = ((it * 10).toInt()) / 10.0
                                    forceDisconnect = false
                                    viewModel.updateDemoSimulation(simTemp, simHum, false)
                                },
                                valueRange = 34.0f..41.0f,
                                modifier = Modifier.testTag("slider_sim_temp")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Simulated Humidity: ${simHum.toInt()}%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Slider(
                                value = simHum.toFloat(),
                                onValueChange = {
                                    simHum = ((it * 10).toInt()) / 10.0
                                    forceDisconnect = false
                                    viewModel.updateDemoSimulation(simTemp, simHum, false)
                                },
                                valueRange = 25.0f..90.0f,
                                modifier = Modifier.testTag("slider_sim_humidity")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Quick Presets to test alerts
                            Text("Quick Trigger Test Buttons:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        simTemp = 37.5
                                        simHum = 50.0
                                        forceDisconnect = false
                                        viewModel.updateDemoSimulation(37.5, 50.0, false)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Optimal")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        simTemp = 39.5
                                        forceDisconnect = false
                                        viewModel.updateDemoSimulation(39.5, simHum, false)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("High Temp!")
                                }

                                FilledTonalButton(
                                    onClick = {
                                        forceDisconnect = !forceDisconnect
                                        viewModel.updateDemoSimulation(simTemp, simHum, forceDisconnect)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (forceDisconnect) "Reconnect" else "Disconnect")
                                }
                            }
                        }
                    }
                }
            } else {
                // Physical Bluetooth LE Scanner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("BLE INCUBATOR SENSOR SCANNER", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                                    Text("Supports ESP32, DHT22, SHT31 & Environmental Sensing beacons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.startBleScan() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan for BLE Sensors")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.stopBleScan() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Stop Scan")
                                }
                            }

                            if (discoveredDevices.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Nearby Sensor Beacons:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))

                                discoveredDevices.forEach { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                            .clickable { viewModel.connectBleDevice(device.address) }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(device.name, fontWeight = FontWeight.Bold)
                                            Text("${device.address} • RSSI ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { viewModel.connectBleDevice(device.address) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Architecture note
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MODULAR ARCHITECTURE SPECIFICATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The application implements IncubatorSensorSource with zero cloud dependency. A custom ESP32 or BLE hygrometer can broadcast directly via standard Environmental Sensing Service (UUID 0x181A) or local subnet UDP without requiring external internet or API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
