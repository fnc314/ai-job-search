package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.fnc314.tools.jobtracker.app.core.di.MainApplication
import dev.fnc314.tools.jobtracker.app.core.di.appModule
import dev.fnc314.tools.jobtracker.app.core.impl.AppNavigatorImpl
import dev.fnc314.tools.jobtracker.app.core.ui.AppScaffold
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.core.widgets.ProvideImageLoader
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.tools.jobtracker.core.navigation.Navigator
import dev.fnc314.tools.jobtracker.core.navigation.ProvideAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.TopLevelDestinationEnum
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddJobApplicationEntryRegistry
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.JobApplicationAddApplicationRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.JobApplicationAddLeadRoute
import dev.fnc314.tools.jobtracker.features.companieslist.ui.CompaniesEntryRegistry
import dev.fnc314.tools.jobtracker.features.companieslist.ui.JobApplicationCompaniesRoute
import dev.fnc314.tools.jobtracker.features.companydetails.ui.CompanyDetailsEntryRegistry
import dev.fnc314.tools.jobtracker.features.companydetails.ui.JobApplicationCompanyDetailsRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.JobApplicationDetailRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.JobApplicationDetailsEntryRegistry
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationListRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsEntryRegistry
import dev.fnc314.tools.jobtracker.features.settings.ui.JobApplicationSettingsRoute
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsEntryRegistry
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInEntryRegistry
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInRoute
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.JobApplicationEditsRoute
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.TrackerEditsEntryRegistry
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

private val navigationSavedStateConfig = SavedStateConfiguration {
  serializersModule = SerializersModule {
    polymorphic(androidx.navigation3.runtime.NavKey::class) {
      subclass(SignInRoute::class, SignInRoute.serializer())
      subclass(JobApplicationListRoute::class, JobApplicationListRoute.serializer())
      subclass(JobApplicationCompaniesRoute::class, JobApplicationCompaniesRoute.serializer())
      subclass(JobApplicationCompanyDetailsRoute::class, JobApplicationCompanyDetailsRoute.serializer())
      subclass(JobApplicationDetailRoute::class, JobApplicationDetailRoute.serializer())
      subclass(JobApplicationAddApplicationRoute::class, JobApplicationAddApplicationRoute.serializer())
      subclass(JobApplicationAddLeadRoute::class, JobApplicationAddLeadRoute.serializer())
      subclass(JobApplicationSettingsRoute::class, JobApplicationSettingsRoute.serializer())
      subclass(JobApplicationEditsRoute::class, JobApplicationEditsRoute.serializer())
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

    // Route resolver maps TopLevelDestinationEnum to actual JobApplicationsTrackerNavKey instances
    val routeResolver: (TopLevelDestinationEnum) -> JobApplicationsTrackerNavKey = { dest ->
      when (dest) {
        TopLevelDestinationEnum.Applications -> JobApplicationListRoute
        TopLevelDestinationEnum.Companies -> JobApplicationCompaniesRoute
        TopLevelDestinationEnum.Edits -> JobApplicationEditsRoute
        TopLevelDestinationEnum.Settings -> JobApplicationSettingsRoute
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
                ) { _ ->
                  NavDisplay(
                    backStack = navigator.backStack,
                    onBack = { appNavigator.popBackStack() },
                    entryProvider = entryProvider {
                      entryRegistries.forEach { it.register(this) }
                    },
                    modifier = Modifier.fillMaxSize(),
                  )
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
