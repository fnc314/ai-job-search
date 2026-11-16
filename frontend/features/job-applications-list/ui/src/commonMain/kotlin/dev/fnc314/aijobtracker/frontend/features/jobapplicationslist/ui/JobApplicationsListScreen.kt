@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings
import dev.fnc314.aijobtracker.frontend.core.designsystem.LocalWindowSizeClass
import dev.fnc314.aijobtracker.frontend.core.designsystem.WindowWidthSizeClass
import dev.fnc314.aijobtracker.frontend.core.designsystem.componentSizes
import dev.fnc314.aijobtracker.frontend.core.designsystem.iconSizes
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.widgets.EmptyStateWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.ErrorBannerWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.JobApplicationCard
import dev.fnc314.aijobtracker.frontend.core.widgets.LoadingWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.TooltipIconButton
import dev.fnc314.aijobtracker.frontend.core.widgets.TopLevelCardSurfaceVariant
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalTopBarTitleSummarySlot
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component.FilterMenu
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component.JobApplicationsSectionHeader
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component.JobApplicationsSummaryMenu
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component.SortMenu
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.Res
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_action_add_application
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_action_add_lead
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_empty_message
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_error_prefix
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_fab_cd
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_fab_icon_cd
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_jump_to_top_cd
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_jump_to_top_tooltip
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_tooltip_refresh
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main dashboard screen displaying the collection of tracked job applications in an adaptive list or grid.
 *
 * Automatically adapts between single-column lists on [WindowWidthSizeClass.Compact] viewports and multi-column
 * responsive grids on [WindowWidthSizeClass.Medium] and [WindowWidthSizeClass.Expanded] displays.
 *
 * @param viewModel Backing [JobApplicationsListViewModel] instance resolved via Koin.
 * @param modifier Layout modifier applied to the screen root.
 * @param settings Active user preferences [AppSettings].
 * @param onApplicationClick Callback invoked when user selects an application card, passing its slug/UUID.
 * @param onAddApplicationClick Callback invoked to navigate to the Add Application screen.
 * @param onAddLeadClick Callback invoked to navigate to the Add Lead screen.
 * @param onEditsClick Callback invoked to navigate to the Pending Edits screen.
 * @param onSettingsClick Callback invoked to open application preferences.
 * @param onSettingsChange Callback invoked when settings are modified.
 */
@Composable
public fun JobApplicationsListScreen(
  viewModel: JobApplicationsListViewModel = koinViewModel(),
  modifier: Modifier = Modifier,
  settings: AppSettings = AppSettings(),
  onApplicationClick: (String) -> Unit,
  onAddApplicationClick: () -> Unit = {},
  onAddLeadClick: () -> Unit = {},
  onEditsClick: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
  onSettingsChange: (AppSettings) -> Unit = {},
) {
  val state by viewModel.uiState.collectAsState()
  val titleSummarySlot = LocalTopBarTitleSummarySlot.current

  var lastContent by remember { mutableStateOf<JobApplicationsListUiState.Content?>(null) }
  if (state is JobApplicationsListUiState.Content) {
    lastContent = state as JobApplicationsListUiState.Content
  }

  // Publish summary content to the top app bar title dropdown when applications are loaded
  val currentApplications = (state as? JobApplicationsListUiState.Content)?.applications
    ?: lastContent?.applications
  LaunchedEffect(currentApplications) {
    if (currentApplications != null) {
      titleSummarySlot.summary.value = { onDismiss ->
        JobApplicationsSummaryMenu(
          applications = currentApplications,
          onDismiss = onDismiss,
        )
      }
    }
  }

  when (val s = state) {
    JobApplicationsListUiState.Loading -> {
      LoadingWidget(modifier = modifier)
    }

    is JobApplicationsListUiState.Error -> {
      ErrorBannerWidget(
        message = stringResource(Res.string.list_error_prefix, s.message),
        onRetry = { viewModel.onIntent(JobApplicationsListUiIntent.Refresh) },
        modifier = modifier.padding(MaterialTheme.spacing.medium),
      )
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
        viewModel.onIntent(JobApplicationsListUiIntent.ResetNavigation)
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
        viewModel.onIntent(JobApplicationsListUiIntent.ResetNavigation)
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
        viewModel.onIntent(JobApplicationsListUiIntent.ResetNavigation)
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
        viewModel.onIntent(JobApplicationsListUiIntent.ResetNavigation)
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
        viewModel.onIntent(JobApplicationsListUiIntent.ResetNavigation)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun JobApplicationsListContent(
  content: JobApplicationsListUiState.Content,
  onIntent: (JobApplicationsListUiIntent) -> Unit,
  modifier: Modifier = Modifier,
) {
  var fabMenuExpanded by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val windowSizeClass = LocalWindowSizeClass.current
  val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
  val coroutineScope = rememberCoroutineScope()

  val listState = rememberLazyListState()
  val gridState = rememberLazyGridState()

  // Reset scroll to top whenever the filter or sorting configuration changes
  LaunchedEffect(content.statusFilter, content.ageFilter, content.sortColumn, content.sortAscending) {
    if (isCompact) {
      listState.scrollToItem(0)
    } else {
      gridState.scrollToItem(0)
    }
  }

  // Derive whether to show the "Jump to Top" action
  val showJumpToTop by remember {
    derivedStateOf {
      val firstVisible = if (isCompact) listState.firstVisibleItemIndex else gridState.firstVisibleItemIndex
      firstVisible > 1
    }
  }

  // Group applications into prioritized categories: 1=Interviewing, 2=In Draft, 3=Applied, 4=Rejected
  val groupedApplications: Map<Int, List<JobApplicationItemUiModel>> = remember(content.applications) {
    content.applications
      .groupBy { it.status.sectionOrder }
      .entries
      .sortedBy { it.key }
      .associate { it.key to it.value }
  }
  val activeSections = remember(groupedApplications) {
    groupedApplications.keys.toList()
  }

  // Calculate item index offsets for jumping between sections
  val sectionOffsets = remember(groupedApplications) {
    val offsets = mutableMapOf<Int, Int>()
    var accumulated = 0
    groupedApplications.forEach { (sectionOrder, apps) ->
      offsets[sectionOrder] = accumulated
      // 1 item for the header + N items for the application cards
      accumulated += 1 + apps.size
    }
    offsets
  }

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
            tooltip = stringResource(Res.string.list_tooltip_refresh),
            icon = Icons.Default.Refresh,
            onClick = {
              onIntent(JobApplicationsListUiIntent.Refresh)
            },
          )
        }
      }

      // Content with pull-to-refresh
      PullToRefreshBox(
        isRefreshing = content.isRefreshing,
        onRefresh = { onIntent(JobApplicationsListUiIntent.Refresh) },
        modifier = Modifier.fillMaxSize(),
      ) {
        if (content.applications.isEmpty()) {
          EmptyStateWidget(
            title = stringResource(Res.string.list_empty_message),
            modifier = Modifier.fillMaxSize(),
          )
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
              ) {
                groupedApplications.forEach { (sectionOrder, apps) ->
                  stickyHeader(
                    key = "header_$sectionOrder",
                    contentType = "section_header",
                  ) {
                    JobApplicationsSectionHeader(
                      sectionOrder = sectionOrder,
                      count = apps.size,
                      availableSections = activeSections,
                      onJumpToSection = { targetOrder ->
                        val targetIndex = sectionOffsets[targetOrder] ?: 0
                        coroutineScope.launch {
                          listState.animateScrollToItem(targetIndex)
                        }
                      },
                    )
                  }

                  items(
                    items = apps,
                    key = { it.id },
                    contentType = { "application_card" },
                  ) { item ->
                    JobApplicationCard(
                      model = item.toCardUiModel(),
                      onApplicationClick = { onIntent(item.onClickIntent) },
                      modifier = Modifier,
                      variant = when (sectionOrder) {
                        1 -> TopLevelCardSurfaceVariant.Highlighted
                        2 -> TopLevelCardSurfaceVariant.FilledTonal
                        3 -> TopLevelCardSurfaceVariant.FilledTonal
                        4 -> TopLevelCardSurfaceVariant.Surface
                        else -> TopLevelCardSurfaceVariant.FilledTonal
                      },
                    )
                  }
                }
              }
            }

            WindowWidthSizeClass.Medium -> {
              LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
              ) {
                groupedApplications.forEach { (sectionOrder, apps) ->
                  item(
                    key = "header_$sectionOrder",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header",
                  ) {
                    JobApplicationsSectionHeader(
                      sectionOrder = sectionOrder,
                      count = apps.size,
                      availableSections = activeSections,
                      onJumpToSection = { targetOrder ->
                        val targetIndex = sectionOffsets[targetOrder] ?: 0
                        coroutineScope.launch {
                          gridState.animateScrollToItem(targetIndex)
                        }
                      },
                    )
                  }

                  items(
                    items = apps,
                    key = { it.id },
                    contentType = { "application_card" },
                  ) { item ->
                    JobApplicationCard(
                      model = item.toCardUiModel(),
                      onApplicationClick = { onIntent(item.onClickIntent) },
                      modifier = Modifier,
                      variant = when (sectionOrder) {
                        1 -> TopLevelCardSurfaceVariant.Highlighted
                        2 -> TopLevelCardSurfaceVariant.FilledTonal
                        3 -> TopLevelCardSurfaceVariant.FilledTonal
                        4 -> TopLevelCardSurfaceVariant.Surface
                        else -> TopLevelCardSurfaceVariant.FilledTonal
                      },
                    )
                  }
                }
              }
            }

            WindowWidthSizeClass.Expanded -> {
              LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = MaterialTheme.componentSizes.listCardAdaptiveMin),
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
              ) {
                groupedApplications.forEach { (sectionOrder, apps) ->
                  item(
                    key = "header_$sectionOrder",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "section_header",
                  ) {
                    JobApplicationsSectionHeader(
                      sectionOrder = sectionOrder,
                      count = apps.size,
                      availableSections = activeSections,
                      onJumpToSection = { targetOrder ->
                        val targetIndex = sectionOffsets[targetOrder] ?: 0
                        coroutineScope.launch {
                          gridState.animateScrollToItem(targetIndex)
                        }
                      },
                    )
                  }

                  items(
                    items = apps,
                    key = { it.id },
                    contentType = { "application_card" },
                  ) { item ->
                    JobApplicationCard(
                      model = item.toCardUiModel(),
                      onApplicationClick = { onIntent(item.onClickIntent) },
                      modifier = Modifier,
                      variant = when (sectionOrder) {
                        1 -> TopLevelCardSurfaceVariant.Highlighted
                        2 -> TopLevelCardSurfaceVariant.FilledTonal
                        3 -> TopLevelCardSurfaceVariant.FilledTonal
                        4 -> TopLevelCardSurfaceVariant.Surface
                        else -> TopLevelCardSurfaceVariant.FilledTonal
                      },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Floating Action Buttons column (Jump to Top + Add FAB)
    val fabCd: String = stringResource(Res.string.list_fab_cd)
    val jumpToTopCd: String = stringResource(Res.string.list_jump_to_top_cd)
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(MaterialTheme.spacing.medium),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
      AnimatedVisibility(
        visible = showJumpToTop,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
      ) {
        SmallFloatingActionButton(
          onClick = {
            coroutineScope.launch {
              if (isCompact) {
                listState.animateScrollToItem(0)
              } else {
                gridState.animateScrollToItem(0)
              }
            }
          },
          modifier = Modifier.semantics { contentDescription = jumpToTopCd },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = jumpToTopCd,
          )
        }
      }

      Box {
        FloatingActionButton(
          onClick = {
            focusManager.clearFocus()
            fabMenuExpanded = true
          },
          modifier = Modifier.semantics { contentDescription = fabCd },
          contentColor = MaterialTheme.colorScheme.onPrimary,
          containerColor = MaterialTheme.colorScheme.primary,
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.list_fab_icon_cd),
          )
        }
        DropdownMenu(
          expanded = fabMenuExpanded,
          onDismissRequest = { fabMenuExpanded = false },
        ) {
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.list_action_add_application)) },
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
            text = { Text(stringResource(Res.string.list_action_add_lead)) },
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
}
