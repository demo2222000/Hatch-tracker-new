package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorReading
import com.example.sensor.SensorConnectionStatus
import com.example.sensor.SensorState
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusDemo
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TealSecondaryLight

@Composable
fun ClimateGaugeCard(
    sensorState: SensorState,
    latestReading: SensorReading?,
    recentReadings: List<SensorReading>,
    targetTempC: Double,
    minTempC: Double,
    maxTempC: Double,
    targetHumidityPct: Double,
    minHumidityPct: Double,
    maxHumidityPct: Double,
    isFahrenheit: Boolean,
    formatTemp: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("climate_gauge_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Sensor mode indicator (CRITICAL: Differentiates Real, Demo, Disconnected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INCUBATOR CLIMATE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SensorStatusBadge(status = sensorState.status, isSimulated = sensorState.isSimulated)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (sensorState.status == SensorConnectionStatus.DISCONNECTED) {
                // Show disconnected state clearly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StatusCritical.copy(alpha = 0.1f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Sensor Disconnected",
                            tint = StatusCritical,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sensor Disconnected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusCritical
                        )
                        Text(
                            text = sensorState.connectionErrorMessage ?: "No live telemetry available. Check power or reconnect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Climate Readouts: Two Columns (Temperature & Humidity)
                val currentTemp = latestReading?.temperatureC ?: sensorState.currentTempC
                val currentHum = latestReading?.humidityPct ?: sensorState.currentHumidityPct

                // Temp status evaluation
                val tempStatus = when {
                    currentTemp > maxTempC -> "HIGH"
                    currentTemp < minTempC -> "LOW"
                    else -> "OPTIMAL"
                }
                val tempStatusColor = when (tempStatus) {
                    "OPTIMAL" -> StatusOptimal
                    "HIGH" -> StatusCritical
                    else -> StatusWarning
                }

                // Humidity status evaluation
                val humStatus = when {
                    currentHum > maxHumidityPct -> "HIGH"
                    currentHum < minHumidityPct -> "LOW"
                    else -> "OPTIMAL"
                }
                val humStatusColor = when (humStatus) {
                    "OPTIMAL" -> StatusOptimal
                    "HIGH" -> StatusCritical
                    else -> StatusWarning
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Temperature Column
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = "Temperature",
                                        tint = AmberPrimaryLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TEMP",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(tempStatusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tempStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = tempStatusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formatTemp(currentTemp),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Target: ${formatTemp(targetTempC)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sparkline chart
                            MiniTrendChart(
                                readings = recentReadings,
                                lineColor = AmberPrimaryLight,
                                fillColor = AmberPrimaryLight,
                                valueExtractor = { it.temperatureC },
                                minTarget = minTempC,
                                maxTarget = maxTempC,
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }

                    // Humidity Column
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Air,
                                        contentDescription = "Humidity",
                                        tint = TealSecondaryLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "HUMIDITY",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(humStatusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = humStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = humStatusColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${currentHum.toInt()}%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Target: ${targetHumidityPct.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sparkline chart
                            MiniTrendChart(
                                readings = recentReadings,
                                lineColor = TealSecondaryLight,
                                fillColor = TealSecondaryLight,
                                valueExtractor = { it.humidityPct },
                                minTarget = minHumidityPct,
                                maxTarget = maxHumidityPct,
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorStatusBadge(
    status: SensorConnectionStatus,
    isSimulated: Boolean,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeBg, badgeTextColor, dotColor) = when {
        status == SensorConnectionStatus.DISCONNECTED -> {
            Quad("DISCONNECTED", StatusCritical.copy(alpha = 0.15f), StatusCritical, StatusCritical)
        }
        status == SensorConnectionStatus.SCANNING -> {
            Quad("SCANNING...", StatusWarning.copy(alpha = 0.15f), StatusWarning, StatusWarning)
        }
        isSimulated -> {
            Quad("DEMO SENSOR", StatusDemo.copy(alpha = 0.15f), StatusDemo, StatusDemo)
        }
        else -> {
            Quad("REAL SENSOR", StatusOptimal.copy(alpha = 0.15f), StatusOptimal, StatusOptimal)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                ),
                color = badgeTextColor
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
