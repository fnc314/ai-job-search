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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.fnc314.tools.jobtracker.app.core.di.MainApplication
import dev.fnc314.tools.jobtracker.app.core.di.appModule
import dev.fnc314.tools.jobtracker.app.core.ui.AppScaffold
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddApplicationRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.addJobApplicationNavHook
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
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

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

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // React to authentication changes
    LaunchedEffect(isAuthenticated) {
      if (isAuthenticated) {
        navController.navigate(ListRoute) {
          popUpTo(SignInRoute) { inclusive = true }
        }
      } else {
        navController.navigate(SignInRoute) {
          popUpTo(0) { inclusive = true }
        }
      }
    }

    JobTrackerTheme(
      darkTheme = isDark,
      seedColor = seedColor,
    ) {
      ProvideWindowSizeClass {
        KmpInspector {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
          ) {
            AppScaffold(
              navController = navController,
              isAuthenticated = isAuthenticated,
              snackbarHostState = snackbarHostState,
            ) { _ ->
              NavHost(
                navController = navController,
                startDestination = if (isAuthenticated) ListRoute else SignInRoute,
                modifier = Modifier.fillMaxSize(),
              ) {
                signInNavHook(this, navController)

                jobApplicationsListNavHook(
                  onApplicationClick = { id -> navController.navigate(DetailRoute(id)) },
                  onAddApplicationClick = { navController.navigate(AddApplicationRoute) },
                  onAddLeadClick = { navController.navigate(AddLeadRoute) },
                  onEditsClick = { navController.navigate(EditsRoute) },
                  onSettingsClick = { navController.navigate(SettingsRoute) },
                  settings = settings,
                  onSettingsChange = { settingsRepository.updateSettings(it) },
                )(this, navController)

                jobApplicationDetailsNavHook(
                  settings = settings,
                  onNavigateBack = { navController.popBackStack() },
                )(this, navController)

                addJobApplicationNavHook(
                  onNavigateBack = { navController.popBackStack() },
                )(this, navController)

                trackerEditsNavHook(
                  onNavigateBack = { navController.popBackStack() },
                  onEditClick = { appId -> navController.navigate(DetailRoute(appId)) },
                  settings = settings,
                  onSettingsChange = { settingsRepository.updateSettings(it) },
                )(this, navController)

                settingsNavHook(
                  settings = settings,
                  onSettingsChange = { settingsRepository.updateSettings(it) },
                  onNavigateBack = { navController.popBackStack() },
                  onSignOut = {
                    navController.navigate(SignInRoute) {
                      popUpTo(0) { inclusive = true }
                    }
                  },
                )(this, navController)
              }
            }
          }
        }
      }
    }
  }
}
