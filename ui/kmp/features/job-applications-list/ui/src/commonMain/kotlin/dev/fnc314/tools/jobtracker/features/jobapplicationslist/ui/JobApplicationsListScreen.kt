package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.FilterMenu
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.JobApplicationRow
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component.SortMenu
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun JobApplicationsListScreen(
    viewModel: JobApplicationsListViewModel = koinViewModel(),
    onApplicationClick: (String) -> Unit,
    onAddApplicationClick: () -> Unit = {},
    onAddLeadClick: () -> Unit = {},
    onEditsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    modifier: Modifier = Modifier,
): Unit {
    val state by viewModel.uiState.collectAsState()

    var lastContent by remember { mutableStateOf<JobApplicationsListUiState.Content?>(null) }
    if (state is JobApplicationsListUiState.Content) {
        lastContent = state as JobApplicationsListUiState.Content
    }

    when (val s = state) {
        JobApplicationsListUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is JobApplicationsListUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Text(
                        text = "Error: ${s.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = { viewModel.onIntent(JobApplicationsListUiIntent.Refresh) }) {
                        Text("Retry")
                    }
                }
            }
        }
        is JobApplicationsListUiState.Content -> {
            JobApplicationsListContent(
                content = s,
                onIntent = viewModel::onIntent,
                modifier = modifier,
            )
        }
        is JobApplicationsListUiState.NavigateToDetail -> {
            LaunchedEffect(s) {
                onApplicationClick(s.applicationId)
            }
            lastContent?.let { content ->
                JobApplicationsListContent(
                    content = content,
                    onIntent = viewModel::onIntent,
                    modifier = modifier,
                )
            }
        }
        JobApplicationsListUiState.NavigateToAddApplication -> {
            LaunchedEffect(s) {
                onAddApplicationClick()
            }
            lastContent?.let { content ->
                JobApplicationsListContent(
                    content = content,
                    onIntent = viewModel::onIntent,
                    modifier = modifier,
                )
            }
        }
        JobApplicationsListUiState.NavigateToAddLead -> {
            LaunchedEffect(s) {
                onAddLeadClick()
            }
            lastContent?.let { content ->
                JobApplicationsListContent(
                    content = content,
                    onIntent = viewModel::onIntent,
                    modifier = modifier,
                )
            }
        }
        JobApplicationsListUiState.NavigateToEdits -> {
            LaunchedEffect(s) {
                onEditsClick()
            }
            lastContent?.let { content ->
                JobApplicationsListContent(
                    content = content,
                    onIntent = viewModel::onIntent,
                    modifier = modifier,
                )
            }
        }
        JobApplicationsListUiState.NavigateToSettings -> {
            LaunchedEffect(s) {
                onSettingsClick()
            }
            lastContent?.let { content ->
                JobApplicationsListContent(
                    content = content,
                    onIntent = viewModel::onIntent,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
internal fun JobApplicationsListContent(
    content: JobApplicationsListUiState.Content,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var fabMenuExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val windowSizeClass = LocalWindowSizeClass.current

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Toolbar row: FilterMenu opposite SortMenu & Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.extraSmall,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterMenu(
                    selectedStatus = content.statusFilter,
                    selectedAge = content.ageFilter,
                    onFilterChange = { status, age ->
                        onIntent(JobApplicationsListUiIntent.ChangeFilter(status, age))
                    },
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    SortMenu(
                        sortColumn = content.sortColumn,
                        sortAscending = content.sortAscending,
                        onSortChange = { col, asc ->
                            onIntent(JobApplicationsListUiIntent.ChangeSort(col, asc))
                        },
                    )
                    TooltipIconButton(
                        tooltip = "Refresh",
                        icon = Icons.Default.Refresh,
                        onClick = { onIntent(JobApplicationsListUiIntent.Refresh) },
                    )
                    TooltipIconButton(
                        tooltip = "Pending edits",
                        icon = Icons.Default.Edit,
                        onClick = { onIntent(JobApplicationsListUiIntent.ClickEdits) },
                    )
                    TooltipIconButton(
                        tooltip = "Settings",
                        icon = Icons.Default.Settings,
                        onClick = { onIntent(JobApplicationsListUiIntent.ClickSettings) },
                    )
                }
            }

            // Content
            if (content.applications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No applications found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
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
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            items(content.applications, key = { it.id }) { item ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    JobApplicationRow(
                                        model = item,
                                        onIntent = onIntent,
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
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            items(content.applications, key = { it.id }) { item ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    JobApplicationRow(
                                        model = item,
                                        onIntent = onIntent,
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
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            items(content.applications, key = { it.id }) { item ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    JobApplicationRow(
                                        model = item,
                                        onIntent = onIntent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button with dropdown
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.spacing.medium),
        ) {
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
                        onIntent(JobApplicationsListUiIntent.ClickAddApplication)
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
                        onIntent(JobApplicationsListUiIntent.ClickAddLead)
                    },
                )
            }
        }
    }
}
