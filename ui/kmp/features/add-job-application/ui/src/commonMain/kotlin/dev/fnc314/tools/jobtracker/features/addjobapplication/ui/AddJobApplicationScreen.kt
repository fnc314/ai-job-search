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
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.Res
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_action_cancel
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_action_save_empty
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_action_save_with_company
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_action_saving_empty
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_action_saving_with_company
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_application_url_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_application_url_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_channel_cd
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_channel_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_company_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_company_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_job_id_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_job_id_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_job_url_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_job_url_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_location_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_location_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_required_supporting
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_role_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_role_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_role_type_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_role_type_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_sector_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_sector_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_status_cd
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_status_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_tags_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_tags_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_title
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_work_mode_cd
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_work_mode_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_form_required_hint
import org.jetbrains.compose.resources.stringResource
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
            val saveButtonText: String = if (isSubmitting) {
                if (rememberedForm.company.isNotBlank()) {
                    stringResource(Res.string.add_app_action_saving_with_company, rememberedForm.company)
                } else {
                    stringResource(Res.string.add_app_action_saving_empty)
                }
            } else {
                if (rememberedForm.company.isNotBlank()) {
                    stringResource(Res.string.add_app_action_save_with_company, rememberedForm.company)
                } else {
                    stringResource(Res.string.add_app_action_save_empty)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.add_app_title),
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
                        Text(stringResource(Res.string.add_app_action_cancel))
                    }
                    Button(
                        onClick = { viewModel.onIntent(AddJobApplicationUiIntent.Submit) },
                        enabled = !isSubmitting,
                    ) {
                        Text(saveButtonText)
                    }
                }
            }

            Text(
                text = stringResource(Res.string.add_form_required_hint),
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
                label = { Text(stringResource(Res.string.add_app_company_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_company_placeholder)) },
                singleLine = true,
                supportingText = { Text(stringResource(Res.string.add_app_required_supporting)) },
            )

            // Role
            OutlinedTextField(
                value = rememberedForm.role,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateRole(it)) },
                label = { Text(stringResource(Res.string.add_app_role_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_role_placeholder)) },
                singleLine = true,
            )

            // Sector
            OutlinedTextField(
                value = rememberedForm.sector,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateSector(it)) },
                label = { Text(stringResource(Res.string.add_app_sector_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_sector_placeholder)) },
                singleLine = true,
            )

            // Role Type
            OutlinedTextField(
                value = rememberedForm.roleType,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateRoleType(it)) },
                label = { Text(stringResource(Res.string.add_app_role_type_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_role_type_placeholder)) },
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
                label = { Text(stringResource(Res.string.add_app_job_url_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_job_url_placeholder)) },
                singleLine = true,
            )

            // Application URL
            OutlinedTextField(
                value = rememberedForm.jobApplicationUrl,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateJobApplicationUrl(it)) },
                label = { Text(stringResource(Res.string.add_app_application_url_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_application_url_placeholder)) },
                singleLine = true,
            )

            // Job ID
            OutlinedTextField(
                value = rememberedForm.jobId,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateJobId(it)) },
                label = { Text(stringResource(Res.string.add_app_job_id_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_job_id_placeholder)) },
                singleLine = true,
            )

            // Location
            OutlinedTextField(
                value = rememberedForm.location,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateLocation(it)) },
                label = { Text(stringResource(Res.string.add_app_location_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_location_placeholder)) },
                singleLine = true,
            )

            // Tags
            OutlinedTextField(
                value = rememberedForm.tags,
                onValueChange = { viewModel.onIntent(AddJobApplicationUiIntent.UpdateTags(it)) },
                label = { Text(stringResource(Res.string.add_app_tags_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_app_tags_placeholder)) },
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
    val statusCd: String = stringResource(Res.string.add_app_status_cd)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.add_app_status_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = statusCd },
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
    val channelCd: String = stringResource(Res.string.add_app_channel_cd)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.add_app_channel_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = channelCd },
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
    val workModeCd: String = stringResource(Res.string.add_app_work_mode_cd)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.add_app_work_mode_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .semantics { contentDescription = workModeCd },
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
