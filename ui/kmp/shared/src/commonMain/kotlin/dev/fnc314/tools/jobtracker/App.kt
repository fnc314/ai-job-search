package dev.fnc314.tools.jobtracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.di.appModule
import dev.fnc314.tools.jobtracker.navigation.*
import dev.fnc314.tools.jobtracker.ui.add.AddJobApplicationScreen
import dev.fnc314.tools.jobtracker.ui.detail.JobApplicationDetailScreen
import dev.fnc314.tools.jobtracker.ui.edits.TrackerEditsScreen
import dev.fnc314.tools.jobtracker.ui.lead.AddLeadScreen
import dev.fnc314.tools.jobtracker.ui.list.JobApplicationsListScreen
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import org.koin.compose.KoinApplication
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
        val systemDark = isSystemInDarkTheme()
        var settings by remember { mutableStateOf(AppSettings(darkTheme = systemDark)) }

        val seedColor = settings.seedColorHex.toColorOrNull() ?: Color(0xFF6750A4)

        JobTrackerTheme(
            darkTheme = settings.darkTheme,
            seedColor = seedColor,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val navigator = rememberNavigator(startRoute = ListRoute)

                val entryProvider = remember {
                    navEntryProvider {
                        entry<ListRoute> {
                            JobApplicationsListScreen(
                                onApplicationClick = { id -> navigator.navigateTo(DetailRoute(id)) },
                                onAddApplicationClick = { navigator.navigateTo(AddApplicationRoute) },
                                onAddLeadClick = { navigator.navigateTo(AddLeadRoute) },
                                onEditsClick = { navigator.navigateTo(EditsRoute) },
                                settings = settings,
                                onSettingsChange = { settings = it },
                            )
                        }

                        entry<DetailRoute> { route ->
                            JobApplicationDetailScreen(
                                applicationId = route.applicationId,
                                onNavigateBack = { navigator.goBack() },
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
                                onSettingsChange = { settings = it },
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