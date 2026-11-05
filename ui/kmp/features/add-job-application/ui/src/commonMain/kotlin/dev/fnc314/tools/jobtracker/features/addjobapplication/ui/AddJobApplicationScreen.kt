package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.NotesWidget
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddJobApplicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddJobApplicationViewModel = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.componentSizes.formMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        ) {
            // Form header with Cancel and Save actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Add Application",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.submit { onNavigateBack() } },
                        enabled = !formState.submitting,
                    ) {
                        Text(if (formState.submitting) "Saving..." else "Save")
                    }
                }
            }
                Text(
                    text = "(*) Denotes a required field",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Error message
                formState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(MaterialTheme.spacing.subMedium),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // Company (required)
                OutlinedTextField(
                    value = formState.company,
                    onValueChange = viewModel::updateCompany,
                    label = { Text("Company (*)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Acme Corp") },
                    singleLine = true,
                    supportingText = { Text("Required") },
                )

                // Role
                OutlinedTextField(
                    value = formState.role,
                    onValueChange = viewModel::updateRole,
                    label = { Text("Role / Title") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Senior Software Engineer") },
                    singleLine = true,
                )

                // Sector
                OutlinedTextField(
                    value = formState.sector,
                    onValueChange = viewModel::updateSector,
                    label = { Text("Sector") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. SaaS, Fintech") },
                    singleLine = true,
                )

                // Role Type
                OutlinedTextField(
                    value = formState.roleType,
                    onValueChange = viewModel::updateRoleType,
                    label = { Text("Role Type") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Full-time, Contract") },
                    singleLine = true,
                )

                // Work Mode dropdown
                WorkModeDropdown(
                    selected = formState.workMode,
                    onSelected = viewModel::updateWorkMode,
                )

                // Status dropdown
                StatusDropdown(
                    selected = formState.status,
                    onSelected = viewModel::updateStatus,
                )

                // Channel dropdown
                ChannelDropdown(
                    selected = formState.channel,
                    onSelected = viewModel::updateChannel,
                )

                // Job URL
                OutlinedTextField(
                    value = formState.jobUrl,
                    onValueChange = viewModel::updateJobUrl,
                    label = { Text("Job URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    singleLine = true,
                )

                // Application URL
                OutlinedTextField(
                    value = formState.jobApplicationUrl,
                    onValueChange = viewModel::updateJobApplicationUrl,
                    label = { Text("Application URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://...") },
                    singleLine = true,
                )

                // Job ID
                OutlinedTextField(
                    value = formState.jobId,
                    onValueChange = viewModel::updateJobId,
                    label = { Text("Job ID") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. GH-12345") },
                    singleLine = true,
                )

                // Location
                OutlinedTextField(
                    value = formState.location,
                    onValueChange = viewModel::updateLocation,
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. San Francisco, CA") },
                    singleLine = true,
                )

                // Tags
                OutlinedTextField(
                    value = formState.tags,
                    onValueChange = viewModel::updateTags,
                    label = { Text("Tags") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("remote, senior, kotlin (comma-separated)") },
                    singleLine = true,
                )

                // Notes
                NotesWidget(
                    initialNotes = formState.notes,
                    onNotesChanged = viewModel::updateNotes,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Spacing well below the last item for keyboard clearance
                Spacer(modifier = Modifier.height(MaterialTheme.componentSizes.formBottomClearance))
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selected: TrackerStatus,
    onSelected: (TrackerStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = "Select application status" },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TrackerStatus.entries.filter { it != TrackerStatus.UNSPECIFIED }.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName()) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelDropdown(
    selected: ApplicationChannel,
    onSelected: (ApplicationChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Channel") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = "Select application channel" },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ApplicationChannel.entries.filter { it != ApplicationChannel.UNSPECIFIED }.forEach { channel ->
                DropdownMenuItem(
                    text = { Text(channel.displayName()) },
                    onClick = {
                        onSelected(channel)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkModeDropdown(
    selected: WorkMode,
    onSelected: (WorkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Work Mode") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = "Select work mode" },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            WorkMode.entries.filter { it != WorkMode.UNSPECIFIED }.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName()) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}
