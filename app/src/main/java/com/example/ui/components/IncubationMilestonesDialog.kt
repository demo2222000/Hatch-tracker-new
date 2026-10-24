package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight

enum class MilestoneTopic(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    LOCKDOWN("Stop Rotation (Lockdown)", Icons.Default.Block, "lockdown"),
    HUMIDITY("Increase Humidity (65-70%)", Icons.Default.WaterDrop, "humidity"),
    HATCH_DAY("Hatch Day: Pip & Zip", Icons.Default.Egg, "hatch_day"),
    CANDLING("Candling & Rotten Eggs", Icons.Default.Lightbulb, "candling")
}

@Composable
fun IncubationMilestonesDialog(
    initialTopic: MilestoneTopic = MilestoneTopic.LOCKDOWN,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(initialTopic.ordinal) }
    val topics = MilestoneTopic.values()
    val currentTopic = topics[selectedIndex]

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_incubation_milestones"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberPrimaryLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AmberPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Incubation Guide & Alerts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    topics.forEachIndexed { index, topic ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            text = {
                                Text(
                                    text = topic.title.take(18) + if (topic.title.length > 18) "…" else "",
                                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (currentTopic) {
                        MilestoneTopic.LOCKDOWN -> LockdownGuideContent()
                        MilestoneTopic.HUMIDITY -> HumidityGuideContent()
                        MilestoneTopic.HATCH_DAY -> HatchDayGuideContent()
                        MilestoneTopic.CANDLING -> CandlingBiosecurityContent()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimaryLight),
                modifier = Modifier.testTag("action_close_milestones_dialog")
            ) {
                Text("Got It! I'll Follow This")
            }
        }
    )
}

@Composable
private fun LockdownGuideContent() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusCritical, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CRITICAL: CEASE ALL ROTATION AT LOCKDOWN",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusCritical
                )
                Text(
                    text = "Usually Day 18 (Chicken), Day 25 (Duck/Turkey), or Day 14 (Quail).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    GuidancePoint(
        number = "1",
        title = "Why Turning Must Stop",
        body = "In the final 3 days before hatching, the chick absorbs the remaining yolk sac and positions its head beneath the right wing, orienting its beak toward the air cell at the blunt end of the egg. Turning after lockdown disrupts this delicate positioning and can cause malpresentation, preventing the chick from pipping."
    )

    GuidancePoint(
        number = "2",
        title = "Remove Egg Turner Trays",
        body = "Take eggs out of automatic turning racks or cradles. Place them gently on their sides on the flat hatcher floor or rubberized mesh liner so newly hatched chicks cannot get their feet caught in mechanical turner tracks."
    )

    GuidancePoint(
        number = "3",
        title = "Seal the Incubator",
        body = "Do NOT open the incubator during lockdown and pip stages. Each opening releases the built-up humidity cushion and hot air, causing the inner shell membrane to dry out instantly like shrink-wrap."
    )
}

@Composable
private fun HumidityGuideContent() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TealSecondaryLight.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = TealSecondaryLight, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BOOST HUMIDITY TO 65% – 70%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TealSecondaryLight
                )
                Text(
                    text = "Days 1–17: 45–55% RH → Days 18–21: 65–70% RH",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    GuidancePoint(
        number = "1",
        title = "Preventing Membrane 'Shrink-Wrapping'",
        body = "When the chick makes its initial pip, room air enters the egg. If relative humidity is low (below 60%), the exposed inner shell membrane quickly dries into tough, leathery parchment. The chick becomes trapped, unable to rotate or push the egg cap off."
    )

    GuidancePoint(
        number = "2",
        title = "How to Increase Humidity Effectively",
        body = "Fill all internal water reservoirs or channels with warm water (not cold). Place a clean wet sponge or lint-free cotton cloth inside the base to increase evaporative surface area."
    )

    GuidancePoint(
        number = "3",
        title = "Ventilation Balance",
        body = "Never close ventilation holes 100%. Pipping chicks consume significantly more oxygen and expel high volumes of carbon dioxide. Keep at least one air vent slightly cracked."
    )
}

@Composable
private fun HatchDayGuideContent() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GoldTertiaryLight.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Egg, contentDescription = null, tint = GoldTertiaryLight, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "HATCH DAY PHASES & PATIENCE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = GoldTertiaryLight
                )
                Text(
                    text = "Normal hatch duration takes 12 to 24 hours per chick.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    GuidancePoint(
        number = "1",
        title = "Phase 1: Internal Pip",
        body = "Chick pips into the internal air cell. You may hear faint cheeping or rocking of the egg. The chick starts breathing air for the first time."
    )

    GuidancePoint(
        number = "2",
        title = "Phase 2: External Pip",
        body = "A tiny star crack appears on the outside shell. Chick rests here for 6 to 24 hours while blood vessels in the chorioallantoic membrane recede and yolk sac finishes absorbing."
    )

    GuidancePoint(
        number = "3",
        title = "Phase 3: Zipping & Emergence",
        body = "The chick pecks around the circumference in a circle ('zipping') and kicks open the shell lid. This explosive phase usually takes 30-90 minutes."
    )

    GuidancePoint(
        number = "4",
        title = "Leave Chicks in the Incubator",
        body = "Newly hatched chicks are wet and weak. Leave them in the warm incubator for 12–24 hours until fully dried and fluffed. They can live up to 72 hours solely on their absorbed yolk."
    )
}

@Composable
private fun CandlingBiosecurityContent() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StatusAlert.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAlert, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "BIOSECURITY & REMOVING SPOILED EGGS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = StatusAlert
                )
                Text(
                    text = "Discard infertiles and rotters to protect healthy embryos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    GuidancePoint(
        number = "1",
        title = "Day 7–10 Candling",
        body = "In a dark room with a bright LED flashlight: Viable eggs show spider-like red blood veins radiating from a dark eye spot. Clear eggs have no veins (infertile; remove them). Blood ring eggs show a red ring or streak (died early; remove them)."
    )

    GuidancePoint(
        number = "2",
        title = "Dangers of Rotten 'Exploder' Eggs",
        body = "Bacteria entering through shell pores will multiply in the warm incubator. A spoiled egg produces methane gas and can explode violently, coating every healthy egg with lethal pathogens."
    )

    GuidancePoint(
        number = "3",
        title = "Warning Signs of a Bad Egg",
        body = "• Weeping or sweating brown droplets on shell\n• Foul, sulfuric odor\n• Dark blotches that slosh when turned\n• Air cell moving freely when tilted\nAlways remove suspicious eggs in a plastic bag immediately!"
    )
}

@Composable
private fun GuidancePoint(
    number: String,
    title: String,
    body: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AmberPrimaryLight.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AmberPrimaryLight
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
