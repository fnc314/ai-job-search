package dev.fnc314.tools.jobtracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.di.appModule
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddJobApplicationScreen
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadScreen
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.JobApplicationsDetailsScreen
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListScreen
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsScreen
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInScreen
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.TrackerEditsScreen
import dev.fnc314.tools.jobtracker.navigation.*
import dev.fnc314.tools.jobtracker.ui.AppScaffold
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
public fun App() {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(appModule)
            }
        ),
    ) {
        val settingsRepository = koinInject<AppSettingsRepository>()
        val settings by settingsRepository.settings.collectAsState()
        val systemDark = isSystemInDarkTheme()
        val isDark = settings.isDark(systemDark)

        val seedColor = settings.seedColorHex.toColorOrNull() ?: SeedColors.ProfessionalBlue

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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppScaffold(
                        navController = navController,
                        isAuthenticated = isAuthenticated,
                        snackbarHostState = snackbarHostState,
                    ) { _ ->
                        val signInViewModel = koinViewModel<SignInViewModel>()

                        NavHost(
                            navController = navController,
                            startDestination = if (isAuthenticated) ListRoute else SignInRoute,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            composable<SignInRoute> {
                                SignInScreen(
                                    viewModel = signInViewModel,
                                    onSignInSuccess = {
                                        // Handled by reactive authState LaunchedEffect
                                    },
                                )
                            }

                            composable<ListRoute> {
                                JobApplicationsListScreen(
                                    onApplicationClick = { id ->
                                        navController.navigate(DetailRoute(id))
                                    },
                                    onAddApplicationClick = {
                                        navController.navigate(AddApplicationRoute)
                                    },
                                    onAddLeadClick = {
                                        navController.navigate(AddLeadRoute)
                                    },
                                    onEditsClick = {
                                        navController.navigate(EditsRoute)
                                    },
                                    onSettingsClick = {
                                        navController.navigate(SettingsRoute)
                                    },
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                )
                            }

                            composable<DetailRoute> { backStackEntry ->
                                val detailRoute: DetailRoute = backStackEntry.toRoute()
                                JobApplicationsDetailsScreen(
                                    applicationId = detailRoute.applicationId,
                                    onNavigateBack = { navController.popBackStack() },
                                    settings = settings,
                                )
                            }

                            composable<AddApplicationRoute> {
                                AddJobApplicationScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                )
                            }

                            composable<AddLeadRoute> {
                                AddLeadScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                )
                            }

                            composable<EditsRoute> {
                                TrackerEditsScreen(
                                    onNavigateBack = { navController.popBackStack() },
                                    onEditClick = { appId ->
                                        navController.navigate(DetailRoute(appId))
                                    },
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                    onNavigateBack = { navController.popBackStack() },
                                    signInViewModel = signInViewModel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
