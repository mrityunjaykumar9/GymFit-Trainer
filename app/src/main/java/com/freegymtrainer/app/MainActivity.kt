package com.freegymtrainer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.freegymtrainer.app.di.AppContainer
import com.freegymtrainer.app.di.SettingsDataStore
import com.freegymtrainer.app.presentation.navigation.NavGraph
import com.freegymtrainer.app.presentation.navigation.Screen
import com.freegymtrainer.app.presentation.theme.FreeGymTrainerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or not - proceed either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        val app = application as GymTrainerApp
        val settingsStore = SettingsDataStore(applicationContext)

        setContent {
            val themeMode by settingsStore.themeMode.collectAsState(initial = "light")
            FreeGymTrainerTheme(themeMode = themeMode) {
                AppRoot(container = app.container, settings = settingsStore, seedingReady = app.seedingReady)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AppRoot(container: AppContainer, settings: SettingsDataStore, seedingReady: kotlinx.coroutines.Deferred<Unit>) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Must wait for first-launch seeding (exercises/workout templates) to finish before
        // reading the profile/showing any screen - otherwise onboarding can complete before
        // workout templates exist, leaving the weekly plan permanently empty.
        seedingReady.await()
        val profile = container.userRepository.getProfile()
        startDestination = if (profile?.onboardingComplete == true) Screen.Home.route else Screen.Onboarding.route
    }

    val destination = startDestination
    if (destination == null) {
        Scaffold { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple(Screen.Home, Icons.Filled.Home, "Home"),
                        Triple(Screen.Exercises, Icons.Filled.FitnessCenter, "Exercises"),
                        Triple(Screen.Workouts, Icons.Filled.List, "Workouts"),
                        Triple(Screen.Nutrition, Icons.Filled.Restaurant, "Nutrition"),
                        Triple(Screen.Progress, Icons.Filled.ShowChart, "Progress")
                    )
                    items.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavGraph(navController = navController, container = container, settings = settings, startDestination = destination)
        }
    }
}
