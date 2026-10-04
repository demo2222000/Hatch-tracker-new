package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatchRatePredictorScreen(
    viewModel: IncubatorViewModel,
    batchId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBatch by viewModel.activeBatch.collectAsState()
    val allBatches by viewModel.allBatches.collectAsState()

    // Form inputs state
    var selectedPreset by remember { mutableStateOf(SpeciesPreset.ALL.first()) }
    var totalEggs by remember { mutableIntStateOf(20) }
    var targetTempC by remember { mutableDoubleStateOf(37.5) }
    var incubationHumPct by remember { mutableDoubleStateOf(50.0) }
    var lockdownHumPct by remember { mutableDoubleStateOf(65.0) }
    var turnsPerDay by remember { mutableIntStateOf(5) }
    var eggStorageDays by remember { mutableIntStateOf(3) }
    var tempStability by remember { mutableStateOf("STABLE") } // STABLE, MINOR_SPIKES, LARGE_SWINGS

    // Candling & Spoilage Inputs
    var infertileCount by remember { mutableIntStateOf(2) }
    var earlyQuitCount by remember { mutableIntStateOf(1) }
    var rottedRemovedCount by remember { mutableIntStateOf(0) }
    var lastRottedTiming by remember { mutableStateOf("NONE") } // NONE, RECENT_24H, FEW_DAYS, OVER_WEEK
    var disinfectedAfterRotted by remember { mutableStateOf(true) }

    // Auto-populate if batch provided or requested
    fun populateFromBatch(batch: IncubationBatch) {
        val preset = SpeciesPreset.getById(batch.species)
        selectedPreset = preset
        totalEggs = max(1, batch.totalEggs)
        targetTempC = batch.targetTempC
        incubationHumPct = batch.targetHumidityPct
        lockdownHumPct = batch.lockdownHumidityPct
        infertileCount = batch.infertileEggs
        earlyQuitCount = batch.earlyQuitEggs
        rottedRemovedCount = batch.rottedEggsRemoved
        if (batch.rottedEggsRemoved > 0) {
            val lastTime = batch.lastEggRottedTimestamp
            if (lastTime != null) {
                val diffDays = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60 * 24)
                lastRottedTiming = when {
                    diffDays < 1 -> "RECENT_24H"
                    diffDays <= 4 -> "FEW_DAYS"
                    else -> "OVER_WEEK"
                }
            } else {
                lastRottedTiming = "FEW_DAYS"
            }
        }
    }

    LaunchedEffect(batchId, allBatches) {
        if (batchId != null) {
            val target = allBatches.find { it.id == batchId }
            if (target != null) populateFromBatch(target)
        } else if (activeBatch != null && rottedRemovedCount == 0 && totalEggs == 20) {
            activeBatch?.let { populateFromBatch(it) }
        }
    }

    // Mathematical Hatch Rate Prediction Engine
    val calculation = remember(
        selectedPreset, totalEggs, targetTempC, incubationHumPct, lockdownHumPct,
        turnsPerDay, eggStorageDays, tempStability, infertileCount, earlyQuitCount,
        rottedRemovedCount, lastRottedTiming, disinfectedAfterRotted
    ) {
        calculatePredictedHatch(
            preset = selectedPreset,
            totalEggs = totalEggs,
            tempC = targetTempC,
            incubationHum = incubationHumPct,
            lockdownHum = lockdownHumPct,
            turnsPerDay = turnsPerDay,
            storageDays = eggStorageDays,
            stability = tempStability,
            infertile = infertileCount,
            earlyQuits = earlyQuitCount,
            rottedRemoved = rottedRemovedCount,
            rottedTiming = lastRottedTiming,
            disinfected = disinfectedAfterRotted
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Hatch Rate Predictor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    activeBatch?.let { b ->
                        IconButton(
                            onClick = { populateFromBatch(b) },
                            modifier = Modifier.testTag("action_load_active_batch")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Load from active batch", tint = AmberPrimaryLight)
                        }
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
            // 1. Prediction Score Hero Card
            item {
                PredictionHeroCard(calculation = calculation, totalEggs = totalEggs)
            }

            // 2. Species Preset Picker
            item {
                Text(
                    text = "1. Species & Baseline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SpeciesPreset.ALL) { preset ->
                        val isSelected = preset.id == selectedPreset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                targetTempC = preset.targetTempC
                                incubationHumPct = preset.targetHumidityPct
                                lockdownHumPct = preset.lockdownHumidityPct
                                turnsPerDay = preset.recommendedTurnsPerDay
                            },
                            label = { Text(preset.commonName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }

            // 3. Egg Counts & Candling Checks
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. Egg Setting & Candling Breakdown",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = totalEggs.toString(),
                                onValueChange = { totalEggs = it.toIntOrNull()?.coerceAtLeast(1) ?: 1 },
                                label = { Text("Initial Set Eggs") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_predict_total_eggs")
                            )

                            OutlinedTextField(
                                value = infertileCount.toString(),
                                onValueChange = { infertileCount = it.toIntOrNull()?.coerceIn(0, totalEggs) ?: 0 },
                                label = { Text("Clear / Infertile") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_predict_infertile")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = earlyQuitCount.toString(),
                                onValueChange = { earlyQuitCount = it.toIntOrNull()?.coerceIn(0, totalEggs) ?: 0 },
                                label = { Text("Blood Rings / Quits") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = rottedRemovedCount.toString(),
                                onValueChange = { rottedRemovedCount = it.toIntOrNull()?.coerceIn(0, totalEggs) ?: 0 },
                                label = { Text("Rotted / Weeping") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_predict_rotted_eggs")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Fertility Rate: ${calculation.fertilityPct.roundToInt()}% (${totalEggs - infertileCount}/$totalEggs fertile eggs)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Rotted Egg Consistency & Contamination Tracking (Requested by user)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rottedRemovedCount > 0) StatusAlert.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rottedRemovedCount > 0) Icons.Default.Warning else Icons.Default.CleanHands,
                                contentDescription = null,
                                tint = if (rottedRemovedCount > 0) StatusAlert else StatusOptimal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Spoilage & Rotted Egg Consistency",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "When was the last spoiled/rotted egg removed?",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "NONE" to "None",
                                "RECENT_24H" to "<24h Ago",
                                "FEW_DAYS" to "2-4 Days",
                                "OVER_WEEK" to "1+ Wk Ago"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = lastRottedTiming == key,
                                    onClick = { lastRottedTiming = key },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("chip_rotted_timing_$key")
                                )
                            }
                        }

                        if (lastRottedTiming != "NONE" || rottedRemovedCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Incubator Sanitized After Removal",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Wiped egg tray with disinfectant to kill explosive bacteria (Proteus/Pseudomonas)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = disinfectedAfterRotted,
                                    onCheckedChange = { disinfectedAfterRotted = it },
                                    modifier = Modifier.testTag("switch_disinfected_tray")
                                )
                            }
                        }
                    }
                }
            }

            // 5. Environmental Conditions (Temp, Humidity, Stability)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = AmberPrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "4. Climate & Turning Factors",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Temperature Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Operating Temperature", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = String.format("%.1f°C (%.1f°F)", targetTempC, (targetTempC * 9 / 5) + 32),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (abs(targetTempC - selectedPreset.targetTempC) <= 0.3) StatusOptimal else StatusAlert
                                )
                            )
                        }
                        Slider(
                            value = targetTempC.toFloat(),
                            onValueChange = { targetTempC = (it * 10).roundToInt() / 10.0 },
                            valueRange = 35.0f..40.0f,
                            steps = 49,
                            modifier = Modifier.testTag("slider_temp")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Humidity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Incubation Humidity (Day 1 - Lockdown)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${incubationHumPct.roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TealSecondaryLight)
                            )
                        }
                        Slider(
                            value = incubationHumPct.toFloat(),
                            onValueChange = { incubationHumPct = it.toDouble() },
                            valueRange = 30f..75f,
                            modifier = Modifier.testTag("slider_humidity")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Lockdown Humidity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lockdown Humidity (Final 3 Days)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${lockdownHumPct.roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TealSecondaryLight)
                            )
                        }
                        Slider(
                            value = lockdownHumPct.toFloat(),
                            onValueChange = { lockdownHumPct = it.toDouble() },
                            valueRange = 45f..85f
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Temperature Stability
                        Text(text = "Incubator Temperature Stability", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "STABLE" to "Stable (±0.2°C)",
                                "MINOR_SPIKES" to "Spikes (±0.8°C)",
                                "LARGE_SWINGS" to "Swings (±1.5°C+)"
                            ).forEach { (mode, label) ->
                                FilterChip(
                                    selected = tempStability == mode,
                                    onClick = { tempStability = mode },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Turning Frequency
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Turning Frequency", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = if (selectedPreset.requiresTurning) "$turnsPerDay turns / day" else "No turn needed",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        if (selectedPreset.requiresTurning) {
                            Slider(
                                value = turnsPerDay.toFloat(),
                                onValueChange = { turnsPerDay = it.roundToInt() },
                                valueRange = 0f..12f,
                                steps = 11
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Pre-Incubation Storage Days
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Egg Age Before Setting", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "$eggStorageDays days stored",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Slider(
                            value = eggStorageDays.toFloat(),
                            onValueChange = { eggStorageDays = it.roundToInt() },
                            valueRange = 0f..20f,
                            steps = 19
                        )
                    }
                }
            }

            // 6. Detailed Factor Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Factor Impact Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FactorScoreRow("Temperature Accuracy", calculation.tempScore, calculation.tempFeedback)
                        Spacer(modifier = Modifier.height(10.dp))
                        FactorScoreRow("Humidity Optimization", calculation.humidityScore, calculation.humidityFeedback)
                        Spacer(modifier = Modifier.height(10.dp))
                        FactorScoreRow("Turning Regularity", calculation.turningScore, calculation.turningFeedback)
                        Spacer(modifier = Modifier.height(10.dp))
                        FactorScoreRow("Pre-Incubation Freshness", calculation.storageScore, calculation.storageFeedback)
                        Spacer(modifier = Modifier.height(10.dp))
                        FactorScoreRow("Biosecurity & Spoilage Impact", calculation.spoilageScore, calculation.spoilageFeedback)
                    }
                }
            }

            // 7. Expert Optimization Recommendations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberPrimaryLight.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberPrimaryLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Actionable Husbandry Advice",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AmberPrimaryLight)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        calculation.recommendations.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("• ", fontWeight = FontWeight.Bold, color = AmberPrimaryLight)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionHeroCard(
    calculation: PredictionResult,
    totalEggs: Int
) {
    val scoreColor = when {
        calculation.overallHatchPct >= 80.0 -> StatusOptimal
        calculation.overallHatchPct >= 65.0 -> GoldTertiaryLight
        calculation.overallHatchPct >= 45.0 -> StatusWarning
        else -> StatusAlert
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ESTIMATED HATCH RATE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage Big Display
            Text(
                text = "${calculation.overallHatchPct.roundToInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 58.sp
                ),
                color = scoreColor,
                modifier = Modifier.testTag("output_predicted_hatch_pct")
            )

            Text(
                text = calculation.ratingLabel,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = scoreColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estimated Chicks: ${calculation.minChicks} – ${calculation.maxChicks} chicks (out of $totalEggs eggs)",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fertile Hatch Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${calculation.fertileHatchPct.roundToInt()}%",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Viable Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${calculation.viableEggs}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spoilage Risk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        calculation.riskLevel,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = scoreColor)
                    )
                }
            }
        }
    }
}

@Composable
fun FactorScoreRow(
    title: String,
    scorePct: Int,
    feedback: String
) {
    val barColor = when {
        scorePct >= 85 -> StatusOptimal
        scorePct >= 70 -> GoldTertiaryLight
        scorePct >= 50 -> StatusWarning
        else -> StatusAlert
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text("$scorePct%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = barColor))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (scorePct / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(feedback, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class PredictionResult(
    val overallHatchPct: Double,
    val fertileHatchPct: Double,
    val viableEggs: Int,
    val fertilityPct: Double,
    val minChicks: Int,
    val maxChicks: Int,
    val ratingLabel: String,
    val riskLevel: String,
    val tempScore: Int,
    val tempFeedback: String,
    val humidityScore: Int,
    val humidityFeedback: String,
    val turningScore: Int,
    val turningFeedback: String,
    val storageScore: Int,
    val storageFeedback: String,
    val spoilageScore: Int,
    val spoilageFeedback: String,
    val recommendations: List<String>
)

fun calculatePredictedHatch(
    preset: SpeciesPreset,
    totalEggs: Int,
    tempC: Double,
    incubationHum: Double,
    lockdownHum: Double,
    turnsPerDay: Int,
    storageDays: Int,
    stability: String,
    infertile: Int,
    earlyQuits: Int,
    rottedRemoved: Int,
    rottedTiming: String,
    disinfected: Boolean
): PredictionResult {
    val safeTotal = max(1, totalEggs)
    val fertileCount = max(0, safeTotal - infertile)
    val fertilityPct = (fertileCount.toDouble() / safeTotal.toDouble()) * 100.0

    // 1. Temperature Multiplier
    val tempDiff = abs(tempC - preset.targetTempC)
    val tempMult: Double
    val tempScore: Int
    val tempFeedback: String
    when {
        tempDiff <= 0.2 -> {
            tempMult = 1.0
            tempScore = 98
            tempFeedback = "Optimal incubator temperature"
        }
        tempDiff <= 0.5 -> {
            tempMult = 0.92
            tempScore = 85
            tempFeedback = "Minor temperature drift (within tolerable boundary)"
        }
        tempDiff <= 1.0 -> {
            tempMult = 0.72
            tempScore = 65
            tempFeedback = "Significant drift: slows metabolism or overheats embryos"
        }
        tempDiff <= 1.5 -> {
            tempMult = 0.45
            tempScore = 40
            tempFeedback = "Severe thermal stress. High risk of dead-in-shell"
        }
        else -> {
            tempMult = 0.15
            tempScore = 15
            tempFeedback = "Critical thermal danger. Lethal to most embryos"
        }
    }

    // 2. Humidity Multiplier
    val humDiffEarly = abs(incubationHum - preset.targetHumidityPct)
    val humDiffLate = abs(lockdownHum - preset.lockdownHumidityPct)
    val humScore: Int
    val humMult: Double
    val humFeedback: String
    val avgHumDiff = (humDiffEarly * 0.6) + (humDiffLate * 0.4)
    when {
        avgHumDiff <= 3.0 -> {
            humMult = 1.0
            humScore = 96
            humFeedback = "Ideal humidity for correct air-cell development"
        }
        avgHumDiff <= 7.0 -> {
            humMult = 0.93
            humScore = 84
            humFeedback = "Acceptable humidity control"
        }
        avgHumDiff <= 14.0 -> {
            humMult = 0.78
            humScore = 64
            humFeedback = "Improper moisture loss; risk of chick stickiness or drowning"
        }
        else -> {
            humMult = 0.50
            humScore = 35
            humFeedback = "Extreme humidity mismatch; risk of shrink-wrapped chicks"
        }
    }

    // 3. Turning Multiplier
    val turningMult: Double
    val turningScore: Int
    val turningFeedback: String
    if (!preset.requiresTurning) {
        turningMult = 1.0
        turningScore = 100
        turningFeedback = "Species requires zero turning (reptile mode)"
    } else {
        when {
            turnsPerDay >= 4 -> {
                turningMult = 1.0
                turningScore = 98
                turningFeedback = "Frequent turning prevents embryo adhesion to membrane"
            }
            turnsPerDay >= 3 -> {
                turningMult = 0.90
                turningScore = 80
                turningFeedback = "Minimal turning met (3x/day)"
            }
            turnsPerDay in 1..2 -> {
                turningMult = 0.65
                turningScore = 50
                turningFeedback = "Insufficient turning: embryos risk adhering to shell"
            }
            else -> {
                turningMult = 0.35
                turningScore = 20
                turningFeedback = "No turning! Heavy embryo mortality expected"
            }
        }
    }

    // 4. Pre-incubation Storage Age Multiplier
    val storageMult: Double
    val storageScore: Int
    val storageFeedback: String
    when {
        storageDays <= 4 -> {
            storageMult = 1.0
            storageScore = 98
            storageFeedback = "Fresh eggs (under 4 days old). Peak viability"
        }
        storageDays <= 7 -> {
            storageMult = 0.94
            storageScore = 88
            storageFeedback = "Normal storage age (5-7 days). Minor viability drop"
        }
        storageDays <= 11 -> {
            storageMult = 0.78
            storageScore = 68
            storageFeedback = "Stored 8-11 days: albumen degrades and hatch delays 0.5h/day"
        }
        storageDays <= 14 -> {
            storageMult = 0.58
            storageScore = 48
            storageFeedback = "Stored 12-14 days: significant cellular degeneration"
        }
        else -> {
            storageMult = 0.30
            storageScore = 25
            storageFeedback = "Old eggs (>14 days): blastoderm viability severely degraded"
        }
    }

    // 5. Spoilage & Rotted Egg Factor (Crucial feature requested)
    val spoilageMult: Double
    val spoilageScore: Int
    val spoilageFeedback: String
    var penaltyPercent = 0.0

    if (rottedRemoved > 0) {
        penaltyPercent += rottedRemoved * 2.5
    }

    when (rottedTiming) {
        "RECENT_24H" -> penaltyPercent += if (disinfected) 4.0 else 12.0
        "FEW_DAYS" -> penaltyPercent += if (disinfected) 2.0 else 8.0
        "OVER_WEEK" -> penaltyPercent += if (disinfected) 1.0 else 4.0
        else -> { /* No recent rotted timing */ }
    }

    if (!disinfected && rottedRemoved > 0) {
        penaltyPercent += 5.0
    }

    spoilageMult = (1.0 - (penaltyPercent / 100.0)).coerceIn(0.40, 1.0)
    spoilageScore = ((spoilageMult) * 100).roundToInt()
    spoilageFeedback = when {
        rottedRemoved == 0 && rottedTiming == "NONE" -> "Zero rotted eggs recorded; pristine incubator biosecurity"
        disinfected -> "$rottedRemoved rotted egg(s) removed; sanitized tray prevents bacterial contamination"
        else -> "WARNING: $rottedRemoved rotted egg(s) removed WITHOUT sanitization. Bacterial spread risk!"
    }

    // 6. Stability Factor
    val stabilityMult = when (stability) {
        "STABLE" -> 1.0
        "MINOR_SPIKES" -> 0.92
        else -> 0.75
    }

    // Combined Fertile Hatchability
    val fertileHatchPct = (100.0 * tempMult * humMult * turningMult * storageMult * spoilageMult * stabilityMult)
        .coerceIn(5.0, 96.0)

    // Viable eggs count deducting candling clears, quits and rotted
    val viableEggs = max(0, safeTotal - infertile - earlyQuits - rottedRemoved)

    // Overall Hatch Rate = (Fertile Hatch % * (Viable / Total))
    val overallHatchPct = (fertileHatchPct * (viableEggs.toDouble() / safeTotal.toDouble()))
        .coerceIn(0.0, 95.0)

    val expectedChicks = (safeTotal * (overallHatchPct / 100.0))
    val minChicks = max(0, (expectedChicks - 1.2).roundToInt())
    val maxChicks = min(safeTotal, (expectedChicks + 1.2).roundToInt())

    val ratingLabel = when {
        overallHatchPct >= 80.0 -> "Excellent Potential (A)"
        overallHatchPct >= 68.0 -> "Good Hatchability (B)"
        overallHatchPct >= 50.0 -> "Moderate / Needs Care (C)"
        else -> "High Risk / Critical Swings (D)"
    }

    val riskLevel = when {
        spoilageScore >= 85 && tempScore >= 80 -> "Low"
        spoilageScore >= 70 && tempScore >= 60 -> "Moderate"
        else -> "Elevated"
    }

    // Actionable Recommendations
    val tips = mutableListOf<String>()

    if (rottedRemoved > 0) {
        if (!disinfected) {
            tips.add("URGENT: Disinfect the incubator base and egg tray immediately to stop airborne bacteria (Pseudomonas) from penetrating neighboring shells.")
        } else {
            tips.add("Candle eggs adjacent to the removed rotted egg within 48 hours to ensure no bacterial weeping has spread.")
        }
    }

    if (tempDiff > 0.3) {
        if (tempC < preset.targetTempC) {
            tips.add("Raise temperature slightly toward ${preset.targetTempC}°C. Sub-optimal temperature delays pipping by several hours.")
        } else {
            tips.add("Lower temperature immediately toward ${preset.targetTempC}°C. Embryos are extremely vulnerable to heat spikes over 38.3°C.")
        }
    }

    if (humDiffLate > 5.0) {
        tips.add("On Lockdown (Day ${preset.lockdownDay}), maintain humidity around ${preset.lockdownHumidityPct.toInt()}% so chick membranes stay soft and pliable.")
    }

    if (turnsPerDay < 3 && preset.requiresTurning) {
        tips.add("Increase egg rotation to at least 4-5 times daily until lockdown to prevent embryo adhesion.")
    }

    if (storageDays > 7) {
        tips.add("Pre-stored for $storageDays days: add +6 to +12 hours to your expected hatch time calculation.")
    }

    if (infertile > (safeTotal * 0.25)) {
        tips.add("High infertile rate (${((infertile.toDouble()/safeTotal)*100).roundToInt()}%): review rooster/male ratio and breeder flock nutrition.")
    }

    if (tips.isEmpty()) {
        tips.add("All conditions are optimal! Maintain steady temperature and prepare lockdown on Day ${preset.lockdownDay}.")
    }

    return PredictionResult(
        overallHatchPct = overallHatchPct,
        fertileHatchPct = fertileHatchPct,
        viableEggs = viableEggs,
        fertilityPct = fertilityPct,
        minChicks = minChicks,
        maxChicks = maxChicks,
        ratingLabel = ratingLabel,
        riskLevel = riskLevel,
        tempScore = tempScore,
        tempFeedback = tempFeedback,
        humidityScore = humScore,
        humidityFeedback = humFeedback,
        turningScore = turningScore,
        turningFeedback = turningFeedback,
        storageScore = storageScore,
        storageFeedback = storageFeedback,
        spoilageScore = spoilageScore,
        spoilageFeedback = spoilageFeedback,
        recommendations = tips
    )
}
