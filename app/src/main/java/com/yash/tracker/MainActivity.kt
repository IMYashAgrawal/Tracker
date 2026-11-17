package com.yash.tracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.yash.tracker.security.VaultLifecycleObserver
import com.yash.tracker.ui.screen1_log.LogScreen
import com.yash.tracker.ui.screen2_history.HistoryScreen
import com.yash.tracker.ui.screen3_analysis.AnalysisScreen
import com.yash.tracker.ui.theme.TrackerTheme
import com.yash.tracker.ui.vault.VaultScreen
import com.yash.tracker.ui.vault.VaultState
import com.yash.tracker.ui.vault.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots and hide content in Recents switcher
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        setContent {
            TrackerTheme {
                val vaultViewModel: VaultViewModel = viewModel()
                val vaultState by vaultViewModel.uiState.collectAsState()

                // Register observer for auto-lock when app goes to background
                DisposableEffect(Unit) {
                    val observer = VaultLifecycleObserver {
                        vaultViewModel.lock()
                    }
                    ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
                    onDispose {
                        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (vaultState is VaultState.Unlocked) {
                        MainNavigation()
                    } else {
                        VaultScreen(
                            viewModel = vaultViewModel,
                            onVaultUnlocked = { /* State change triggers recomposition */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Log,
        Screen.History,
        Screen.Analysis
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Log.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Log.route) { LogScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Analysis.route) { AnalysisScreen() }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Log : Screen("log", "Log", Icons.Default.Add)
    object History : Screen("history", "History", Icons.Default.History)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Insights)
}

@Preview(showBackground = true)
@Composable
fun VaultLockedPreview() {
    TrackerTheme {
        Text("Vault Locked Screen")
    }
}
