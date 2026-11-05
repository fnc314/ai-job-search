package dev.fnc314.tools.jobtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.navigation.EditsRoute
import dev.fnc314.tools.jobtracker.navigation.ListRoute
import dev.fnc314.tools.jobtracker.navigation.SettingsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AppScaffold(
    navController: NavHostController,
    isAuthenticated: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.isCompact

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isListSelected = currentDestination?.hasRoute<ListRoute>() == true
    val isEditsSelected = currentDestination?.hasRoute<EditsRoute>() == true
    val isSettingsSelected = currentDestination?.hasRoute<SettingsRoute>() == true

    // Show navigation controls only when authenticated
    val showNav = isAuthenticated && (isListSelected || isEditsSelected || isSettingsSelected)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Job Applications",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {},
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            if (isCompact && showNav) {
                NavigationBar {
                    NavigationBarItem(
                        selected = isListSelected,
                        onClick = {
                            if (!isListSelected) {
                                navController.navigate(ListRoute) {
                                    popUpTo(ListRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isListSelected) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Job Applications",
                            )
                        },
                        label = { Text("Applications") },
                    )
                    NavigationBarItem(
                        selected = isEditsSelected,
                        onClick = {
                            if (!isEditsSelected) {
                                navController.navigate(EditsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isEditsSelected) Icons.Filled.Edit else Icons.Outlined.Edit,
                                contentDescription = "Pending Edits",
                            )
                        },
                        label = { Text("Pending Edits") },
                    )
                    NavigationBarItem(
                        selected = isSettingsSelected,
                        onClick = {
                            if (!isSettingsSelected) {
                                navController.navigate(SettingsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings",
                            )
                        },
                        label = { Text("Settings") },
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->
        if (!isCompact && isAuthenticated) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                NavigationRail {
                    NavigationRailItem(
                        selected = isListSelected,
                        onClick = {
                            if (!isListSelected) {
                                navController.navigate(ListRoute) {
                                    popUpTo(ListRoute) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isListSelected) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Job Applications",
                            )
                        },
                        label = { Text("Applications") },
                    )
                    NavigationRailItem(
                        selected = isEditsSelected,
                        onClick = {
                            if (!isEditsSelected) {
                                navController.navigate(EditsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isEditsSelected) Icons.Filled.Edit else Icons.Outlined.Edit,
                                contentDescription = "Pending Edits",
                            )
                        },
                        label = { Text("Pending Edits") },
                    )
                    NavigationRailItem(
                        selected = isSettingsSelected,
                        onClick = {
                            if (!isSettingsSelected) {
                                navController.navigate(SettingsRoute) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings",
                            )
                        },
                        label = { Text("Settings") },
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content(PaddingValues())
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                content(PaddingValues())
            }
        }
    }
}
