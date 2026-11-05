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
import fnc314jobsearchtracker.apps_core.generated.resources.Res
import fnc314jobsearchtracker.apps_core.generated.resources.app_action_back
import fnc314jobsearchtracker.apps_core.generated.resources.app_action_calendar
import fnc314jobsearchtracker.apps_core.generated.resources.app_action_search
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_applications
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_applications_cd
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_edits
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_edits_cd
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_settings
import fnc314jobsearchtracker.apps_core.generated.resources.app_nav_settings_cd
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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

    val titleRes: StringResource = when {
        isListSelected -> ListRoute.titleRes
        isDetailSelected -> DetailRoute("").titleRes
        isAddAppSelected -> AddApplicationRoute.titleRes
        isAddLeadSelected -> AddLeadRoute.titleRes
        isEditsSelected -> EditsRoute.titleRes
        isSettingsSelected -> SettingsRoute.titleRes
        else -> ListRoute.titleRes
    }

    val topLevelDestinations = remember {
        listOf(
            TopLevelDestination.from(
                route = ListRoute,
                labelRes = Res.string.app_nav_applications,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                contentDescriptionRes = Res.string.app_nav_applications_cd,
            ),
            TopLevelDestination.from(
                route = EditsRoute,
                labelRes = Res.string.app_nav_edits,
                selectedIcon = Icons.Filled.Edit,
                unselectedIcon = Icons.Outlined.Edit,
                contentDescriptionRes = Res.string.app_nav_edits_cd,
            ),
            TopLevelDestination.from(
                route = SettingsRoute,
                labelRes = Res.string.app_nav_settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                contentDescriptionRes = Res.string.app_nav_settings_cd,
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
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.app_action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (isAuthenticated) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(Res.string.app_action_search),
                            )
                        }
                        IconButton(onClick = onCalendarClick) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = stringResource(Res.string.app_action_calendar),
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
                                    contentDescription = stringResource(destination.contentDescriptionRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
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
                                    contentDescription = stringResource(destination.contentDescriptionRes),
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
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
