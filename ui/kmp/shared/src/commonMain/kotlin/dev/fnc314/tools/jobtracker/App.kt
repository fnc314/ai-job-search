package dev.fnc314.tools.jobtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import dev.fnc314.tools.jobtracker.di.appModule
import dev.fnc314.tools.jobtracker.ui.add.AddJobApplicationScreen
import dev.fnc314.tools.jobtracker.ui.component.JobTrackerTheme
import dev.fnc314.tools.jobtracker.ui.detail.JobApplicationDetailScreen
import dev.fnc314.tools.jobtracker.ui.edits.TrackerEditsScreen
import dev.fnc314.tools.jobtracker.ui.lead.AddLeadScreen
import dev.fnc314.tools.jobtracker.ui.list.JobApplicationsListScreen
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import org.koin.compose.KoinApplication

enum class AddMode { Application, Lead }
enum class ScreenRoute { List, Detail, AddApplication, AddLead, Edits }

@Composable
fun App() {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        var settings by remember { mutableStateOf(AppSettings()) }
        
        JobTrackerTheme(
            darkTheme = settings.darkTheme,
        ) {
            Surface(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                var selectedAppId: String? by remember { mutableStateOf<String?>(null) }
                var showEdits by remember { mutableStateOf(false) }
                var addMode: AddMode? by remember { mutableStateOf<AddMode?>(null) }
                var fabMenuExpanded by remember { mutableStateOf(false) }
                val focusManager = LocalFocusManager.current

                when {
                    showEdits ->
                        TrackerEditsScreen(
                            onNavigateBack = { showEdits = false },
                            onEditClick = { appId ->
                                showEdits = false
                                selectedAppId = appId
                            },
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
                        Box(modifier = Modifier.fillMaxSize()) {
                            JobApplicationsListScreen(
                                onApplicationClick = { id -> selectedAppId = id },
                                onAddClick = {
                                    focusManager.clearFocus()
                                    fabMenuExpanded = true
                                },
                                onEditsClick = { showEdits = true },
                            )
                            Box(
                                modifier = Modifier.matchParentSize(),
                            ) {
                                DropdownMenu(
                                    expanded = fabMenuExpanded,
                                    onDismissRequest = { fabMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Add Application") },
                                        onClick = {
                                            addMode = AddMode.Application
                                            fabMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Add Lead") },
                                        onClick = {
                                            addMode = AddMode.Lead
                                            fabMenuExpanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pending Edits") },
                                        onClick = {
                                            showEdits = true
                                            fabMenuExpanded = false
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