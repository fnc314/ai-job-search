package dev.fnc314.tools.jobtracker.ui.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase
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
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val applications by viewModel.filteredApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val sortColumn by viewModel.sortColumn.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(AppSettings()) }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onSettingsChange = { settings = it },
            onDismiss = { showSettings = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Applications") },
                actions = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.semantics { contentDescription = "Open settings" },
                    ) {
                        Text("⚙")
                    }
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.semantics { contentDescription = "Refresh" },
                    ) {
                        Text("↻")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.semantics { contentDescription = "Add new application" },
            ) {
                Text("+")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterJobApplicationsUseCase.FilterType.entries.forEach { filter ->
                    FilterChipItem(
                        filterType = filter,
                        selected = activeFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }

            // Column header with sort indicators
            ColumnHeader(
                sortColumn = sortColumn,
                sortAscending = sortAscending,
                onSortClick = { column -> viewModel.toggleSortColumn(column) },
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error: $error")
                            Spacer(modifier = Modifier.height(8.dp))
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
                        Text("No applications found")
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(applications, key = { it.id }) { app ->
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
