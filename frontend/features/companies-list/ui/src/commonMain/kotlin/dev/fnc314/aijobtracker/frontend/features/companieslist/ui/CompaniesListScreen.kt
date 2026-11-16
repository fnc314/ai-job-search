@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fnc314.aijobtracker.frontend.features.companieslist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.designsystem.LocalWindowSizeClass
import dev.fnc314.aijobtracker.frontend.core.designsystem.WindowWidthSizeClass
import dev.fnc314.aijobtracker.frontend.core.designsystem.componentSizes
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalTopBarTitleSummarySlot
import dev.fnc314.aijobtracker.frontend.core.widgets.EmptyStateWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.ErrorBannerWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.LoadingWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.TooltipIconButton
import dev.fnc314.aijobtracker.frontend.core.widgets.TopLevelCardSurfaceVariant
import dev.fnc314.aijobtracker.frontend.features.companieslist.domain.CompanySortOption
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.component.CompaniesListSummaryMenu
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.component.CompanyCard
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.component.CompanyFilterMenu
import ai_job_tracker_frontend.features_companies_list_ui.generated.resources.Res
import ai_job_tracker_frontend.features_companies_list_ui.generated.resources.companies_empty_message
import ai_job_tracker_frontend.features_companies_list_ui.generated.resources.companies_search_placeholder
import ai_job_tracker_frontend.features_companies_list_ui.generated.resources.companies_sort_menu_tooltip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Top-level screen presenting the catalog of employers.
 */
@Composable
internal fun CompaniesListScreen(
    onCompanyClick: (companyId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompaniesListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val titleSummarySlot = LocalTopBarTitleSummarySlot.current

    val companies = (state as? CompaniesListUiState.Content)?.companies
    androidx.compose.runtime.LaunchedEffect(companies) {
        if (companies != null) {
            titleSummarySlot.summary.value = { onDismiss ->
                CompaniesListSummaryMenu(
                    companies = companies,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    when (val s = state) {
        is CompaniesListUiState.Loading -> {
            LoadingWidget(modifier = modifier.fillMaxSize())
        }
        is CompaniesListUiState.Error -> {
            ErrorBannerWidget(
                message = s.message,
                onRetry = { viewModel.onIntent(CompaniesListUiIntent.Refresh) },
                modifier = modifier.fillMaxSize(),
            )
        }
        is CompaniesListUiState.Content -> {
            CompaniesListContent(
                content = s,
                onIntent = viewModel::onIntent,
                onCompanyClick = onCompanyClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun CompaniesListContent(
    content: CompaniesListUiState.Content,
    onIntent: (CompaniesListUiIntent) -> Unit,
    onCompanyClick: (companyId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar row 1: FilterMenu opposite SortMenu (following JobApplicationsListScreen pattern)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CompanyFilterMenu(
                selectedFilter = content.statusFilter,
                onFilterChange = { onIntent(CompaniesListUiIntent.ChangeStatusFilter(it)) },
            )

            Box {
                TooltipIconButton(
                    tooltip = stringResource(Res.string.companies_sort_menu_tooltip),
                    icon = Icons.AutoMirrored.Filled.List,
                    onClick = { sortMenuExpanded = true },
                )
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                ) {
                    CompanySortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                sortMenuExpanded = false
                                onIntent(CompaniesListUiIntent.ChangeSort(option))
                            },
                        )
                    }
                }
            }
        }

        // Toolbar row 2: Non-wrapping search bar with clear icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = content.searchQuery,
                onValueChange = { onIntent(CompaniesListUiIntent.Search(it)) },
                placeholder = {
                    Text(
                        text = stringResource(Res.string.companies_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (content.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onIntent(CompaniesListUiIntent.Search("")) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PullToRefreshBox(
            isRefreshing = content.isRefreshing,
            onRefresh = { onIntent(CompaniesListUiIntent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            if (content.companies.isEmpty()) {
                EmptyStateWidget(
                    title = stringResource(Res.string.companies_empty_message),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val listPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small,
                )

                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = listPadding,
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            items(content.companies, key = { it.id }) { company ->
                                CompanyCard(
                                    model = company,
                                    onCompanyClick = onCompanyClick,
                                    variant = TopLevelCardSurfaceVariant.Highlighted,
                                )
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
                            items(content.companies, key = { it.id }) { company ->
                                CompanyCard(
                                    model = company,
                                    onCompanyClick = onCompanyClick,
                                    variant = TopLevelCardSurfaceVariant.Highlighted,
                                )
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
                            items(content.companies, key = { it.id }) { company ->
                                CompanyCard(
                                    model = company,
                                    onCompanyClick = onCompanyClick,
                                    variant = TopLevelCardSurfaceVariant.Highlighted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
