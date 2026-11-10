package dev.fnc314.tools.jobtracker.features.companieslist.ui

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
import androidx.compose.material.icons.filled.Refresh
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
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.EmptyStateWidget
import dev.fnc314.tools.jobtracker.core.widgets.ErrorBannerWidget
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanySortOption
import dev.fnc314.tools.jobtracker.features.companieslist.ui.component.CompanyCard
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_empty_message
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_refresh_tooltip
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_search_placeholder
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_sort_menu_tooltip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Top-level screen presenting the catalog of employers.
 */
@Composable
public fun CompaniesListScreen(
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CompaniesListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompaniesListContent(
    content: CompaniesListUiState.Content,
    onIntent: (CompaniesListUiIntent) -> Unit,
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Toolbar: Search + Sort + Refresh
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
            OutlinedTextField(
                value = content.searchQuery,
                onValueChange = { onIntent(CompaniesListUiIntent.Search(it)) },
                placeholder = {
                    Text(
                        stringResource(Res.string.companies_search_placeholder),
                        style = MaterialTheme.typography.bodySmall,
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
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

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

            TooltipIconButton(
                tooltip = stringResource(Res.string.companies_refresh_tooltip),
                icon = Icons.Default.Refresh,
                onClick = { onIntent(CompaniesListUiIntent.Refresh) },
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
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
