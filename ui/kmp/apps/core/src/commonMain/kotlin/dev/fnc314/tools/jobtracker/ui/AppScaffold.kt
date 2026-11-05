package dev.fnc314.tools.jobtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.navigation.TopLevelDestination
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddApplicationRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.DetailRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.ListRoute
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsRoute
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.EditsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AppScaffold(
    navController: NavHostController,
    isAuthenticated: Boolean,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.isCompact

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isListSelected = currentDestination?.hasRoute<ListRoute>() == true
    val isEditsSelected = currentDestination?.hasRoute<EditsRoute>() == true
    val isSettingsSelected = currentDestination?.hasRoute<SettingsRoute>() == true
    val isDetailSelected = currentDestination?.hasRoute<DetailRoute>() == true
    val isAddAppSelected = currentDestination?.hasRoute<AddApplicationRoute>() == true
    val isAddLeadSelected = currentDestination?.hasRoute<AddLeadRoute>() == true

    val showNav = isAuthenticated && (isListSelected || isEditsSelected || isSettingsSelected)
    val canNavigateBack = navController.previousBackStackEntry != null && !isListSelected

    val title = when {
        isListSelected -> "Job Applications"
        isDetailSelected -> "Application Details"
        isAddAppSelected -> "Add Application"
        isAddLeadSelected -> "Add Lead"
        isEditsSelected -> "Pending Edits"
        isSettingsSelected -> "Settings"
        else -> "Job Applications"
    }

    val topLevelDestinations = remember {
        listOf(
            TopLevelDestination.from(
                route = ListRoute,
                label = "Applications",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                contentDescription = "Job Applications",
            ),
            TopLevelDestination.from(
                route = EditsRoute,
                label = "Pending Edits",
                selectedIcon = Icons.Filled.Edit,
                unselectedIcon = Icons.Outlined.Edit,
                contentDescription = "Pending Edits",
            ),
            TopLevelDestination.from(
                route = SettingsRoute,
                label = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                contentDescription = "Settings",
            ),
        )
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                            )
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = "Calendar",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            if (isCompact && showNav) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        val selected = when (destination.route) {
                            is ListRoute -> isListSelected
                            is EditsRoute -> isEditsSelected
                            is SettingsRoute -> isSettingsSelected
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        if (destination.route is ListRoute) {
                                            popUpTo(ListRoute) { inclusive = true }
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.contentDescription,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
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
                    topLevelDestinations.forEach { destination ->
                        val selected = when (destination.route) {
                            is ListRoute -> isListSelected
                            is EditsRoute -> isEditsSelected
                            is SettingsRoute -> isSettingsSelected
                            else -> false
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        if (destination.route is ListRoute) {
                                            popUpTo(ListRoute) { inclusive = true }
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.contentDescription,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
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
