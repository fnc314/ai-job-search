package dev.fnc314.tools.jobtracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.core.designsystem.ProvideWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.di.appModule
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInScreen
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import dev.fnc314.tools.jobtracker.navigation.*
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddJobApplicationScreen
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.AddLeadScreen
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.JobApplicationsDetailsScreen
import dev.fnc314.tools.jobtracker.features.trackeredits.ui.TrackerEditsScreen
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListScreen
import dev.fnc314.tools.jobtracker.features.settings.ui.SettingsScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(
                    appModule
                )
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

        JobTrackerTheme(
            darkTheme = isDark,
            seedColor = seedColor,
        ) {
            ProvideWindowSizeClass {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                if (!isAuthenticated) {
                    // Show sign-in screen when not authenticated
                    val signInViewModel = koinViewModel<SignInViewModel>()
                    SignInScreen(
                        viewModel = signInViewModel,
                        onSignInSuccess = {
                            // Auth state change will automatically switch to the main app
                        },
                    )
                } else {
                    // Main app navigation (authenticated)
                    val signInViewModel = koinViewModel<SignInViewModel>()
                    val navigator = rememberNavigator(startRoute = ListRoute)

                    val entryProvider = remember {
                        navEntryProvider {
                            entry<ListRoute> {
                                JobApplicationsListScreen(
                                    onApplicationClick = { id -> navigator.navigateTo(DetailRoute(id)) },
                                    onAddApplicationClick = { navigator.navigateTo(AddApplicationRoute) },
                                    onAddLeadClick = { navigator.navigateTo(AddLeadRoute) },
                                    onEditsClick = { navigator.navigateTo(EditsRoute) },
                                    onSettingsClick = { navigator.navigateTo(SettingsRoute) },
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                )
                            }

                            entry<DetailRoute> { route ->
                                JobApplicationsDetailsScreen(
                                    applicationId = route.applicationId,
                                    onNavigateBack = { navigator.goBack() },
                                    settings = settings,
                                )
                            }

                            entry<AddApplicationRoute> {
                                AddJobApplicationScreen(
                                    onNavigateBack = { navigator.goBack() },
                                )
                            }

                            entry<AddLeadRoute> {
                                AddLeadScreen(
                                    onNavigateBack = { navigator.goBack() },
                                )
                            }

                            entry<EditsRoute> {
                                TrackerEditsScreen(
                                    onNavigateBack = { navigator.goBack() },
                                    onEditClick = { appId ->
                                        navigator.navigateTo(DetailRoute(appId))
                                    },
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                )
                            }

                            entry<SettingsRoute> {
                                SettingsScreen(
                                    settings = settings,
                                    onSettingsChange = { settingsRepository.updateSettings(it) },
                                    onNavigateBack = { navigator.goBack() },
                                    signInViewModel = signInViewModel,
                                )
                            }
                        }
                    }

                    NavDisplay(
                        navigator = navigator,
                        entryProvider = entryProvider,
                        onBack = { navigator.goBack() },
                    )
                }
            }
        }
    }
}
}

private fun String.toColorOrNull(): Color? {
    return try {
        val hex = this.removePrefix("#")
        if (hex.length == 6) {
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            Color(red = r / 255f, green = g / 255f, blue = b / 255f)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}