package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight
import kotlin.math.sin

/**
 * An animated, laboratory-grade telemetry visualizer featuring expanding sonar radar waves,
 * dynamic thermometer mercury column, and animated humidity water-level ripples.
 */
@Composable
fun AnimatedSensorTelemetryInstrument(
    temperatureC: Double,
    humidityPct: Double,
    formattedTemp: String,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sensor_instrument_anim")

    // Sonar radar wave expansion
    val radarWave1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave_1"
    )
    val radarAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha_1"
    )

    // Moisture wave bobbing
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "water_wave_phase"
    )

    // Smooth animated values
    val animatedTempFraction by animateFloatAsState(
        targetValue = ((temperatureC - 35.0) / (40.0 - 35.0)).toFloat().coerceIn(0.05f, 0.95f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "anim_mercury"
    )
    val animatedHumidityFraction by animateFloatAsState(
        targetValue = (humidityPct / 100.0).toFloat().coerceIn(0.05f, 0.95f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "anim_humidity"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with Live Pulse Radar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StatusOptimal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(radarWave1)
                                    .clip(CircleShape)
                                    .background(StatusOptimal.copy(alpha = radarAlpha1))
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = StatusOptimal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LIVE TELEMETRY WAVEFORM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isLive) "Receiving Telemetry Packets (1 Hz)" else "Sensor Disconnected",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isLive) StatusOptimal else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isLive) StatusOptimal else MaterialTheme.colorScheme.error).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isLive) "STREAMING" else "OFFLINE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = if (isLive) StatusOptimal else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Interactive Instruments: Animated Thermometer & Moisture Chamber
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Animated Thermometer Tube
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = AmberPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TEMPERATURE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formattedTemp,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = AmberPrimaryLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Thermometer Canvas
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                            val w = size.width
                            val h = size.height

                            // Track
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(0f, h * 0.25f),
                                size = Size(w, h * 0.5f),
                                cornerRadius = CornerRadius(h * 0.25f, h * 0.25f)
                            )

                            // Animated Liquid Mercury
                            val fillWidth = (w * animatedTempFraction).coerceIn(8.dp.toPx(), w)
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(GoldTertiaryLight, AmberPrimaryLight, Color(0xFFFF5722))
                                ),
                                topLeft = Offset(0f, h * 0.25f),
                                size = Size(fillWidth, h * 0.5f),
                                cornerRadius = CornerRadius(h * 0.25f, h * 0.25f)
                            )

                            // Bulb at right end of liquid
                            drawCircle(
                                color = Color(0xFFFF5722),
                                center = Offset(fillWidth, h * 0.5f),
                                radius = h * 0.45f
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("35.0°C", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Optimal (37.5°)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = StatusOptimal, fontWeight = FontWeight.Bold)
                            Text("40.0°C", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 2. Animated Moisture & Humidity Chamber
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = TealSecondaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HUMIDITY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${humidityPct.toInt()}% RH",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            ),
                            color = TealSecondaryLight
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Humidity Wave Canvas
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                            val w = size.width
                            val h = size.height

                            // Track
                            drawRoundRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(0f, h * 0.25f),
                                size = Size(w, h * 0.5f),
                                cornerRadius = CornerRadius(h * 0.25f, h * 0.25f)
                            )

                            // Animated Water Fill
                            val fillWidth = (w * animatedHumidityFraction).coerceIn(8.dp.toPx(), w)
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF80DEEA), TealSecondaryLight, Color(0xFF00897B))
                                ),
                                topLeft = Offset(0f, h * 0.25f),
                                size = Size(fillWidth, h * 0.5f),
                                cornerRadius = CornerRadius(h * 0.25f, h * 0.25f)
                            )

                            // Floating water droplet head
                            drawCircle(
                                color = Color(0xFF00897B),
                                center = Offset(fillWidth, h * 0.5f),
                                radius = h * 0.45f
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("30%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Lockdown (65%)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = TealSecondaryLight, fontWeight = FontWeight.Bold)
                            Text("90%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
