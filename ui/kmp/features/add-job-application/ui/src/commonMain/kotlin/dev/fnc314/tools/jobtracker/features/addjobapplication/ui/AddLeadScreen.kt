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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.NotesWidget
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.Res
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_required_supporting
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_form_required_hint
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_action_cancel
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_action_save_empty
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_action_save_with_company
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_action_saving_empty
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_action_saving_with_company
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_channel_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_channel_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_company_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_company_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_contact_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_contact_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_role_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_role_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_tags_label
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_tags_placeholder
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddLeadScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddLeadViewModel = koinViewModel(),
): Unit {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AddLeadUiState.NavigateBack -> onNavigateBack()
            is AddLeadUiState.Success -> onNavigateBack()
            else -> Unit
        }
    }

    var rememberedForm by remember { mutableStateOf(AddLeadFormUiModel()) }
    if (uiState is AddLeadUiState.Editing) {
        rememberedForm = (uiState as AddLeadUiState.Editing).form
    }

    val isSubmitting = uiState is AddLeadUiState.Submitting
    val errorMessage = when (val state = uiState) {
        is AddLeadUiState.Error -> state.message
        is AddLeadUiState.Editing -> state.form.validationError
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
                    stringResource(Res.string.add_lead_action_saving_with_company, rememberedForm.company)
                } else {
                    stringResource(Res.string.add_lead_action_saving_empty)
                }
            } else {
                if (rememberedForm.company.isNotBlank()) {
                    stringResource(Res.string.add_lead_action_save_with_company, rememberedForm.company)
                } else {
                    stringResource(Res.string.add_lead_action_save_empty)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.add_lead_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onIntent(AddLeadUiIntent.ClickBack) },
                    ) {
                        Text(stringResource(Res.string.add_lead_action_cancel))
                    }
                    Button(
                        onClick = { viewModel.onIntent(AddLeadUiIntent.Submit) },
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
                onValueChange = { viewModel.onIntent(AddLeadUiIntent.UpdateCompany(it)) },
                label = { Text(stringResource(Res.string.add_lead_company_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_lead_company_placeholder)) },
                singleLine = true,
                supportingText = { Text(stringResource(Res.string.add_app_required_supporting)) },
            )

            // Contact name
            OutlinedTextField(
                value = rememberedForm.contact,
                onValueChange = { viewModel.onIntent(AddLeadUiIntent.UpdateContact(it)) },
                label = { Text(stringResource(Res.string.add_lead_contact_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_lead_contact_placeholder)) },
                singleLine = true,
            )

            // Role
            OutlinedTextField(
                value = rememberedForm.role,
                onValueChange = { viewModel.onIntent(AddLeadUiIntent.UpdateRole(it)) },
                label = { Text(stringResource(Res.string.add_lead_role_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_lead_role_placeholder)) },
                singleLine = true,
            )

            // Channel
            OutlinedTextField(
                value = rememberedForm.channel,
                onValueChange = { viewModel.onIntent(AddLeadUiIntent.UpdateChannel(it)) },
                label = { Text(stringResource(Res.string.add_lead_channel_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_lead_channel_placeholder)) },
                singleLine = true,
            )

            // Notes
            NotesWidget(
                initialNotes = rememberedForm.notes,
                onNotesChanged = { viewModel.onIntent(AddLeadUiIntent.UpdateNotes(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Tags
            OutlinedTextField(
                value = rememberedForm.tags,
                onValueChange = { viewModel.onIntent(AddLeadUiIntent.UpdateTags(it)) },
                label = { Text(stringResource(Res.string.add_lead_tags_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.add_lead_tags_placeholder)) },
                singleLine = true,
            )

            // Spacing well below the last item for keyboard clearance
            Spacer(modifier = Modifier.height(MaterialTheme.componentSizes.formBottomClearance))
        }
    }
}
