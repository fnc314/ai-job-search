package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.FilterMenu
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.JobApplicationRow
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.SortMenu
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun JobApplicationsListScreen(
    viewModel: JobApplicationsListViewModel = koinViewModel(),
    onApplicationClick: (String) -> Unit,
    onAddApplicationClick: () -> Unit = {},
    onAddLeadClick: () -> Unit = {},
    onEditsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val applications by viewModel.filteredApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val ageFilter by viewModel.ageFilter.collectAsState()
    val sortColumn by viewModel.sortColumn.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    var fabMenuExpanded by remember { mutableStateOf(false) }
    var selectedBottomNavIndex by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    val windowSizeClass = LocalWindowSizeClass.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Job Applications",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    TooltipIconButton(
                        tooltip = "Refresh",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.refresh() },
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 0,
                    onClick = { selectedBottomNavIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomNavIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Job Applications",
                        )
                    },
                    label = { Text("Applications") },
                )
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 1,
                    onClick = {
                        selectedBottomNavIndex = 1
                        onEditsClick()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomNavIndex == 1) Icons.Filled.Edit else Icons.Outlined.Edit,
                            contentDescription = "Pending Edits",
                        )
                    },
                    label = { Text("Pending Edits") },
                )
                NavigationBarItem(
                    selected = selectedBottomNavIndex == 2,
                    onClick = {
                        selectedBottomNavIndex = 2
                        onSettingsClick()
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedBottomNavIndex == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    },
                    label = { Text("Settings") },
                )
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        fabMenuExpanded = true
                    },
                    modifier = Modifier.semantics { contentDescription = "Add new application or lead" },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Application") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            onAddApplicationClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Add Lead") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            onAddLeadClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Pending Edits") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            onEditsClick()
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Toolbar row: FilterMenu opposite SortMenu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterMenu(
                    selectedStatus = statusFilter,
                    selectedAge = ageFilter,
                    onFilterChange = { status, age ->
                        viewModel.setFilter(status, age)
                    },
                )
                SortMenu(
                    sortColumn = sortColumn,
                    sortAscending = sortAscending,
                    onSortChange = { col, asc ->
                        viewModel.setSort(col, asc)
                    },
                )
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                applications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No applications found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val listPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        top = MaterialTheme.spacing.small,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.fabBottomPadding,
                    )

                    when (windowSizeClass.widthSizeClass) {
                        WindowWidthSizeClass.Compact -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listPadding,
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                items(applications, key = { it.id }) { app ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        JobApplicationRow(
                                            application = app,
                                            onClick = { onApplicationClick(app.id) },
                                        )
                                    }
                                }
                            }
                        }
                        WindowWidthSizeClass.Medium -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listPadding,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                            ) {
                                items(applications, key = { it.id }) { app ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        JobApplicationRow(
                                            application = app,
                                            onClick = { onApplicationClick(app.id) },
                                        )
                                    }
                                }
                            }
                        }
                        WindowWidthSizeClass.Expanded -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = MaterialTheme.componentSizes.listCardAdaptiveMin),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = listPadding,
                                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                items(applications, key = { it.id }) { app ->
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        JobApplicationRow(
                                            application = app,
                                            onClick = { onApplicationClick(app.id) },
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
