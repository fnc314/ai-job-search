package dev.fnc314.tools.jobtracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.fnc314.tools.jobtracker.di.appModule
import dev.fnc314.tools.jobtracker.ui.add.AddJobApplicationScreen
import dev.fnc314.tools.jobtracker.core.designsystem.JobTrackerTheme
import dev.fnc314.tools.jobtracker.ui.detail.JobApplicationDetailScreen
import dev.fnc314.tools.jobtracker.ui.edits.TrackerEditsScreen
import dev.fnc314.tools.jobtracker.ui.lead.AddLeadScreen
import dev.fnc314.tools.jobtracker.ui.list.JobApplicationsListScreen
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

enum class AddMode { Application, Lead }
enum class ScreenRoute { List, Detail, AddApplication, AddLead, Edits }

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
                var selectedAppId: String? by remember { mutableStateOf<String?>(null) }
                var showEdits by remember { mutableStateOf(false) }
                var addMode: AddMode? by remember { mutableStateOf<AddMode?>(null) }

                when {
                    showEdits ->
                        TrackerEditsScreen(
                            onNavigateBack = { showEdits = false },
                            onEditClick = { appId ->
                                showEdits = false
                                selectedAppId = appId
                            },
                            settings = settings,
                            onSettingsChange = { settings = it },
                        )

                    addMode == AddMode.Application ->
                        AddJobApplicationScreen(
                            onNavigateBack = { addMode = null },
                        )

                    addMode == AddMode.Lead ->
                        AddLeadScreen(
                            onNavigateBack = { addMode = null },
                        )

                    selectedAppId != null ->
                        JobApplicationDetailScreen(
                            applicationId = selectedAppId!!,
                            onNavigateBack = { selectedAppId = null },
                        )

                    else ->
                        JobApplicationsListScreen(
                            onApplicationClick = { id -> selectedAppId = id },
                            onAddApplicationClick = { addMode = AddMode.Application },
                            onAddLeadClick = { addMode = AddMode.Lead },
                            onEditsClick = { showEdits = true },
                            settings = settings,
                            onSettingsChange = { settings = it },
                        )
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