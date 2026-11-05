package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.NotesWidget
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AddLeadScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddLeadViewModel = koinViewModel(),
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
                    text = "Add Lead",
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

                // Contact name
                OutlinedTextField(
                    value = formState.contact,
                    onValueChange = viewModel::updateContact,
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Jane Smith") },
                    singleLine = true,
                )

                // Channel
                OutlinedTextField(
                    value = formState.channel,
                    onValueChange = viewModel::updateChannel,
                    label = { Text("Channel") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. LinkedIn, Referral, Direct") },
                    singleLine = true,
                )

                // Notes
                NotesWidget(
                    initialNotes = formState.notes,
                    onNotesChanged = viewModel::updateNotes,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Tags
                OutlinedTextField(
                    value = formState.tags,
                    onValueChange = viewModel::updateTags,
                    label = { Text("Tags") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("recruiter, outbound, kotlin (comma-separated)") },
                    singleLine = true,
                )

                // Spacing well below the last item for keyboard clearance
                Spacer(modifier = Modifier.height(MaterialTheme.componentSizes.formBottomClearance))
            }
        }
    }
