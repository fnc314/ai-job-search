package dev.fnc314.aijobtracker.frontend.app.core

import androidx.compose.foundation.isSystemInDarkTheme
import co.touchlab.kermit.platformLogWriter
import dev.fnc314.aijobtracker.frontend.core.logging.AppLogger
import dev.fnc314.aijobtracker.frontend.core.logging.KmpInspectorLogWriter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.fnc314.aijobtracker.frontend.app.core.di.MainApplication
import dev.fnc314.aijobtracker.frontend.app.core.di.appModule
import dev.fnc314.aijobtracker.frontend.app.core.impl.AppNavigatorImpl
import dev.fnc314.aijobtracker.frontend.app.core.ui.AppScaffold
import dev.fnc314.aijobtracker.frontend.core.data.model.AuthState
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettingsRepository
import dev.fnc314.aijobtracker.frontend.core.designsystem.JobTrackerTheme
import dev.fnc314.aijobtracker.frontend.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.aijobtracker.frontend.core.designsystem.SeedColors
import dev.fnc314.aijobtracker.frontend.core.designsystem.toColorOrNull
import dev.fnc314.aijobtracker.frontend.core.widgets.ProvideImageLoader
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.Navigator
import dev.fnc314.aijobtracker.frontend.core.navigation.ProvideAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.TopLevelDestination
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.AddJobApplicationEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddApplicationRoute
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddLeadRoute
import dev.fnc314.aijobtracker.frontend.features.artifactpreviewer.ui.ArtifactPreviewerEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.artifactpreviewer.ui.ArtifactPreviewRoute
import dev.fnc314.aijobtracker.frontend.features.trackeredits.ui.JobApplicationEditsRoute
import dev.fnc314.aijobtracker.frontend.features.trackeredits.ui.TrackerEditsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.CompaniesEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.JobApplicationCompaniesRoute
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.CompanyDetailsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.JobApplicationCompanyDetailsRoute
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.JobApplicationDetailRoute
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.JobApplicationDetailsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationListRoute
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.settings.ui.JobApplicationSettingsRoute
import dev.fnc314.aijobtracker.frontend.features.settings.ui.SettingsEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.signin.data.repository.AuthRepository
import dev.fnc314.aijobtracker.frontend.features.signin.ui.SignInEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.signin.ui.SignInRoute
import dev.fnc314.aijobtracker.frontend.features.aiapply.ui.AiApplyEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.aiapply.ui.AiApplyRoute
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration
import androidx.navigation3.runtime.NavKey

private val navigationSavedStateConfig = SavedStateConfiguration {
  serializersModule = SerializersModule {
    polymorphic(NavKey::class) {
      subclass(SignInRoute::class, SignInRoute.serializer())
      subclass(JobApplicationListRoute::class, JobApplicationListRoute.serializer())
      subclass(JobApplicationCompaniesRoute::class, JobApplicationCompaniesRoute.serializer())
      subclass(JobApplicationCompanyDetailsRoute::class, JobApplicationCompanyDetailsRoute.serializer())
      subclass(JobApplicationDetailRoute::class, JobApplicationDetailRoute.serializer())
      subclass(JobApplicationAddApplicationRoute::class, JobApplicationAddApplicationRoute.serializer())
      subclass(JobApplicationAddLeadRoute::class, JobApplicationAddLeadRoute.serializer())
      subclass(JobApplicationSettingsRoute::class, JobApplicationSettingsRoute.serializer())
      subclass(AiApplyRoute::class, AiApplyRoute.serializer())
      subclass(ArtifactPreviewRoute::class, ArtifactPreviewRoute.serializer())
    }
  }
}

/**
 * Root multiplatform application entry composable.
 *
 * Bootstraps Koin dependency injection via [appModule], loads visual appearance settings,
 * monitors authentication status, and coordinates navigation across all feature modules.
 */
@Composable
public fun App() {
  // Initialize Kermit to route logs to both platform loggers and KMP Inspector
  AppLogger.init(platformLogWriter(), KmpInspectorLogWriter())
  KoinApplication(
    configuration =
      koinConfiguration<MainApplication> {
        modules(appModule)
      },
  ) {
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.settings.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = settings.isDark(systemDark)

    val seedColor =
      settings.seedColorHex.toColorOrNull()
        ?: SeedColors.ProfessionalBlue

    // Observe auth state from the AuthRepository singleton
    val authRepository = koinInject<AuthRepository>()

    // Initialize auth state on app start (restores Firebase auth session)
    // Use rememberSaveable so auth init is skipped on config-change recompositions
    var authInitialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
      authRepository.initAuthState()
      authInitialized = true
    }
    if (!authInitialized) return@KoinApplication

    val authState by authRepository.authState.collectAsState()
    val isAuthenticated = authState is AuthState.Authenticated

    // Track whether login() has been called for the current auth session
    // to prevent switchRoot from firing on config-change recompositions
    var loginCalled by rememberSaveable { mutableStateOf(false) }

    val initialRoute: JobApplicationsTrackerNavKey = if (isAuthenticated) JobApplicationListRoute else SignInRoute
    val backStack = rememberNavBackStack(navigationSavedStateConfig, initialRoute)
    val navigator = remember(backStack) { Navigator(backStack) }

    // Route resolver maps TopLevelDestination to actual JobApplicationsTrackerNavKey instances
    val routeResolver: (TopLevelDestination) -> JobApplicationsTrackerNavKey = { dest ->
      when (dest) {
        TopLevelDestination.Applications -> JobApplicationListRoute
        TopLevelDestination.Companies -> JobApplicationCompaniesRoute
        TopLevelDestination.Apply -> AiApplyRoute
        TopLevelDestination.Edits -> JobApplicationEditsRoute
        TopLevelDestination.Settings -> JobApplicationSettingsRoute
      }
    }
    val appNavigator = remember(navigator) {
      AppNavigatorImpl(navigator, routeResolver)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Collect all feature entry registries from Koin by concrete type
    val entryRegistries = listOf<FeatureEntryRegistry>(
      koinInject<SignInEntryRegistry>(),
      koinInject<JobApplicationsEntryRegistry>(),
      koinInject<JobApplicationDetailsEntryRegistry>(),
      koinInject<CompaniesEntryRegistry>(),
      koinInject<CompanyDetailsEntryRegistry>(),
      koinInject<SettingsEntryRegistry>(),
      koinInject<AddJobApplicationEntryRegistry>(),
      koinInject<AiApplyEntryRegistry>(),
      koinInject<ArtifactPreviewerEntryRegistry>(),
      koinInject<TrackerEditsEntryRegistry>(),
    )

    // React to authentication changes
    // Guard login() so it only fires during the initial auth transition, not on
    // every config-change recomposition when authState re-emits.
    LaunchedEffect(isAuthenticated) {
      if (isAuthenticated) {
        if (!loginCalled) {
          appNavigator.login()
          loginCalled = true
        }
      } else {
        loginCalled = false
        appNavigator.logout()
      }
    }

    JobTrackerTheme(
      darkTheme = isDark,
      seedColor = seedColor,
    ) {
      ProvideImageLoader {
        ProvideWindowSizeClass {
          ProvideAppNavigator(appNavigator) {
            val appContent: @Composable () -> Unit = {
              Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
              ) {
                AppScaffold(
                  navigator = navigator,
                  isAuthenticated = isAuthenticated,
                  snackbarHostState = snackbarHostState,
                ) { innerPadding ->
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(innerPadding)
                  ) {
                    NavDisplay(
                      backStack = navigator.backStack,
                      onBack = { appNavigator.popBackStack() },
                      entryProvider = entryProvider {
                        entryRegistries.forEach { it(this) }
                      },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              }
            }

            LaunchedEffect(settings.enableKmpInspector) {
              setInspectorEnabled(settings.enableKmpInspector)
            }
            KmpInspector(enabled = settings.enableKmpInspector, content = appContent)
          }
        }
      }
    }
  }
}
