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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val now = remember { System.currentTimeMillis() }

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
            // Species Preset Selection
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

            // Batch Name & Initial Eggs
            item {
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Batch Name / ID") },
                    placeholder = { Text("e.g. Lavender Orpington #1") },
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

            // Expected Hatch Calculation Summary Card
            item {
                val expectedHatchDate = now + TimeUnit.DAYS.toMillis(incubationDays.toLong())
                val lockdownDate = now + TimeUnit.DAYS.toMillis(lockdownDay.toLong())

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = AmberPrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUTOMATIC TIMELINE SCHEDULE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Setting Date:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(now)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Lockdown (Day $lockdownDay):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(lockdownDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Expected Hatch:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateFormat.format(Date(expectedHatchDate)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AmberPrimaryLight))
                            }
                        }
                    }
                }
            }

            // Climate Thresholds
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

            // Egg Turning Interval
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

            // Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Parent stock, breed, egg source, candling observations)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
            }

            // Submit Button
            item {
                val totalEggs = totalEggsText.toIntOrNull() ?: 12
                val finalName = if (batchName.isNotBlank()) batchName else "${selectedPreset.commonName} Batch"

                Button(
                    onClick = {
                        val newBatch = IncubationBatch(
                            name = finalName,
                            species = selectedPreset.commonName,
                            startDate = now,
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
