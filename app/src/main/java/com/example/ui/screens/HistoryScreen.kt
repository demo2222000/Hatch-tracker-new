package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DetailedTrendChart
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.HistoryRange
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: IncubatorViewModel,
    modifier: Modifier = Modifier
) {
    val activeBatch by viewModel.activeBatch.collectAsState()
    val readings by viewModel.sensorReadings.collectAsState()
    val currentRange by viewModel.historyRange.collectAsState()
    val isFahrenheit by viewModel.isFahrenheit.collectAsState()

    val timeFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    val targetMinTemp = activeBatch?.minTempC ?: 37.0
    val targetMaxTemp = activeBatch?.maxTempC ?: 38.3
    val targetMinHum = activeBatch?.minHumidityPct ?: 45.0
    val targetMaxHum = activeBatch?.maxHumidityPct ?: 55.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Climate History & Analytics", fontWeight = FontWeight.Bold) }
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
            // Range Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryRange.values().forEach { range ->
                        FilterChip(
                            selected = currentRange == range,
                            onClick = { viewModel.setHistoryRange(range) },
                            label = { Text(range.label) }
                        )
                    }
                }
            }

            // Temperature History Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TEMPERATURE TREND (°C)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = AmberPrimaryLight
                            )

                            if (readings.isNotEmpty()) {
                                val avg = readings.map { it.temperatureC }.average()
                                Text(
                                    text = "Avg: ${String.format(Locale.US, "%.1f°C", avg)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DetailedTrendChart(
                            readings = readings,
                            title = "Temperature",
                            unit = "°C",
                            lineColor = AmberPrimaryLight,
                            fillColor = AmberPrimaryLight,
                            targetMin = targetMinTemp,
                            targetMax = targetMaxTemp,
                            valueExtractor = { it.temperatureC }
                        )
                    }
                }
            }

            // Humidity History Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RELATIVE HUMIDITY TREND (%)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = TealSecondaryLight
                            )

                            if (readings.isNotEmpty()) {
                                val avg = readings.map { it.humidityPct }.average()
                                Text(
                                    text = "Avg: ${String.format(Locale.US, "%.0f%%", avg)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        DetailedTrendChart(
                            readings = readings,
                            title = "Humidity",
                            unit = "%",
                            lineColor = TealSecondaryLight,
                            fillColor = TealSecondaryLight,
                            targetMin = targetMinHum,
                            targetMax = targetMaxHum,
                            valueExtractor = { it.humidityPct }
                        )
                    }
                }
            }

            // Logged Readings Table Header
            item {
                Text(
                    text = "Historical Telemetry Log (${readings.size} records)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (readings.isEmpty()) {
                item {
                    Text("No sensor readings logged in this time window.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(readings.reversed().take(50), key = { it.id }) { reading ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = timeFormat.format(Date(reading.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${reading.sensorSource} (${if (reading.isSimulated) "Demo" else "Real"})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (reading.isSimulated) AmberPrimaryLight else TealSecondaryLight
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = viewModel.formatTemperature(reading.temperatureC),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AmberPrimaryLight
                            )
                            Text(
                                text = "${reading.humidityPct.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TealSecondaryLight
                            )
                        }
                    }
                }
            }
        }
    }
}
