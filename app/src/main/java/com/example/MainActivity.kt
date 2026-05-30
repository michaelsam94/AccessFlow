package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.service.AccessFlowService
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MacroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent deep-links, if any
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                // Automatic onboarding enforcement: If required service permissions are missing or turned off, navigate directly to onboarding
                LaunchedEffect(isServiceActive) {
                    if (!isServiceActive) {
                        Log.d("MainActivity", "Accessibility service offline. Directing user to onboarding graph.")
                        navController.navigate("onboarding_graph") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (navController.currentDestination?.route == "onboarding_graph") {
                        Log.d("MainActivity", "Accessibility service active. Unlocking main dashboard.")
                        navController.navigate("macros") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (isServiceActive) "macros" else "onboarding_graph",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Onboarding Navigation Graph Section
                    composable(route = "onboarding_graph") {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = {
                                navController.navigate("macros") {
                                    popUpTo("onboarding_graph") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Main App Feature Screens structured inside default bottom scaffolds
                    composable(route = "macros") {
                        MainScaffold(navController = navController, viewModel = viewModel) {
                            MacrosListScreen(viewModel = viewModel)
                        }
                    }

                    composable(
                        route = "macros?highlightId={highlightId}",
                        arguments = listOf(
                            navArgument("highlightId") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        ),
                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "accessflow://macro/{highlightId}"
                            }
                        )
                    ) { backStackEntry ->
                        val highlightId = backStackEntry.arguments?.getString("highlightId")
                        MainScaffold(navController = navController, viewModel = viewModel) {
                            MacrosListScreen(viewModel = viewModel, highlightMacroId = highlightId)
                        }
                    }

                    composable(route = "rules") {
                        MainScaffold(navController = navController, viewModel = viewModel) {
                            PhraseRulesScreen(viewModel = viewModel)
                        }
                    }

                    composable(
                        route = "settings",
                        deepLinks = listOf(
                            navDeepLink {
                                uriPattern = "accessflow://settings"
                            }
                        )
                    ) {
                        MainScaffold(navController = navController, viewModel = viewModel) {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Ensure VM keeps up to date with settings toggled in system panels
        viewModel.checkPermissions()
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        val data = intent?.data
        if (Intent.ACTION_VIEW == action && data != null) {
            Log.d("MainActivity", "Resolved custom AccessFlow deep link: $data")
        }
    }
}

@Composable
fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    viewModel: MacroViewModel,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SpaceCardBg,
                contentColor = Color.White
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Navigation Items List
                val items = listOf(
                    Triple("macros", "Macros", Icons.Default.List),
                    Triple("rules", "Phrase Rules", Icons.Default.Edit),
                    Triple("settings", "Diagnostics", Icons.Default.Settings)
                )

                items.forEach { (route, label, icon) ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true
                    NavigationBarItem(
                        icon = { Icon(imageVector = icon, contentDescription = "$label Navigation shortcut link") },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        selected = isSelected,
                        onClick = {
                            if (currentDestination?.route != route) {
                                // Bottom nav navigation state preservation requirements
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonTeal,
                            selectedTextColor = NeonTeal,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate,
                            indicatorColor = Color(0xFF2E244B)
                        ),
                        modifier = Modifier
                            .testTag("nav_item_$route")
                            .semantics { 
                                role = Role.Button
                            }
                    )
                }
            }
        },
        containerColor = SpaceDarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}
