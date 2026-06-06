package com.example

import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GatewayViewModel

enum class AppScreen {
    Intro,
    Permissions,
    Onboarding,
    Dashboard
}

enum class DashboardTab(val title: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    History("History", Icons.Default.History),
    SIMs("SIMs", Icons.Default.SdCard),
    Settings("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: GatewayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val isPaired by viewModel.isPaired.collectAsStateWithLifecycle()

                var currentScreen by remember { mutableStateOf(AppScreen.Intro) }
                var activeTab by remember { mutableStateOf(DashboardTab.Home) }

                // Periodic permission check e.g. onResume or state change
                fun refreshNavigationFlow() {
                    val hasSeenWelcome = viewModel.prefsManager.hasSeenWelcome
                    currentScreen = if (!hasSeenWelcome) {
                        AppScreen.Intro
                    } else if (!viewModel.hasPermissions(context)) {
                        AppScreen.Permissions
                    } else if (!isPaired) {
                        AppScreen.Onboarding
                    } else {
                        AppScreen.Dashboard
                    }
                }

                LaunchedEffect(isPaired) {
                    refreshNavigationFlow()
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshNavigationFlow()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CosmicDark
                ) {
                    when (currentScreen) {
                        AppScreen.Intro -> {
                            IntroScreen(
                                onContinue = {
                                    viewModel.prefsManager.hasSeenWelcome = true
                                    refreshNavigationFlow()
                                }
                            )
                        }

                        AppScreen.Permissions -> {
                            PermissionsScreen(
                                viewModel = viewModel,
                                onContinue = {
                                    refreshNavigationFlow()
                                }
                            )
                        }

                        AppScreen.Onboarding -> {
                            OnboardingScreen(
                                viewModel = viewModel,
                                modifier = Modifier.safeDrawingPadding()
                            )
                        }

                        AppScreen.Dashboard -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = CosmicCard,
                                        contentColor = Color.White
                                    ) {
                                        DashboardTab.values().forEach { tab ->
                                            NavigationBarItem(
                                                selected = activeTab == tab,
                                                onClick = { activeTab = tab },
                                                icon = {
                                                    Icon(
                                                        imageVector = tab.icon,
                                                        contentDescription = tab.title
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = tab.title,
                                                        color = if (activeTab == tab) VibrantGreen else MediumGray
                                                    )
                                                },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = VibrantGreen,
                                                    unselectedIconColor = MediumGray,
                                                    indicatorColor = Color(0xFF1E262F)
                                                )
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                               Box(
                                   modifier = Modifier
                                       .fillMaxSize()
                                       .background(CosmicDark)
                                       .padding(innerPadding)
                               ) {
                                   when (activeTab) {
                                       DashboardTab.Home -> {
                                           HomeScreen(
                                               viewModel = viewModel,
                                               onEditCredentials = {
                                                   viewModel.unpairDevice()
                                               }
                                           )
                                       }
                                       DashboardTab.History -> {
                                           HistoryScreen(viewModel = viewModel)
                                       }
                                       DashboardTab.SIMs -> {
                                           SimScreen(viewModel = viewModel)
                                       }
                                       DashboardTab.Settings -> {
                                           SettingsScreen(
                                               viewModel = viewModel,
                                               onManualPairEdit = {
                                                   viewModel.unpairDevice()
                                               }
                                           )
                                       }
                                   }
                               }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-calculate permissions status on resume
        if (viewModel.hasPermissions(this)) {
            // permissions satisfied, trigger state sync to see if background service should be restarted
        }
    }
}
