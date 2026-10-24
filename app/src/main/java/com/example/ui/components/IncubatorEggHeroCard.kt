package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncubationBatch
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TealSecondaryLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FloatingParticle(
    val id: Long,
    val xOffset: Float,
    val yOffset: Float,
    val color: Color,
    val size: Float,
    val icon: String = "✨"
)

/**
 * An interactive, animated incubator chamber hero card displaying an egg that breathes warmth,
 * rocks subtly with life, pulses with an embryo heartbeat, and wobbles with floating sparkles when tapped.
 */
@Composable
fun IncubatorEggHeroCard(
    batch: IncubationBatch?,
    latestTempC: Double,
    latestHumidityPct: Double,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tapScale = remember { Animatable(1f) }
    val tapWobble = remember { Animatable(0f) }
    var tapCounter by remember { mutableIntStateOf(0) }
    val floatingParticles = remember { mutableStateListOf<FloatingParticle>() }

    // Natural life breathing animation for the incubator chamber
    val infiniteTransition = rememberInfiniteTransition(label = "incubator_chamber_anim")

    // Ambient heat shimmer (glow pulse)
    val heatGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heat_glow"
    )

    // Gentle life rock of the egg (rocks slightly left and right)
    val eggRockAngle by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "egg_gentle_rock"
    )

    // Embryo heartbeat pulse (fast rhythmic contraction ~75 BPM)
    val embryoHeartbeatScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "embryo_heartbeat"
    )

    // Continuous circulation fan rotation
    val circulationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "circulation_rotation"
    )

    fun handleEggTap() {
        tapCounter++
        coroutineScope.launch {
            // Spring bounce
            launch {
                tapScale.animateTo(1.15f, spring(dampingRatio = 0.4f, stiffness = 400f))
                tapScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f))
            }
            // Rapid wobble
            launch {
                tapWobble.animateTo(12f, tween(80))
                tapWobble.animateTo(-12f, tween(80))
                tapWobble.animateTo(8f, tween(70))
                tapWobble.animateTo(-6f, tween(70))
                tapWobble.animateTo(0f, spring(dampingRatio = 0.7f))
            }

            // Spawn floating hearts / sparkles
            val particleId = System.currentTimeMillis()
            val emojis = listOf("🐣", "✨", "💛", "🥚", "⭐")
            val newParticle = FloatingParticle(
                id = particleId,
                xOffset = (Random.nextFloat() - 0.5f) * 120f,
                yOffset = -30f,
                color = if (Random.nextBoolean()) AmberPrimaryLight else GoldTertiaryLight,
                size = 14f,
                icon = emojis.random()
            )
            floatingParticles.add(newParticle)
            delay(1200)
            floatingParticles.removeAll { it.id == particleId }
        }
    }

    val currentDay = batch?.calculateCurrentDay() ?: 0
    val totalDays = batch?.incubationDays ?: 21
    val inLockdown = batch?.isInLockdown() == true
    val isNearHatch = (totalDays - currentDay) <= 1 && currentDay > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("incubator_egg_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Chamber Status & Circulation Fan
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
                            .background(AmberPrimaryLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Air Circulation Active",
                            tint = AmberPrimaryLight,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(circulationAngle)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "ACTIVE INCUBATION CHAMBER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (batch != null) "${batch.name} • ${batch.species}" else "Chamber Ready • No Active Batch",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Interactive tap hint pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberPrimaryLight.copy(alpha = 0.12f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleEggTap() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Tap to check egg life",
                            tint = AmberPrimaryLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tap Egg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = AmberPrimaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Visual Stage: Incubator Chamber Canvas with Animated Egg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2A1C14),
                                Color(0xFF1E140E),
                                Color(0xFF120C08)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleEggTap() },
                contentAlignment = Alignment.Center
            ) {
                // Background Chamber Elements: Ceramic Heating Coil & Warm Radiance
                Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val w = size.width
                    val h = size.height

                    // 1. Warm Radiance aura radiating from top heaters
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmberPrimaryLight.copy(alpha = heatGlowAlpha * 0.45f),
                                GoldTertiaryLight.copy(alpha = heatGlowAlpha * 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(w / 2f, h * 0.45f),
                            radius = w * 0.42f
                        )
                    )

                    // 2. Incubator heater bars at the ceiling
                    val heaterColor = AmberPrimaryLight.copy(alpha = 0.8f + (heatGlowAlpha * 0.2f))
                    drawLine(
                        color = heaterColor,
                        start = Offset(w * 0.22f, 18.dp.toPx()),
                        end = Offset(w * 0.78f, 18.dp.toPx()),
                        strokeWidth = 3.5.dp.toPx()
                    )
                    // Glow line behind heater
                    drawLine(
                        color = GoldTertiaryLight.copy(alpha = heatGlowAlpha * 0.7f),
                        start = Offset(w * 0.24f, 18.dp.toPx()),
                        end = Offset(w * 0.76f, 18.dp.toPx()),
                        strokeWidth = 7.dp.toPx()
                    )

                    // 3. Incubator tray / cradle base at bottom
                    val trayY = h * 0.82f
                    drawRoundRect(
                        color = Color(0xFF3E2D22),
                        topLeft = Offset(w * 0.28f, trayY),
                        size = Size(w * 0.44f, 12.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    // Tray pivot point
                    drawCircle(
                        color = GoldTertiaryLight,
                        center = Offset(w / 2f, trayY + 6.dp.toPx()),
                        radius = 3.5.dp.toPx()
                    )
                }

                // Centered Interactive Animated Egg
                val combinedRotation = eggRockAngle + tapWobble.value
                val combinedScale = tapScale.value

                Box(
                    modifier = Modifier
                        .scale(combinedScale)
                        .rotate(combinedRotation)
                        .size(130.dp, 165.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp, 165.dp)) {
                        val w = size.width
                        val h = size.height

                        // Build Egg Geometry Path
                        val eggPath = Path().apply {
                            val cx = w / 2f
                            val cy = h * 0.54f
                            val rx = w * 0.38f
                            val ry = h * 0.46f

                            // Bottom rounded dome
                            moveTo(cx - rx, cy)
                            cubicTo(
                                cx - rx, cy + ry * 0.85f,
                                cx + rx, cy + ry * 0.85f,
                                cx + rx, cy
                            )
                            // Top tapered dome
                            cubicTo(
                                cx + rx, cy - ry * 0.75f,
                                cx + rx * 0.45f, cy - ry,
                                cx, cy - ry
                            )
                            cubicTo(
                                cx - rx * 0.45f, cy - ry,
                                cx - rx, cy - ry * 0.75f,
                                cx - rx, cy
                            )
                            close()
                        }

                        // Shell Base Gradient (Warm organic eggshell tone)
                        drawPath(
                            path = eggPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFAF0), // Creamy highlight
                                    Color(0xFFF3E5AB), // Warm beige
                                    Color(0xFFE4C99C), // Eggshell brown
                                    Color(0xFFB88A58)  // Shadow contour
                                ),
                                center = Offset(w * 0.4f, h * 0.38f),
                                radius = w * 0.55f
                            ),
                            style = Fill
                        )

                        // If batch is active, render the living embryo glow inside!
                        if (batch != null && currentDay >= 2) {
                            // Warm embryo candling translucency
                            val embryoCenter = Offset(w * 0.52f, h * 0.52f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF5722).copy(alpha = 0.55f * embryoHeartbeatScale),
                                        Color(0xFFFF9800).copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    center = embryoCenter,
                                    radius = w * 0.28f
                                )
                            )

                            // Heartbeat Core Dot
                            drawCircle(
                                color = Color(0xFFFF1744).copy(alpha = 0.8f),
                                center = embryoCenter,
                                radius = 5.dp.toPx() * embryoHeartbeatScale
                            )

                            // Embryo Vein Network (Subtle organic radiating lines)
                            val veinColor = Color(0xFFD32F2F).copy(alpha = 0.4f)
                            drawLine(
                                color = veinColor,
                                start = embryoCenter,
                                end = Offset(w * 0.35f, h * 0.42f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawLine(
                                color = veinColor,
                                start = embryoCenter,
                                end = Offset(w * 0.68f, h * 0.45f),
                                strokeWidth = 1.2.dp.toPx()
                            )
                            drawLine(
                                color = veinColor,
                                start = embryoCenter,
                                end = Offset(w * 0.48f, h * 0.68f),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        // Air Cell at Top (Essential for hatch orientation)
                        drawArc(
                            color = Color(0x33FFFFFF),
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(w * 0.32f, h * 0.12f),
                            size = Size(w * 0.36f, h * 0.15f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Pip crack indicator for Lockdown / Hatch day
                        if (inLockdown || isNearHatch) {
                            val crackColor = Color(0xFF4E342E)
                            val crackPath = Path().apply {
                                moveTo(w * 0.55f, h * 0.36f)
                                lineTo(w * 0.58f, h * 0.39f)
                                lineTo(w * 0.56f, h * 0.42f)
                                lineTo(w * 0.62f, h * 0.44f)
                            }
                            drawPath(
                                path = crackPath,
                                color = crackColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Egg Shell Outline Contour
                        drawPath(
                            path = eggPath,
                            color = Color(0xFF8D6E63).copy(alpha = 0.6f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Floating sparkle particles when egg is tapped
                floatingParticles.forEach { particle ->
                    Text(
                        text = particle.icon,
                        fontSize = particle.size.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(
                                start = (particle.xOffset * 1.5f).dp,
                                bottom = (particle.yOffset * 1.5f).dp
                            )
                    )
                }

                // Bottom Overlay: Live Life Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (batch != null) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Embryo Heartbeat",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier
                                    .size(13.dp)
                                    .scale(embryoHeartbeatScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (inLockdown) "Lockdown Mode • Pip Expected" else "Day $currentDay • Embryo Heartbeat Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Incubator Standby • Start Batch to Incubate",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-metrics row with animated environmental badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Temperature telemetry pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = AmberPrimaryLight.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AmberPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "WARMTH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", latestTempC)}°C",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Humidity telemetry pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = TealSecondaryLight.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = TealSecondaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "HUMIDITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${latestHumidityPct.toInt()}% RH",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Progress Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = GoldTertiaryLight.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (inLockdown) Icons.Default.LockClock else Icons.Default.Egg,
                            contentDescription = null,
                            tint = GoldTertiaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "INCUBATION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val pct = if (totalDays > 0) ((currentDay.toFloat() / totalDays.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                            Text(
                                text = "$pct% Done",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
