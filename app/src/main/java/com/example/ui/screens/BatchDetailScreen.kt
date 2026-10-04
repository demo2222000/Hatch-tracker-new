package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncubationBatch
import com.example.data.model.SpeciesPreset
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(
    batchId: Long,
    viewModel: IncubatorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPredictor: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val batches by viewModel.allBatches.collectAsState()
    val batch = batches.find { it.id == batchId }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Dialog state for editing Start Date (e.g. backdating to Oct 2)
    var showStartDateDialog by remember { mutableStateOf(false) }

    // Dialog state for recording rotted egg removal
    var showRottedEggDialog by remember { mutableStateOf(false) }
    var rottedCountInput by remember { mutableStateOf("1") }
    var rottedNotesInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(batch?.name ?: "Batch Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToPredictor(batchId) },
                        modifier = Modifier.testTag("action_predict_hatch_rate_topbar")
                    ) {
                        Icon(Icons.Default.Biotech, contentDescription = "Predict Hatch Rate", tint = AmberPrimaryLight)
                    }

                    IconButton(
                        onClick = {
                            if (batch != null) {
                                isGeneratingPdf = true
                                viewModel.generatePdfReport(batch.id) { intent ->
                                    isGeneratingPdf = false
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Error generating local PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("export_pdf_button")
                    ) {
                        if (isGeneratingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = AmberPrimaryLight)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (batch == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Batch not found")
            }
            return@Scaffold
        }

        val currentDay = batch.calculateCurrentDay()
        val remainingDays = batch.calculateRemainingDays()
        val inLockdown = batch.isInLockdown()
        val progress = (currentDay.toFloat() / batch.incubationDays.toFloat()).coerceIn(0f, 1f)
        val preset = SpeciesPreset.getById(batch.species)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overview Progress Card
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
                                text = "DAY $currentDay OF ${batch.incubationDays}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (inLockdown) StatusCritical else AmberPrimaryLight
                            )

                            if (batch.isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(StatusOptimal.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StatusOptimal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progress },
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
                                text = if (remainingDays > 0) "$remainingDays days remaining" else "Hatch cycle complete!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Expected: ${dateFormat.format(Date(batch.expectedHatchDate))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Start / Setting Date editor & Hatch Rate Predictor button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showStartDateDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 6.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmberPrimaryLight, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Started: ${dateFormat.format(Date(batch.startDate))}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Edit, contentDescription = "Change Start Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = { onNavigateToPredictor(batch.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight.copy(alpha = 0.15f), contentColor = AmberPrimaryLight),
                                modifier = Modifier.testTag("button_predict_hatch_rate")
                            ) {
                                Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Predict Hatch %", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Hatch Results & Interactive Egg Counts Editor
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
                                text = "EGG COUNT BREAKDOWN",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Hatch: ${String.format(Locale.US, "%.1f%%", batch.hatchingPercentage)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StatusOptimal
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Counter rows
                        CounterRow(
                            label = "Hatched Eggs",
                            count = batch.hatchedEggs,
                            onIncrement = {
                                viewModel.updateBatch(batch.copy(hatchedEggs = (batch.hatchedEggs + 1).coerceAtMost(batch.totalEggs)))
                            },
                            onDecrement = {
                                viewModel.updateBatch(batch.copy(hatchedEggs = (batch.hatchedEggs - 1).coerceAtLeast(0)))
                            },
                            badgeColor = StatusOptimal
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CounterRow(
                            label = "Infertile / Clear (Candling)",
                            count = batch.infertileEggs,
                            onIncrement = {
                                viewModel.updateBatch(batch.copy(infertileEggs = batch.infertileEggs + 1))
                            },
                            onDecrement = {
                                viewModel.updateBatch(batch.copy(infertileEggs = (batch.infertileEggs - 1).coerceAtLeast(0)))
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CounterRow(
                            label = "Early Quits (Blood Ring)",
                            count = batch.earlyQuitEggs,
                            onIncrement = {
                                viewModel.updateBatch(batch.copy(earlyQuitEggs = batch.earlyQuitEggs + 1))
                            },
                            onDecrement = {
                                viewModel.updateBatch(batch.copy(earlyQuitEggs = (batch.earlyQuitEggs - 1).coerceAtLeast(0)))
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CounterRow(
                            label = "Late Quits (Dead in Shell)",
                            count = batch.lateQuitEggs,
                            onIncrement = {
                                viewModel.updateBatch(batch.copy(lateQuitEggs = batch.lateQuitEggs + 1))
                            },
                            onDecrement = {
                                viewModel.updateBatch(batch.copy(lateQuitEggs = (batch.lateQuitEggs - 1).coerceAtLeast(0)))
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        CounterRow(
                            label = "Rotted / Exploder Eggs Removed",
                            count = batch.rottedEggsRemoved,
                            onIncrement = {
                                viewModel.recordRottedEggRemoved(
                                    batchId = batch.id,
                                    count = 1,
                                    timestamp = System.currentTimeMillis()
                                )
                            },
                            onDecrement = {
                                viewModel.updateBatch(
                                    batch.copy(
                                        rottedEggsRemoved = (batch.rottedEggsRemoved - 1).coerceAtLeast(0)
                                    )
                                )
                            },
                            badgeColor = StatusAlert
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Formula: (Hatched / Initial Set) × 100 = (${batch.hatchedEggs} / ${batch.totalEggs}) × 100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Biosecurity & Rotted Egg Tracking Card (User Consistency Request)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (batch.rottedEggsRemoved > 0) StatusAlert.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (batch.rottedEggsRemoved > 0) Icons.Default.Warning else Icons.Default.CleanHands,
                                    contentDescription = null,
                                    tint = if (batch.rottedEggsRemoved > 0) StatusAlert else TealSecondaryLight
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BIOSECURITY & SPOILAGE LOG",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { showRottedEggDialog = true },
                                modifier = Modifier.testTag("action_log_rotted_egg")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Rotted Egg", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Rotted Eggs Removed: ${batch.rottedEggsRemoved}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val lastRottedTimeStr = batch.lastEggRottedTimestamp?.let {
                            dateTimeFormat.format(Date(it))
                        } ?: "None recorded"

                        Text(
                            text = "Last Egg Rotted / Removed Time: $lastRottedTimeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (batch.rottedEggsRemoved > 0) StatusAlert else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (batch.eggRottedNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes: ${batch.eggRottedNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Consistency Rule: Promptly remove spoiled or weeping eggs and wipe the tray with mild disinfectant. This prevents harmful bacteria gases from suffocating viable embryos and stabilizes air cell humidity.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Milestone Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "INCUBATION MILESTONES",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        TimelineItem(
                            day = 1,
                            title = "Eggs Set in Incubator",
                            description = "Target temp ${batch.targetTempC}°C, humidity ${batch.targetHumidityPct.toInt()}%. Rotation starts.",
                            isCompleted = currentDay >= 1,
                            isCurrent = currentDay == 1
                        )

                        TimelineItem(
                            day = preset.candlingDays.firstOrNull() ?: 7,
                            title = "First Candling Check",
                            description = "Discard clear infertile eggs. Look for spider-like blood vessels.",
                            isCompleted = currentDay >= (preset.candlingDays.firstOrNull() ?: 7),
                            isCurrent = currentDay == (preset.candlingDays.firstOrNull() ?: 7)
                        )

                        TimelineItem(
                            day = batch.lockdownDay,
                            title = "Lockdown Period (Critical)",
                            description = "STOP all turning. Increase humidity to ${batch.lockdownHumidityPct.toInt()}%. Do NOT open incubator.",
                            isCompleted = currentDay >= batch.lockdownDay,
                            isCurrent = currentDay in batch.lockdownDay until batch.incubationDays,
                            isWarning = true
                        )

                        TimelineItem(
                            day = batch.incubationDays,
                            title = "Hatch Day!",
                            description = "Chicks pip, zip, and emerge. Leave in incubator until 100% dry and fluffed.",
                            isCompleted = currentDay > batch.incubationDays,
                            isCurrent = currentDay == batch.incubationDays,
                            isLast = true
                        )
                    }
                }
            }

            // 4. Notes & Export PDF
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LOCAL EXPORT & REPORTING",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Generate a comprehensive offline PDF report including temperature, humidity, egg turning history, and candling breakdown. Saved directly to device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isGeneratingPdf = true
                                viewModel.generatePdfReport(batch.id) { intent ->
                                    isGeneratingPdf = false
                                    if (intent != null) {
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_batch_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Share Batch PDF Report")
                        }
                    }
                }
            }
        }

        // Dialog for updating batch start date (e.g. backdating to October 2nd)
        if (showStartDateDialog && batch != null) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = batch.startDate
            )
            DatePickerDialog(
                onDismissRequest = { showStartDateDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { utcMillis ->
                                val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                calUtc.timeInMillis = utcMillis
                                val localCal = Calendar.getInstance()
                                localCal.set(
                                    calUtc.get(Calendar.YEAR),
                                    calUtc.get(Calendar.MONTH),
                                    calUtc.get(Calendar.DAY_OF_MONTH),
                                    10, 0, 0
                                )
                                viewModel.updateBatch(batch.copy(startDate = localCal.timeInMillis))
                            }
                            showStartDateDialog = false
                        }
                    ) {
                        Text("Update Start Date")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showStartDateDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Dialog for logging rotted egg removal
        if (showRottedEggDialog && batch != null) {
            AlertDialog(
                onDismissRequest = { showRottedEggDialog = false },
                title = { Text("Log Spoiled / Rotted Egg Removal", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Record rotted egg removal to maintain environmental consistency and calculate accurate hatch predictions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = rottedCountInput,
                            onValueChange = { rottedCountInput = it },
                            label = { Text("Eggs Removed") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = rottedNotesInput,
                            onValueChange = { rottedNotesInput = it },
                            label = { Text("Sanitation / Reason Notes") },
                            placeholder = { Text("e.g. Weeping egg removed, tray disinfected") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = rottedCountInput.toIntOrNull() ?: 1
                            viewModel.recordRottedEggRemoved(
                                batchId = batch.id,
                                count = count,
                                timestamp = System.currentTimeMillis(),
                                notes = rottedNotesInput
                            )
                            showRottedEggDialog = false
                            rottedNotesInput = ""
                        }
                    ) {
                        Text("Record & Sanitize")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRottedEggDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun CounterRow(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    badgeColor: Color = AmberPrimaryLight,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onDecrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 14.dp),
                color = badgeColor
            )

            FilledTonalIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TimelineItem(
    day: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isWarning: Boolean = false,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> StatusOptimal
                            isCurrent -> if (isWarning) StatusCritical else AmberPrimaryLight
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        text = "D$day",
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(
                            if (isCompleted) StatusOptimal.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(bottom = if (!isLast) 12.dp else 0.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) AmberPrimaryLight else MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
