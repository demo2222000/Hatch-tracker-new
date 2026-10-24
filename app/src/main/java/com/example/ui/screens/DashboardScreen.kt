package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpeciesPreset
import com.example.ui.components.ClimateGaugeCard
import com.example.ui.components.IncubationMilestonesDialog
import com.example.ui.components.MilestoneTopic
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: IncubatorViewModel,
    onNavigateToBatches: () -> Unit,
    onNavigateToCreateBatch: () -> Unit,
    onNavigateToTurning: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSensor: () -> Unit,
    onNavigateToBatchDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBatch by viewModel.activeBatch.collectAsState()
    val sensorState by viewModel.sensorState.collectAsState()
    val latestReading by viewModel.latestReading.collectAsState()
    val recentReadings by viewModel.sensorReadings.collectAsState()
    val turningSchedule by viewModel.turningSchedule.collectAsState()
    val unreadAlerts by viewModel.unacknowledgedAlertsCount.collectAsState()
    val isFahrenheit by viewModel.isFahrenheit.collectAsState()

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    var showMilestonesDialog by remember { mutableStateOf(false) }
    var selectedMilestoneTopic by remember { mutableStateOf(MilestoneTopic.LOCKDOWN) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Egg,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HatchMaster",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Offline Incubator Hub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAlerts,
                        modifier = Modifier.testTag("action_alerts_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            if (unreadAlerts > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(StatusCritical),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadAlerts",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // 1. Live Climate Telemetry Gauge Card
            item {
                val targetTemp = activeBatch?.targetTempC ?: 37.5
                val minTemp = activeBatch?.minTempC ?: 37.0
                val maxTemp = activeBatch?.maxTempC ?: 38.3
                val targetHum = activeBatch?.targetHumidityPct ?: 50.0
                val minHum = activeBatch?.minHumidityPct ?: 45.0
                val maxHum = activeBatch?.maxHumidityPct ?: 55.0

                ClimateGaugeCard(
                    sensorState = sensorState,
                    latestReading = latestReading,
                    recentReadings = recentReadings,
                    targetTempC = targetTemp,
                    minTempC = minTemp,
                    maxTempC = maxTemp,
                    targetHumidityPct = targetHum,
                    minHumidityPct = minHum,
                    maxHumidityPct = maxHum,
                    isFahrenheit = isFahrenheit,
                    formatTemp = { viewModel.formatTemperature(it) },
                    modifier = Modifier.clickable { onNavigateToSensor() }
                )
            }

            // 2. Active Incubation Batch Card
            item {
                val batch = activeBatch
                if (batch != null) {
                    val currentDay = batch.calculateCurrentDay()
                    val remainingDays = batch.calculateRemainingDays()
                    val progress = (currentDay.toFloat() / batch.incubationDays.toFloat()).coerceIn(0f, 1f)
                    val inLockdown = batch.isInLockdown()

                    // Encouraging Pulse Animation on Active Incubation
                    val infinitePulse = rememberInfiniteTransition(label = "batch_pulse")
                    val pulseScale by infinitePulse.animateFloat(
                        initialValue = 0.97f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1300, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    // Smooth animated progress
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        label = "progress_bar_anim"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBatchDetail(batch.id) }
                            .testTag("active_batch_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = batch.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${batch.species} • Started ${dateFormat.format(Date(batch.startDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .scale(if (inLockdown) pulseScale else 1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (inLockdown) StatusCritical.copy(alpha = 0.15f)
                                            else AmberPrimaryLight.copy(alpha = 0.15f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (inLockdown) "LOCKDOWN ACTIVE" else "DAY $currentDay OF ${batch.incubationDays}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (inLockdown) StatusCritical else AmberPrimaryLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Smooth Animated Linear Progress bar
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (inLockdown) StatusCritical else AmberPrimaryLight,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$remainingDays days until hatch",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Expected: ${dateFormat.format(Date(batch.expectedHatchDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (inLockdown) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(StatusCritical.copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LockClock,
                                                contentDescription = null,
                                                tint = StatusCritical,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Lockdown active: Stop egg rotation. Maintain humidity around ${batch.lockdownHumidityPct.toInt()}% for hatching.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StatusCritical
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = {
                                                selectedMilestoneTopic = MilestoneTopic.LOCKDOWN
                                                showMilestonesDialog = true
                                            },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Open Lockdown Guide →", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusCritical)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty Batch Call to Action
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Egg,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AmberPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Active Incubation Batch",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Create a new batch to track hatch days, schedule turning alarms, and calculate hatching percentages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToCreateBatch,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Set New Batch")
                            }
                        }
                    }
                }
            }

            // 3. Egg Counts & Hatch Percentage Breakdown
            item {
                val batch = activeBatch
                if (batch != null) {
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
                                    text = "EGG COUNTS & RESULTS",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Rate: ${String.format(Locale.US, "%.1f%%", batch.hatchingPercentage)}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (batch.hatchedEggs > 0) StatusOptimal else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatBox(label = "Total Set", count = batch.totalEggs, color = MaterialTheme.colorScheme.onSurface)
                                StatBox(label = "Viable", count = batch.estimatedViableEggs, color = TealSecondaryLight)
                                StatBox(label = "Hatched", count = batch.hatchedEggs, color = StatusOptimal)
                                StatBox(label = "Unhatched", count = batch.unhatchedEggs, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 4. Egg-Turning Quick Action & Status
            item {
                val batch = activeBatch
                if (batch != null && !batch.isInLockdown()) {
                    val schedule = turningSchedule
                    val now = System.currentTimeMillis()
                    val nextTurnMs = (schedule?.nextTurnTimestamp ?: 0L) - now
                    val nextTurnStr = if (nextTurnMs <= 0) "Due Now!" else {
                        val hours = TimeUnit.MILLISECONDS.toHours(nextTurnMs)
                        val mins = TimeUnit.MILLISECONDS.toMinutes(nextTurnMs) % 60
                        if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToTurning() },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                        contentDescription = null,
                                        tint = GoldTertiaryLight,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "EGG TURNING SCHEDULE",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(GoldTertiaryLight.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "NEXT: $nextTurnStr",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = GoldTertiaryLight
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { viewModel.recordTurnNow(batch.id, "LEFT") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_turn_left_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Turned Left")
                                }

                                Button(
                                    onClick = { viewModel.recordTurnNow(batch.id, "RIGHT") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("quick_turn_right_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Turned Right")
                                }
                            }
                        }
                    }
                }
            }

            // 5. Incubation Pop-up Guidance Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberPrimaryLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INCUBATION POP-UP GUIDELINES",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    selectedMilestoneTopic = MilestoneTopic.LOCKDOWN
                                    showMilestonesDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_guide_lockdown")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Stop Turning", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedMilestoneTopic = MilestoneTopic.HUMIDITY
                                    showMilestonesDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_guide_humidity")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Humidity 70%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedMilestoneTopic = MilestoneTopic.HATCH_DAY
                                    showMilestonesDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_guide_hatch_day")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Egg, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Hatch Day", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6. Beginner Guidance / Species Tip
            item {
                val batch = activeBatch
                val preset = SpeciesPreset.getById(batch?.species ?: "chicken")
                val currentDay = batch?.calculateCurrentDay() ?: 1

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TealSecondaryLight.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealSecondaryLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = TealSecondaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "${preset.commonName} Husbandry Guidance (Day $currentDay)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = TealSecondaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = preset.tips,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Incubation Milestones Guidance Dialog
        if (showMilestonesDialog) {
            IncubationMilestonesDialog(
                initialTopic = selectedMilestoneTopic,
                onDismiss = { showMilestonesDialog = false }
            )
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
