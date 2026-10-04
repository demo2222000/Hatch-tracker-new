package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.BatchDetailScreen
import com.example.ui.screens.BatchesScreen
import com.example.ui.screens.CreateEditBatchScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EggTurningScreen
import com.example.ui.screens.HatchRatePredictorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SensorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeciesGuideScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IncubatorViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Batches : Screen("batches", "Batches", Icons.Default.Egg)
    object Turning : Screen("turning", "Turning", Icons.AutoMirrored.Filled.RotateRight)
    object Sensor : Screen("sensor", "Sensor", Icons.Default.Sensors)
    object More : Screen("more", "More", Icons.Default.GridView)

    // Sub-screens
    object CreateBatch : Screen("create_batch", "New Batch", null)
    object BatchDetail : Screen("batch_detail/{batchId}", "Batch Details", null) {
        fun createRoute(batchId: Long) = "batch_detail/$batchId"
    }
    object History : Screen("history", "History", null)
    object Alerts : Screen("alerts", "Alerts", null)
    object Reports : Screen("reports", "Reports", null)
    object SpeciesGuide : Screen("species_guide", "Guide", null)
    object Settings : Screen("settings", "Settings", null)
    object PredictHatchRate : Screen("predict_hatch_rate?batchId={batchId}", "Predict Hatch Rate", Icons.Default.Biotech) {
        fun createRoute(batchId: Long? = null) = if (batchId != null) "predict_hatch_rate?batchId=$batchId" else "predict_hatch_rate"
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: IncubatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val navigateToTarget = intent?.getStringExtra("navigate_to")

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Check and request notification permissions on Android 13+
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Handle deep link / notification extra
                    if (!navigateToTarget.isNullOrBlank()) {
                        when (navigateToTarget) {
                            "turning" -> navController.navigate(Screen.Turning.route)
                            "sensor" -> navController.navigate(Screen.Sensor.route)
                            "batches" -> navController.navigate(Screen.Batches.route)
                        }
                    }
                }

                MainScaffold(navController = navController, viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    viewModel: IncubatorViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Dashboard,
        Screen.Batches,
        Screen.Turning,
        Screen.Sensor,
        Screen.More
    )

    val showBottomBar = bottomBarScreens.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    bottomBarScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = isSelected,
                            modifier = Modifier.testTag("nav_${screen.route}"),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Dashboard Screen
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToBatches = { navController.navigate(Screen.Batches.route) },
                    onNavigateToCreateBatch = { navController.navigate(Screen.CreateBatch.route) },
                    onNavigateToTurning = { navController.navigate(Screen.Turning.route) },
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToSensor = { navController.navigate(Screen.Sensor.route) },
                    onNavigateToBatchDetail = { batchId ->
                        navController.navigate(Screen.BatchDetail.createRoute(batchId))
                    }
                )
            }

            // 2. Batches List Screen
            composable(Screen.Batches.route) {
                BatchesScreen(
                    viewModel = viewModel,
                    onNavigateToCreateBatch = { navController.navigate(Screen.CreateBatch.route) },
                    onNavigateToBatchDetail = { batchId ->
                        navController.navigate(Screen.BatchDetail.createRoute(batchId))
                    }
                )
            }

            // 3. Create Batch Screen
            composable(Screen.CreateBatch.route) {
                CreateEditBatchScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 4. Batch Details Screen
            composable(
                route = Screen.BatchDetail.route,
                arguments = listOf(navArgument("batchId") { type = NavType.LongType })
            ) { backStackEntry ->
                val batchId = backStackEntry.arguments?.getLong("batchId") ?: 0L
                BatchDetailScreen(
                    batchId = batchId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPredictor = { id ->
                        navController.navigate(Screen.PredictHatchRate.createRoute(id))
                    }
                )
            }

            // 5. Egg Turning Screen
            composable(Screen.Turning.route) {
                EggTurningScreen(viewModel = viewModel)
            }

            // 6. Sensor Telemetry Screen
            composable(Screen.Sensor.route) {
                SensorScreen(viewModel = viewModel)
            }

            // 7. More Tools Hub Screen
            composable(Screen.More.route) {
                MoreHubScreen(
                    viewModel = viewModel,
                    onNavigateToPredictor = { navController.navigate(Screen.PredictHatchRate.createRoute(null)) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToSpecies = { navController.navigate(Screen.SpeciesGuide.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // 8. History Screen
            composable(Screen.History.route) {
                HistoryScreen(viewModel = viewModel)
            }

            // 9. Alerts Screen
            composable(Screen.Alerts.route) {
                AlertsScreen(viewModel = viewModel)
            }

            // 10. Reports Screen
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = viewModel)
            }

            // 11. Species Guide Screen
            composable(Screen.SpeciesGuide.route) {
                SpeciesGuideScreen(
                    onSelectPresetToCreate = {
                        navController.navigate(Screen.CreateBatch.route)
                    }
                )
            }

            // 12. Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }

            // 13. Predict Hatch Rate Screen
            composable(
                route = Screen.PredictHatchRate.route,
                arguments = listOf(
                    navArgument("batchId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val batchIdStr = backStackEntry.arguments?.getString("batchId")
                val batchId = batchIdStr?.toLongOrNull()
                HatchRatePredictorScreen(
                    viewModel = viewModel,
                    batchId = batchId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
