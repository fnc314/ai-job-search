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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.fnc314.tools.jobtracker.app.core.di.MainApplication
import dev.fnc314.tools.jobtracker.app.core.di.appModule
import dev.fnc314.tools.jobtracker.app.core.ui.AppScaffold
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.core.widgets.ProvideImageLoader
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import dev.fnc314.tools.jobtracker.core.navigation.Navigator
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddApplicationRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.addJobApplicationNavHook
import dev.fnc314.tools.jobtracker.features.companieslist.ui.CompaniesListRoute
import dev.fnc314.tools.jobtracker.features.companieslist.ui.companiesListNavHook
import dev.fnc314.tools.jobtracker.features.companydetails.ui.CompanyDetailsRoute
import dev.fnc314.tools.jobtracker.features.companydetails.ui.companyDetailsNavHook
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.DetailRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.jobApplicationDetailsNavHook
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.ListRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.jobApplicationsListNavHook
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsRoute
import dev.fnc314.tools.jobtracker.features.settings.ui.settingsNavHook
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInRoute
import dev.fnc314.tools.jobtracker.features.signin.ui.signInNavHook
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.EditsRoute
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.trackerEditsNavHook
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
      subclass(ListRoute::class, ListRoute.serializer())
      subclass(CompaniesListRoute::class, CompaniesListRoute.serializer())
      subclass(CompanyDetailsRoute::class, CompanyDetailsRoute.serializer())
      subclass(DetailRoute::class, DetailRoute.serializer())
      subclass(AddApplicationRoute::class, AddApplicationRoute.serializer())
      subclass(AddLeadRoute::class, AddLeadRoute.serializer())
      subclass(SettingsRoute::class, SettingsRoute.serializer())
      subclass(EditsRoute::class, EditsRoute.serializer())
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
    var authInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
      authRepository.initAuthState()
      authInitialized = true
    }
    if (!authInitialized) return@KoinApplication

    val authState by authRepository.authState.collectAsState()
    val isAuthenticated = authState is AuthState.Authenticated

    val initialRoute: NavKey = if (isAuthenticated) ListRoute else SignInRoute
    val backStack = rememberNavBackStack(navigationSavedStateConfig, initialRoute)
    val navigator = remember(backStack) { Navigator(backStack) }
    val snackbarHostState = remember { SnackbarHostState() }

    // React to authentication changes
    LaunchedEffect(isAuthenticated) {
      if (isAuthenticated) {
        navigator.switchRoot(ListRoute)
      } else {
        navigator.switchRoot(SignInRoute)
      }
    }

    JobTrackerTheme(
      darkTheme = isDark,
      seedColor = seedColor,
    ) {
      ProvideImageLoader {
        ProvideWindowSizeClass {
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
                onBack = { navigator.popBackStack() },
                entryProvider = entryProvider {
                  signInNavHook(this, navigator)

                  jobApplicationsListNavHook(
                    onApplicationClick = { id -> navigator.navigate(DetailRoute(id)) },
                    onAddApplicationClick = { navigator.navigate(AddApplicationRoute) },
                    onAddLeadClick = { navigator.navigate(AddLeadRoute) },
                    onEditsClick = { navigator.navigate(EditsRoute) },
                    onSettingsClick = { navigator.navigate(SettingsRoute) },
                    settings = settings,
                    onSettingsChange = { settingsRepository.updateSettings(it) },
                  )(this, navigator)

                  companiesListNavHook(
                    onCompanyClick = { id, name, website ->
                      navigator.navigate(
                        CompanyDetailsRoute(
                          companyId = id,
                          companyName = name,
                          website = website,
                        ),
                      )
                    },
                  )(this, navigator)

                  companyDetailsNavHook(
                    onNavigateBack = { navigator.popBackStack() },
                    onNavigateToApplication = { id ->
                      navigator.navigate(DetailRoute(applicationId = id))
                    },
                    onOpenUrl = { url -> PlatformFileOpener().openUrl(url) },
                  )(this, navigator)

                  jobApplicationDetailsNavHook(
                    settings = settings,
                    onNavigateBack = { navigator.popBackStack() },
                  )(this, navigator)

                  addJobApplicationNavHook(
                    onNavigateBack = { navigator.popBackStack() },
                  )(this, navigator)

                  trackerEditsNavHook(
                    onNavigateBack = { navigator.popBackStack() },
                    onEditClick = { appId -> navigator.navigate(DetailRoute(appId)) },
                    settings = settings,
                    onSettingsChange = { settingsRepository.updateSettings(it) },
                  )(this, navigator)

                  settingsNavHook(
                    settings = settings,
                    onSettingsChange = { settingsRepository.updateSettings(it) },
                    onNavigateBack = { navigator.popBackStack() },
                    onSignOut = {
                      navigator.switchRoot(SignInRoute)
                    },
                  )(this, navigator)
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
