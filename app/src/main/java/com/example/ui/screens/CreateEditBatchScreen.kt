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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncubationBatch
import com.example.data.model.SpeciesPreset
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditBatchScreen(
    viewModel: IncubatorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var batchName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(SpeciesPreset.ALL.first()) }
    var totalEggsText by remember { mutableStateOf("12") }
    var incubationDays by remember { mutableIntStateOf(selectedPreset.incubationDays) }
    var lockdownDay by remember { mutableIntStateOf(selectedPreset.lockdownDay) }
    var targetTempC by remember { mutableDoubleStateOf(selectedPreset.targetTempC) }
    var targetHumidityPct by remember { mutableDoubleStateOf(selectedPreset.targetHumidityPct) }
    var lockdownHumidityPct by remember { mutableDoubleStateOf(selectedPreset.lockdownHumidityPct) }
    var turningIntervalHours by remember { mutableIntStateOf(2) }
    var notes by remember { mutableStateOf("") }

    // Start Date Customization (Requested by user)
    val now = remember { System.currentTimeMillis() }
    var startDateMillis by remember { mutableStateOf(now) }
    var showStartDatePicker by remember { mutableStateOf(false) }

    // Rotted Egg Removal Tracking (Requested by user)
    var rottedEggsText by remember { mutableStateOf("0") }
    var lastRottedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showRottedDatePicker by remember { mutableStateOf(false) }
    var rottedNotes by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    val oct2Timestamp = remember {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 2, 10, 0, 0)
        cal.timeInMillis
    }

    val yesterdayTimestamp = remember { now - TimeUnit.DAYS.toMillis(1) }

    fun applyPreset(preset: SpeciesPreset) {
        selectedPreset = preset
        incubationDays = preset.incubationDays
        lockdownDay = preset.lockdownDay
        targetTempC = preset.targetTempC
        targetHumidityPct = preset.targetHumidityPct
        lockdownHumidityPct = preset.lockdownHumidityPct
        if (batchName.isBlank()) {
            batchName = "${preset.commonName} Batch"
        }
    }

    // Date Picker Dialog for Setting / Start Date
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
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
                            startDateMillis = localCal.timeInMillis
                        }
                        showStartDatePicker = false
                    },
                    modifier = Modifier.testTag("dialog_confirm_date_button")
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Date Picker Dialog for Last Rotted Egg Removed Date
    if (showRottedDatePicker) {
        val rottedDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = lastRottedDateMillis ?: now
        )
        DatePickerDialog(
            onDismissRequest = { showRottedDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        rottedDatePickerState.selectedDateMillis?.let { utcMillis ->
                            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                            calUtc.timeInMillis = utcMillis
                            val localCal = Calendar.getInstance()
                            localCal.set(
                                calUtc.get(Calendar.YEAR),
                                calUtc.get(Calendar.MONTH),
                                calUtc.get(Calendar.DAY_OF_MONTH),
                                12, 0, 0
                            )
                            lastRottedDateMillis = localCal.timeInMillis
                        }
                        showRottedDatePicker = false
                    }
                ) {
                    Text("Select Removal Date")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRottedDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = rottedDatePickerState)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("New Incubation Batch", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 1. Species Preset Selection
            item {
                Text(
                    text = "Select Species Preset",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SpeciesPreset.ALL) { preset ->
                        val isSelected = preset.id == selectedPreset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { applyPreset(preset) },
                            label = { Text(preset.commonName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            // 2. Batch Name & Initial Eggs
            item {
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Batch Name / ID") },
                    placeholder = { Text("e.g. Backyard Flock #1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_batch_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = totalEggsText,
                        onValueChange = { totalEggsText = it },
                        label = { Text("Initial Eggs Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_egg_count"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = "$incubationDays days",
                        onValueChange = {},
                        label = { Text("Incubation Days") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // 3. Start / Setting Date Selection (CRITICAL USER REQUEST)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = AmberPrimaryLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BATCH START / SETTING DATE",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.testTag("action_pick_custom_date")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Date", tint = AmberPrimaryLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Current selected start date display
                        Text(
                            text = dateFormat.format(Date(startDateMillis)),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clickable { showStartDatePicker = true }
                                .testTag("display_selected_start_date")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick date selector chips (including October 2nd requested by user)
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = dateFormat.format(Date(startDateMillis)) == dateFormat.format(Date(now)),
                                    onClick = { startDateMillis = now },
                                    label = { Text("Today") },
                                    modifier = Modifier.testTag("chip_date_today")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = dateFormat.format(Date(startDateMillis)) == dateFormat.format(Date(oct2Timestamp)),
                                    onClick = { startDateMillis = oct2Timestamp },
                                    label = { Text("October 2, 2026") },
                                    modifier = Modifier.testTag("chip_date_oct_2")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = dateFormat.format(Date(startDateMillis)) == dateFormat.format(Date(yesterdayTimestamp)),
                                    onClick = { startDateMillis = yesterdayTimestamp },
                                    label = { Text("Yesterday") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { showStartDatePicker = true },
                                    label = { Text("Pick Custom...") }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Expected Timeline Schedule Card (Dynamically computed from chosen start date!)
            item {
                val expectedHatchDate = startDateMillis + TimeUnit.DAYS.toMillis(incubationDays.toLong())
                val lockdownDate = startDateMillis + TimeUnit.DAYS.toMillis(lockdownDay.toLong())
                val currentDay = if (now >= startDateMillis) {
                    ((now - startDateMillis) / TimeUnit.DAYS.toMillis(1)).toInt() + 1
                } else 0
                val remainingDays = max(0, incubationDays - currentDay)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AmberPrimaryLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TIMELINE FOR THIS START DATE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (currentDay > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmberPrimaryLight.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Current: Day $currentDay",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AmberPrimaryLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Setting Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(startDateMillis)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Lockdown (Day $lockdownDay):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(lockdownDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Expected Hatch:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(expectedHatchDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AmberPrimaryLight))
                                Text("$remainingDays days left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 5. Rotted / Spoiled Egg Tracking (CRITICAL USER REQUEST: maintain consistency)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if ((rottedEggsText.toIntOrNull() ?: 0) > 0) StatusAlert.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if ((rottedEggsText.toIntOrNull() ?: 0) > 0) Icons.Default.Warning else Icons.Default.CleanHands,
                                contentDescription = null,
                                tint = if ((rottedEggsText.toIntOrNull() ?: 0) > 0) StatusAlert else TealSecondaryLight
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ROTTED / SPOILED EGG LOG (BIOSECURITY)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = rottedEggsText,
                                onValueChange = { rottedEggsText = it },
                                label = { Text("Spoiled Eggs Removed") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_rotted_eggs_count"),
                                singleLine = true
                            )

                            // Quick button to set removal date if any eggs rotted
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Last Egg Rotted Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (lastRottedDateMillis == null) lastRottedDateMillis = now
                                        showRottedDatePicker = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("action_set_rotted_date")
                                ) {
                                    Text(
                                        text = lastRottedDateMillis?.let { dateFormat.format(Date(it)) } ?: "Record Date",
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if ((rottedEggsText.toIntOrNull() ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = rottedNotes,
                                onValueChange = { rottedNotes = it },
                                label = { Text("Sanitation / Removal Notes") },
                                placeholder = { Text("e.g. Weeping egg removed on Oct 03, tray cleaned with antiseptic") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_rotted_notes"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Notice: Removing rotted eggs promptly maintains incubator humidity consistency and prevents bacterial gases from affecting healthy embryos.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 6. Climate Thresholds
            item {
                Text(
                    text = "Incubation Climate Setpoints",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = "$targetTempC°C",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Temp") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = "${targetHumidityPct.toInt()}%",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Humidity") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = "${lockdownHumidityPct.toInt()}%",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Lockdown Hum.") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7. Egg Turning Interval
            item {
                Text(
                    text = "Egg Turning Reminders Interval",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 4, 6).forEach { hours ->
                        FilterChip(
                            selected = turningIntervalHours == hours,
                            onClick = { turningIntervalHours = hours },
                            label = { Text("Every ${hours}h") }
                        )
                    }
                }
            }

            // 8. General Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("General Notes (Breed, egg source, incubator model)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    maxLines = 3
                )
            }

            // 9. Submit Button
            item {
                val totalEggs = totalEggsText.toIntOrNull() ?: 12
                val rottedCount = rottedEggsText.toIntOrNull() ?: 0
                val finalName = if (batchName.isNotBlank()) batchName else "${selectedPreset.commonName} Batch"

                Button(
                    onClick = {
                        val newBatch = IncubationBatch(
                            name = finalName,
                            species = selectedPreset.commonName,
                            startDate = startDateMillis,
                            totalEggs = totalEggs,
                            incubationDays = incubationDays,
                            lockdownDay = lockdownDay,
                            targetTempC = targetTempC,
                            minTempC = selectedPreset.minTempC,
                            maxTempC = selectedPreset.maxTempC,
                            targetHumidityPct = targetHumidityPct,
                            minHumidityPct = selectedPreset.minHumidityPct,
                            maxHumidityPct = selectedPreset.maxHumidityPct,
                            lockdownHumidityPct = lockdownHumidityPct,
                            rottedEggsRemoved = rottedCount,
                            lastEggRottedTimestamp = if (rottedCount > 0) (lastRottedDateMillis ?: now) else null,
                            eggRottedNotes = rottedNotes,
                            notes = notes,
                            isActive = true
                        )
                        viewModel.createBatch(newBatch, turningIntervalHours) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_create_batch_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight)
                ) {
                    Text("Start Incubation Batch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
