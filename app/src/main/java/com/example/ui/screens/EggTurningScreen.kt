package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TurningLog
import com.example.ui.components.IncubationMilestonesDialog
import com.example.ui.components.MilestoneTopic
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusAlert
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
fun EggTurningScreen(
    viewModel: IncubatorViewModel,
    modifier: Modifier = Modifier
) {
    val activeBatch by viewModel.activeBatch.collectAsState()
    val schedule by viewModel.turningSchedule.collectAsState()
    val turningLogs by viewModel.turningLogs.collectAsState()
    val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    var showPastTurnDialog by remember { mutableStateOf(false) }
    var showMilestonesDialog by remember { mutableStateOf(false) }
    var selectedMilestoneTopic by remember { mutableStateOf(MilestoneTopic.LOCKDOWN) }

    val now = System.currentTimeMillis()
    val batch = activeBatch
    val inLockdown = batch?.isInLockdown() == true

    // Determine current visual tilt from latest log
    val lastOrientation = turningLogs.firstOrNull()?.orientation ?: "CENTER"
    val targetRotationAngle = when (lastOrientation) {
        "LEFT" -> -32f
        "RIGHT" -> 32f
        else -> 0f
    }
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "egg_tilt_angle"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Egg Turning Management", fontWeight = FontWeight.Bold) },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            selectedMilestoneTopic = MilestoneTopic.LOCKDOWN
                            showMilestonesDialog = true
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("action_info_milestones")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Incubation Guides", fontSize = 12.sp)
                    }
                }
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
            // Lockdown Alert Banner (if in lockdown or near lockdown)
            if (inLockdown) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(StatusCritical.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LOCKDOWN ACTIVE: STOP ROTATION",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = StatusCritical
                                )
                                Text(
                                    text = "Turning must cease completely so embryos can orient toward the air cell. Increase humidity to 65-70%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = {
                                        selectedMilestoneTopic = MilestoneTopic.LOCKDOWN
                                        showMilestonesDialog = true
                                    },
                                    modifier = Modifier.testTag("action_view_lockdown_guide")
                                ) {
                                    Text("Read Lockdown & Humidity Instructions →", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Animated Egg Cradle & Countdown Card
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
                        // Interactive Animated Cradle Visualizer
                        AnimatedEggCradleVisualizer(
                            tiltAngle = animatedRotation,
                            lastOrientation = lastOrientation,
                            isDue = nextTurnMs <= 0
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (inLockdown) "ROTATION PAUSED FOR LOCKDOWN" else "NEXT SCHEDULED ROTATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = if (inLockdown) StatusCritical else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (inLockdown) "Eggs Resting on Floor" else nextTurnStr,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (inLockdown) 24.sp else 32.sp
                            ),
                            color = if (inLockdown) StatusCritical else GoldTertiaryLight
                        )

                        Text(
                            text = if (inLockdown) "Keep incubator closed until hatch is 100% complete"
                            else "Target: ${schedule?.dailyTurnsTarget ?: 12} turns/day • Interval: ${schedule?.intervalHours ?: 2}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Turn Buttons + Already Rotated Button
                        if (batch != null && !inLockdown) {
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
                                    Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Turned Left")
                                }

                                Button(
                                    onClick = { viewModel.recordTurnNow(batch.id, "RIGHT") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("action_turn_right"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Turned Right")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // "Already Rotated" Past Log Button
                            OutlinedButton(
                                onClick = { showPastTurnDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("action_already_rotated"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp), tint = AmberPrimaryLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Already Rotated? (Log Past Turn Time)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Milestones Guide Buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AmberPrimaryLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INCUBATION POP-UP GUIDANCE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                                    .testTag("guide_lockdown_button")
                            ) {
                                Text("Lockdown", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedMilestoneTopic = MilestoneTopic.HUMIDITY
                                    showMilestonesDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guide_humidity_button")
                            ) {
                                Text("Humidity", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    selectedMilestoneTopic = MilestoneTopic.HATCH_DAY
                                    showMilestonesDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("guide_hatch_day_button")
                            ) {
                                Text("Hatch Day", fontSize = 12.sp)
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                Column(modifier = Modifier.weight(1f)) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Turning History (${turningLogs.size} logs)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (turningLogs.isEmpty()) {
                item {
                    Text(
                        text = "No turning events logged yet. Tap 'Turned Left' or 'Already Rotated' to record your first rotation.",
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

    // "Already Rotated" Past Turn Dialog
    if (showPastTurnDialog && batch != null) {
        AlreadyRotatedDialog(
            onDismiss = { showPastTurnDialog = false },
            onConfirm = { pastTimestamp, orientation, notes ->
                viewModel.recordPastTurn(
                    batchId = batch.id,
                    timestamp = pastTimestamp,
                    orientation = orientation,
                    notes = notes
                )
                showPastTurnDialog = false
            }
        )
    }

    // Milestones Guidance Dialog
    if (showMilestonesDialog) {
        IncubationMilestonesDialog(
            initialTopic = selectedMilestoneTopic,
            onDismiss = { showMilestonesDialog = false }
        )
    }
}

/**
 * Animated Egg Cradle Visualizer showing a tilted incubator rack with animated eggs.
 */
@Composable
private fun AnimatedEggCradleVisualizer(
    tiltAngle: Float,
    lastOrientation: String,
    isDue: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(160.dp, 100.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background Cradle Shadow
        Canvas(modifier = Modifier.size(150.dp, 80.dp)) {
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.05f),
                topLeft = Offset(10f, 65f),
                size = Size(size.width - 20f, 12f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        // Tilting Cradle Platform
        Box(
            modifier = Modifier
                .size(140.dp, 70.dp)
                .rotate(tiltAngle),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Cradle Base Plate
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    topLeft = Offset(5f, 42f),
                    size = Size(size.width - 10f, 14f),
                    cornerRadius = CornerRadius(7f, 7f)
                )
                drawRoundRect(
                    color = Color(0xFFCBD5E1),
                    topLeft = Offset(5f, 42f),
                    size = Size(size.width - 10f, 14f),
                    cornerRadius = CornerRadius(7f, 7f),
                    style = Stroke(width = 2f)
                )

                // 3 Egg Cups
                val eggSpacing = (size.width - 40f) / 3f
                for (i in 0..2) {
                    val cx = 25f + (i * eggSpacing) + (eggSpacing / 2f)
                    // Egg shape
                    drawOval(
                        color = Color(0xFFFDE68A), // Eggshell warm ivory
                        topLeft = Offset(cx - 16f, 12f),
                        size = Size(32f, 42f)
                    )
                    // Egg outline
                    drawOval(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(cx - 16f, 12f),
                        size = Size(32f, 42f),
                        style = Stroke(width = 1.5f)
                    )
                    // Air cell subtle marker on top
                    drawArc(
                        color = Color.White.copy(alpha = 0.6f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - 10f, 14f),
                        size = Size(20f, 12f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Orientation Badge Overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isDue) StatusCritical.copy(alpha = 0.18f)
                    else AmberPrimaryLight.copy(alpha = 0.15f)
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = when (lastOrientation) {
                    "LEFT" -> "Cradle Tilted Left (-32°)"
                    "RIGHT" -> "Cradle Tilted Right (+32°)"
                    else -> "Cradle Centered (0°)"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isDue) StatusCritical else AmberPrimaryLight
            )
        }
    }
}

/**
 * Dialog to record an egg rotation that already took place in the past.
 */
@Composable
private fun AlreadyRotatedDialog(
    onDismiss: () -> Unit,
    onConfirm: (timestamp: Long, orientation: String, notes: String) -> Unit
) {
    var selectedMinutesAgo by remember { mutableIntStateOf(15) }
    var orientation by remember { mutableStateOf("LEFT") }
    var notes by remember { mutableStateOf("") }

    val presetMinutes = listOf(5, 15, 30, 60, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_already_rotated"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AmberPrimaryLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AmberPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Log Already-Rotated Turn", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Specify when the rotation happened so your incubation schedule and countdown remain 100% accurate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Direction selection
                Text("Turn Direction:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = orientation == "LEFT",
                        onClick = { orientation = "LEFT" },
                        label = { Text("Left (45°)") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = orientation == "RIGHT",
                        onClick = { orientation = "RIGHT" },
                        label = { Text("Right (45°)") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Time presets
                Text("When did you turn them?", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.forEach { mins ->
                        FilterChip(
                            selected = selectedMinutesAgo == mins,
                            onClick = { selectedMinutesAgo = mins },
                            label = { Text(if (mins < 60) "${mins}m ago" else "${mins / 60}h ago") }
                        )
                    }
                }

                // Custom minutes slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Elapsed Time:", style = MaterialTheme.typography.labelSmall)
                        Text("$selectedMinutesAgo minutes ago", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = selectedMinutesAgo.toFloat(),
                        onValueChange = { selectedMinutesAgo = it.toInt() },
                        valueRange = 1f..180f,
                        modifier = Modifier.testTag("slider_past_turn_minutes")
                    )
                }

                // Optional notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g., Manual turn before leaving)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val turnTimestamp = System.currentTimeMillis() - (selectedMinutesAgo * 60 * 1000L)
                    val noteText = notes.ifBlank { "Logged rotation ($selectedMinutesAgo mins ago)" }
                    onConfirm(turnTimestamp, orientation, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight),
                modifier = Modifier.testTag("action_confirm_past_turn")
            ) {
                Text("Record Past Rotation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                        imageVector = if (log.orientation == "LEFT") Icons.AutoMirrored.Filled.RotateLeft else Icons.AutoMirrored.Filled.RotateRight,
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
