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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TurningLog
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusOptimal
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EggTurningScreen(
    viewModel: IncubatorViewModel,
    modifier: Modifier = Modifier
) {
    val activeBatch by viewModel.activeBatch.collectAsState()
    val schedule by viewModel.turningSchedule.collectAsState()
    val turningLogs by viewModel.turningLogs.collectAsState()
    val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val now = System.currentTimeMillis()
    val batch = activeBatch

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Egg Turning Management", fontWeight = FontWeight.Bold) }
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
            // Next Turn Countdown Card
            item {
                val nextTurnMs = (schedule?.nextTurnTimestamp ?: 0L) - now
                val nextTurnStr = if (nextTurnMs <= 0) "Turning Due Now!" else {
                    val hours = TimeUnit.MILLISECONDS.toHours(nextTurnMs)
                    val mins = TimeUnit.MILLISECONDS.toMinutes(nextTurnMs) % 60
                    val secs = TimeUnit.MILLISECONDS.toSeconds(nextTurnMs) % 60
                    if (hours > 0) "${hours}h ${mins}m" else "${mins}m ${secs}s"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GoldTertiaryLight.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoldTertiaryLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = null,
                                tint = GoldTertiaryLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "NEXT SCHEDULED ROTATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = nextTurnStr,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            ),
                            color = GoldTertiaryLight
                        )

                        Text(
                            text = "Scheduled turns today: ${schedule?.dailyTurnsTarget ?: 12} times (every ${schedule?.intervalHours ?: 2}h)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Turn Buttons
                        if (batch != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.recordTurnNow(batch.id, "LEFT") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("action_turn_left"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                                ) {
                                    Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Turned Left (45°)")
                                }

                                Button(
                                    onClick = { viewModel.recordTurnNow(batch.id, "RIGHT") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("action_turn_right"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                                ) {
                                    Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Turned Right (45°)")
                                }
                            }
                        }
                    }
                }
            }

            // Turning Configuration Card
            item {
                val currentSched = schedule
                if (currentSched != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "TURNING SCHEDULER SETTINGS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Enabled Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Enable Turning Reminders", fontWeight = FontWeight.SemiBold)
                                    Text("Fires exact alarm even when phone is idle/sleeping", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = currentSched.enabled,
                                    onCheckedChange = { viewModel.updateTurningSchedule(currentSched.copy(enabled = it)) },
                                    modifier = Modifier.testTag("toggle_turning_reminders")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interval Selector
                            Text("Turning Interval", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 4, 6).forEach { hours ->
                                    FilterChip(
                                        selected = currentSched.intervalHours == hours,
                                        onClick = { viewModel.updateTurningSchedule(currentSched.copy(intervalHours = hours)) },
                                        label = { Text("${hours}h") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 24 Hours vs Daytime only
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("24-Hour Schedule", fontWeight = FontWeight.SemiBold)
                                    Text("Keep rotating at night for maximum embryo viability", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = currentSched.is24Hours,
                                    onCheckedChange = { viewModel.updateTurningSchedule(currentSched.copy(is24Hours = it)) }
                                )
                            }
                        }
                    }
                }
            }

            // Turning History Table
            item {
                Text(
                    text = "Recent Turning History (${turningLogs.size} logs)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (turningLogs.isEmpty()) {
                item {
                    Text(
                        text = "No turning events logged yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(turningLogs.take(30), key = { it.id }) { log ->
                    TurningLogCard(log = log, timeFormat = timeFormat)
                }
            }
        }
    }
}

@Composable
private fun TurningLogCard(
    log: TurningLog,
    timeFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (log.action == "COMPLETED") StatusOptimal.copy(alpha = 0.15f)
                            else GoldTertiaryLight.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (log.orientation == "LEFT") Icons.Default.RotateLeft else Icons.Default.RotateRight,
                        contentDescription = null,
                        tint = if (log.action == "COMPLETED") StatusOptimal else GoldTertiaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "Turned to ${log.orientation} (${log.action})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (log.notes.isNotBlank()) {
                        Text(
                            text = log.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = timeFormat.format(Date(log.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
