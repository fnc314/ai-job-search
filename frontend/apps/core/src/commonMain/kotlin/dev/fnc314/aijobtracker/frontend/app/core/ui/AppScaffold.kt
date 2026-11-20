@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fnc314.aijobtracker.frontend.app.core.ui

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Business
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.designsystem.LocalWindowSizeClass
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalTopBarActionsSlot
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalTopBarTitleSummarySlot
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActionsSlot
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarTitleSummarySlot
import dev.fnc314.aijobtracker.frontend.core.widgets.TopBarTitleDropdown
import dev.fnc314.aijobtracker.frontend.core.navigation.Navigator
import dev.fnc314.aijobtracker.frontend.core.navigation.TopLevelDestination
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddApplicationRoute
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddLeadRoute
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.JobApplicationCompaniesRoute
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.JobApplicationCompanyDetailsRoute
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.JobApplicationDetailRoute
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationListRoute
import dev.fnc314.aijobtracker.frontend.features.settings.ui.JobApplicationSettingsRoute
import dev.fnc314.aijobtracker.frontend.features.aiapply.ui.AiApplyRoute
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_action_back
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_applications
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_applications_cd
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_companies
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_companies_cd
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_apply
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_apply_cd
import dev.fnc314.aijobtracker.frontend.apps_core.generated.resources.app_nav_settings_cd
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Data class carrying all UI metadata for a top-level destination.
 */
private data class TopLevelDestinationInfo(
    val enumValue: TopLevelDestination,
    val route: JobApplicationsTrackerNavKey,
    val labelRes: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescriptionRes: StringResource,
)

/**
 * Resolves the current [TopLevelDestination] from the active destination on the back stack.
 */
private fun resolveTopLevelDestination(currentDestination: JobApplicationsTrackerNavKey?): TopLevelDestination? =
    when {
        currentDestination == JobApplicationListRoute -> TopLevelDestination.Applications
        currentDestination == JobApplicationCompaniesRoute -> TopLevelDestination.Companies
        currentDestination == AiApplyRoute -> TopLevelDestination.Apply
        currentDestination == dev.fnc314.aijobtracker.frontend.features.trackeredits.ui.JobApplicationEditsRoute -> TopLevelDestination.Edits
        currentDestination == JobApplicationSettingsRoute -> TopLevelDestination.Settings
        else -> null
    }

/**
 * Returns true if the destination is a top-level bottom-nav target.
 */
private fun isTopLevelDestination(key: JobApplicationsTrackerNavKey?): Boolean =
    key == JobApplicationListRoute ||
        key == JobApplicationCompaniesRoute ||
        key == AiApplyRoute


/**
 * Responsive application scaffold adapting navigation chrome between mobile (bottom navigation bar)
 * and tablet/desktop (navigation rail).
 *
 * @param navigator Navigation coordinator tracking active destination and back stack.
 * @param isAuthenticated Whether a valid authenticated user session is active.
 * @param snackbarHostState State manager for displaying transient snackbar alerts.
 * @param modifier Composable layout modifier.
 * @param content Slot rendering screen content with appropriate scaffold padding.
 *   Child screens contribute top-bar actions via [LocalTopBarActionsSlot] to the
 *   shared TopAppBar.
 */
@Composable
public fun AppScaffold(
  navigator: Navigator,
  isAuthenticated: Boolean,
  snackbarHostState: SnackbarHostState,
  modifier: Modifier = Modifier,
  content: @Composable (PaddingValues) -> Unit,
) {
  val windowSizeClass = LocalWindowSizeClass.current
  val isCompact = windowSizeClass.isCompact
  val appNavigator = LocalAppNavigator.current

  val currentDestination: JobApplicationsTrackerNavKey? = navigator.backStack.lastOrNull() as? JobApplicationsTrackerNavKey
  val currentTopLevel = resolveTopLevelDestination(currentDestination)
  val showNav = isAuthenticated && isTopLevelDestination(currentDestination)

  // Resolve toolbar actions from the current route's declaration
  val topBarActions: TopBarActions = currentDestination?.topBarActions ?: TopBarActions.None
  val canNavigateBack = topBarActions.showBackButton && navigator.backStack.size > 1

  // Title resolution: use JobApplicationsTrackerNavKey.titleRes for all routes
  val titleText: String = when (currentDestination) {
    is JobApplicationCompanyDetailsRoute -> stringResource(currentDestination.titleRes)
    is JobApplicationDetailRoute -> stringResource(currentDestination.titleRes)
    is JobApplicationAddApplicationRoute -> stringResource(JobApplicationAddApplicationRoute.titleRes)
    is JobApplicationAddLeadRoute -> stringResource(JobApplicationAddLeadRoute.titleRes)
    else -> currentDestination?.let { stringResource(it.titleRes) }
      ?: stringResource(JobApplicationListRoute.titleRes)
  }

  val topLevelDestinations = remember {
    listOf(
      TopLevelDestinationInfo(
        enumValue = TopLevelDestination.Applications,
        route = JobApplicationListRoute,
        labelRes = Res.string.app_nav_applications,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescriptionRes = Res.string.app_nav_applications_cd,
      ),
      TopLevelDestinationInfo(
        enumValue = TopLevelDestination.Companies,
        route = JobApplicationCompaniesRoute,
        labelRes = Res.string.app_nav_companies,
        selectedIcon = Icons.Filled.Business,
        unselectedIcon = Icons.Outlined.Business,
        contentDescriptionRes = Res.string.app_nav_companies_cd,
      ),
      TopLevelDestinationInfo(
        enumValue = TopLevelDestination.Apply,
        route = AiApplyRoute,
        labelRes = Res.string.app_nav_apply,
        selectedIcon = Icons.AutoMirrored.Filled.Send,
        unselectedIcon = Icons.AutoMirrored.Outlined.Send,
        contentDescriptionRes = Res.string.app_nav_apply_cd,
      ),
    )
  }

  // Shared actions slot: child screens populate this via the content lambda,
  // and the TopAppBar reads it.
  val childActionsSlot = remember { TopBarActionsSlot() }

  // Shared title summary slot: child screens populate summary content,
  // and the TopAppBar title wraps with an animated chevron dropdown.
  val titleSummarySlot = remember { TopBarTitleSummarySlot() }
  var isTitleDropdownExpanded by remember { mutableStateOf(false) }

  // Clear custom actions and summaries when the current route changes
  LaunchedEffect(currentDestination) {
    if (!topBarActions.hasCustomActions) {
      childActionsSlot.render.value = {}
    }
    titleSummarySlot.summary.value = null
    isTitleDropdownExpanded = false
  }

  Scaffold(
    modifier = modifier,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          val summaryContent = titleSummarySlot.summary.value
          if (summaryContent != null) {
            TopBarTitleDropdown(
              title = titleText,
              isExpanded = isTitleDropdownExpanded,
              onToggle = { isTitleDropdownExpanded = !isTitleDropdownExpanded },
              onDismiss = { isTitleDropdownExpanded = false },
              dropdownContent = {
                summaryContent { isTitleDropdownExpanded = false }
              },
            )
          } else {
            Text(
              text = titleText,
              style = MaterialTheme.typography.titleLarge,
            )
          }
        },
        navigationIcon = {
          if (canNavigateBack) {
            IconButton(onClick = { appNavigator.popBackStack() }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.app_action_back),
              )
            }
          }
        },
        actions = {
          // Render custom actions when the current route declares them
          if (topBarActions.hasCustomActions) {
            childActionsSlot.render.value()
          }
          // Render settings icon when the current route declares it
          if (topBarActions.showSettingsIcon) {
            IconButton(onClick = { appNavigator.navigateToSettings() }) {
              Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(Res.string.app_nav_settings_cd),
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
            val selected = currentTopLevel == destination.enumValue
            NavigationBarItem(
              selected = selected,
              onClick = {
                if (!selected) {
                  appNavigator.navigateTo(destination.enumValue)
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
            val selected = currentTopLevel == destination.enumValue
            NavigationRailItem(
              selected = selected,
              onClick = {
                if (!selected) {
                  appNavigator.navigateTo(destination.enumValue)
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
          CompositionLocalProvider(
            LocalTopBarActionsSlot provides childActionsSlot,
            LocalTopBarTitleSummarySlot provides titleSummarySlot,
          ) {
            content(PaddingValues())
          }
        }
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding),
      ) {
        CompositionLocalProvider(
          LocalTopBarActionsSlot provides childActionsSlot,
          LocalTopBarTitleSummarySlot provides titleSummarySlot,
        ) {
          content(PaddingValues())
        }
      }
    }
  }
}
