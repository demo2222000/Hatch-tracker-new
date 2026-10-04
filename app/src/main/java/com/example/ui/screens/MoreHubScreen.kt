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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.TealSecondaryLight
import com.example.ui.viewmodel.IncubatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    viewModel: IncubatorViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSpecies: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unreadAlerts by viewModel.unacknowledgedAlertsCount.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tools & References", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HubMenuItem(
                    icon = Icons.Default.AutoGraph,
                    iconColor = AmberPrimaryLight,
                    title = "Temperature & Humidity History",
                    description = "Detailed multi-range analytics and telemetry graphs",
                    tag = "menu_history",
                    onClick = onNavigateToHistory
                )
            }

            item {
                HubMenuItem(
                    icon = Icons.Default.Notifications,
                    iconColor = if (unreadAlerts > 0) StatusCritical else AmberPrimaryLight,
                    title = "Incubator Alerts Log",
                    description = if (unreadAlerts > 0) "$unreadAlerts unread alarms" else "All conditions normal",
                    badge = if (unreadAlerts > 0) "$unreadAlerts" else null,
                    tag = "menu_alerts",
                    onClick = onNavigateToAlerts
                )
            }

            item {
                HubMenuItem(
                    icon = Icons.Default.PictureAsPdf,
                    iconColor = TealSecondaryLight,
                    title = "Offline PDF Reports",
                    description = "Generate & share batch summary PDFs without internet",
                    tag = "menu_reports",
                    onClick = onNavigateToReports
                )
            }

            item {
                HubMenuItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    iconColor = GoldTertiaryLight,
                    title = "Species Husbandry Guide",
                    description = "Incubation times, temperatures, and tips for chicken, duck, quail...",
                    tag = "menu_species",
                    onClick = onNavigateToSpecies
                )
            }

            item {
                HubMenuItem(
                    icon = Icons.Default.Settings,
                    iconColor = MaterialTheme.colorScheme.onSurface,
                    title = "Settings & Permissions",
                    description = "°C/°F units, exact alarm permissions, and privacy info",
                    tag = "menu_settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun HubMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    badge: String? = null,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(StatusCritical)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
