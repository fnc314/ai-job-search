package dev.fnc314.tools.jobtracker.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material.icons.sharp.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import dev.fnc314.tools.jobtracker.ui.list.component.ColumnHeader
import dev.fnc314.tools.jobtracker.ui.list.component.FilterChipItem
import dev.fnc314.tools.jobtracker.ui.list.component.JobApplicationRow
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import dev.fnc314.tools.jobtracker.ui.settings.SettingsDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun JobApplicationsListScreen(
    viewModel: JobApplicationsListViewModel = koinViewModel(),
    onApplicationClick: (String) -> Unit,
    onAddApplicationClick: () -> Unit = {},
    onAddLeadClick: () -> Unit = {},
    onEditsClick: () -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    signInViewModel: SignInViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val applications by viewModel.filteredApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val sortColumn by viewModel.sortColumn.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val windowSizeClass = LocalWindowSizeClass.current

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onSettingsChange = {
                onSettingsChange(it)
            },
            onDismiss = { showSettings = false },
            viewModel = signInViewModel,
        )
    }

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
                    IconButton(
                        onClick = onEditsClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .semantics { contentDescription = "View pending edits" },
                    ) {
                        Icon(Icons.Sharp.Edit, contentDescription = "View pending edits")
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .semantics { contentDescription = "Open settings" },
                    ) {
                        Icon(Icons.Sharp.Settings, contentDescription = "Open settings")
                    }
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                            .semantics { contentDescription = "Refresh" },
                    ) {
                        Icon(Icons.Sharp.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = {
                        focusManager.clearFocus()
                        fabMenuExpanded = true
                    },
                    modifier = Modifier.semantics { contentDescription = "Add new application" },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Sharp.Add, contentDescription = "Add")
                }
                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Application") },
                        onClick = {
                            fabMenuExpanded = false
                            onAddApplicationClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Add Lead") },
                        onClick = {
                            fabMenuExpanded = false
                            onAddLeadClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Pending Edits") },
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
            // Horizontally scrollable filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                FilterJobApplicationsUseCase.FilterType.entries.forEach { filter ->
                    FilterChipItem(
                        filterType = filter,
                        selected = activeFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }

            // Column header with sort indicators (adapts to settings and screen width)
            ColumnHeader(
                sortColumn = sortColumn,
                sortAscending = sortAscending,
                onSortClick = { column -> viewModel.toggleSortColumn(column) },
                settings = settings,
            )

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
                    // Screen-size reactive layout:
                    // Compact (<600dp): 1-column list
                    // Medium (600-840dp): 2-column grid
                    // Expanded (>=840dp): Adaptive 3+ column grid
                    val listPadding = PaddingValues(
                        start = MaterialTheme.spacing.medium,
                        top = MaterialTheme.spacing.small,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.fabBottomPadding, // Guarantees content is never obscured by FAB
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
                                columns = GridCells.Adaptive(minSize = 360.dp),
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
