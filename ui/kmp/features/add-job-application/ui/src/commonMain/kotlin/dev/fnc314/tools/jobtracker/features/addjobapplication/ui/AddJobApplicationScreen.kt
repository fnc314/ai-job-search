package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.NotesWidget
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddJobApplicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddJobApplicationViewModel = koinViewModel(),
): Unit {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AddJobApplicationUiState.NavigateBack -> onNavigateBack()
            is AddJobApplicationUiState.Success -> onNavigateBack()
            else -> Unit
        }
    }

    var rememberedForm by remember { mutableStateOf(AddJobApplicationFormUiModel()) }
    if (uiState is AddJobApplicationUiState.Editing) {
        rememberedForm = (uiState as AddJobApplicationUiState.Editing).form
    }

    val isSubmitting = uiState is AddJobApplicationUiState.Submitting
    val errorMessage = when (val state = uiState) {
        is AddJobApplicationUiState.Error -> state.message
        is AddJobApplicationUiState.Editing -> state.form.validationError
        else -> null
    }

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
                    OutlinedButton(
                        onClick = { viewModel.onIntent(AddJobApplicationUiIntent.ClickBack) },
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { viewModel.onIntent(AddJobApplicationUiIntent.Submit) },
                        enabled = !isSubmitting,
                    ) {
                        Text(if (isSubmitting) "Saving..." else "Save")
                    }
                }
            }

            Text(
                text = "(*) Denotes a required field",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Error message
            errorMessage?.let { error ->
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
                value = rememberedForm.company,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateCompany(it)) },
                label = { Text("Company (*)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Acme Corp") },
                singleLine = true,
                supportingText = { Text("Required") },
            )

            // Role
            OutlinedTextField(
                value = rememberedForm.role,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateRole(it)) },
                label = { Text("Role / Title") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Senior Software Engineer") },
                singleLine = true,
            )

            // Sector
            OutlinedTextField(
                value = rememberedForm.sector,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateSector(it)) },
                label = { Text("Sector") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. SaaS, Fintech") },
                singleLine = true,
            )

            // Role Type
            OutlinedTextField(
                value = rememberedForm.roleType,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateRoleType(it)) },
                label = { Text("Role Type") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Full-time, Contract") },
                singleLine = true,
            )

            // Work Mode dropdown
            WorkModeDropdown(
                selected = rememberedForm.workMode,
                onSelected = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateWorkMode(it)) },
            )

            // Status dropdown
            StatusDropdown(
                selected = rememberedForm.status,
                onSelected = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateStatus(it)) },
            )

            // Channel dropdown
            ChannelDropdown(
                selected = rememberedForm.channel,
                onSelected = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateChannel(it)) },
            )

            // Job URL
            OutlinedTextField(
                value = rememberedForm.jobUrl,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateJobUrl(it)) },
                label = { Text("Job URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://...") },
                singleLine = true,
            )

            // Application URL
            OutlinedTextField(
                value = rememberedForm.jobApplicationUrl,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateJobApplicationUrl(it)) },
                label = { Text("Application URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://...") },
                singleLine = true,
            )

            // Job ID
            OutlinedTextField(
                value = rememberedForm.jobId,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateJobId(it)) },
                label = { Text("Job ID") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. GH-12345") },
                singleLine = true,
            )

            // Location
            OutlinedTextField(
                value = rememberedForm.location,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateLocation(it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. San Francisco, CA") },
                singleLine = true,
            )

            // Tags
            OutlinedTextField(
                value = rememberedForm.tags,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateTags(it)) },
                label = { Text("Tags") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("remote, senior, kotlin (comma-separated)") },
                singleLine = true,
            )

            // Notes
            NotesWidget(
                initialNotes = rememberedForm.notes,
                onNotesChanged = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateNotes(it)) },
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
): Unit {
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
): Unit {
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
): Unit {
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
