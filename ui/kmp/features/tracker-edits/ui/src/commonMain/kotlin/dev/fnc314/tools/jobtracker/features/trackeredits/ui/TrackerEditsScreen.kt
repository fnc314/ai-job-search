package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TrackerEditsScreen(
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    viewModel: TrackerEditsViewModel = koinViewModel(),
): Unit {
    val uiState: TrackerEditsUiState by viewModel.uiState.collectAsState()
    var rejectDialogItem: TrackerEditItemUiModel? by remember { mutableStateOf(null) }
    var rejectReason: String by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is TrackerEditsUiState.NavigateBack) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Toolbar row: Back, Title & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                IconButton(onClick = { viewModel.onIntent(TrackerEditsUiIntent.ClickBack) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = "Pending Edits",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                val isActionInProgress = (uiState as? TrackerEditsUiState.Content)?.isActionInProgress == true
                val isLoading = uiState is TrackerEditsUiState.Loading

                TooltipIconButton(
                    tooltip = "Expire stale edits",
                    icon = Icons.Default.DateRange,
                    onClick = { viewModel.onIntent(TrackerEditsUiIntent.ExpireStaleEdits) },
                    enabled = !isActionInProgress && !isLoading,
                )
                TooltipIconButton(
                    tooltip = "Refresh edits",
                    icon = Icons.Default.Refresh,
                    onClick = { viewModel.onIntent(TrackerEditsUiIntent.Refresh) },
                    enabled = !isLoading && !isActionInProgress,
                )
            }
        }

        when (val state = uiState) {
            is TrackerEditsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is TrackerEditsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { viewModel.onIntent(TrackerEditsUiIntent.Refresh) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            is TrackerEditsUiState.Content -> {
                if (state.edits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.medium),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No pending edits",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                    ) {
                        items(
                            items = state.edits,
                            key = { item -> item.id },
                        ) { item ->
                            EditCard(
                                item = item,
                                isActionInProgress = state.isActionInProgress,
                                onApprove = {
                                    viewModel.onIntent(item.approveIntent)
                                },
                                onReject = {
                                    rejectDialogItem = item
                                },
                                onViewApplication = { onEditClick(item.applicationId) },
                            )
                        }
                    }
                }
            }
            is TrackerEditsUiState.NavigateBack -> {
                // Navigation side effect dispatched in LaunchedEffect
            }
        }
    }

    // Reject confirmation dialog
    rejectDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = {
                rejectDialogItem = null
                rejectReason = ""
            },
            title = { Text("Reject Edit") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Text("Enter a reason for rejecting this edit:")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Why are you rejecting this edit?") },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            viewModel.onIntent(item.rejectIntent.copy(note = rejectReason))
                            rejectDialogItem = null
                            rejectReason = ""
                        }
                    },
                    enabled = rejectReason.isNotBlank(),
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rejectDialogItem = null
                        rejectReason = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
internal fun EditCard(
    item: TrackerEditItemUiModel,
    isActionInProgress: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewApplication: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            // Header: kind + confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.kindDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Confidence: ${item.confidenceDisplayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Timestamp if available
            if (item.timestamp.isNotBlank()) {
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Application ID
            Text(
                text = "Application: ${item.applicationId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { contentDescription = "View application ${item.applicationId}" }
                    .clickableArea(onClick = onViewApplication),
            )

            // Changes list
            if (item.changes.isNotEmpty()) {
                Text(
                    text = "Changes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.changes.forEach { change ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(MaterialTheme.spacing.small),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
                        ) {
                            Text(
                                text = change.field,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = change.changeDescription,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            change.reason?.let { reasonText ->
                                Text(
                                    text = "Reason: $reasonText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else if (item.changeSummary.isNotBlank()) {
                Text(
                    text = item.changeSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Source email info
            item.sourceEmail?.let { email ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
                    ) {
                        Text(
                            text = "From: ${email.from}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Subject: ${email.subject}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                FilledTonalButton(
                    onClick = onApprove,
                    enabled = !isActionInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Approve this edit" },
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isActionInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Reject this edit" },
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
private fun Modifier.clickableArea(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            onClick = onClick,
            onClickLabel = null,
            role = Role.Button,
        ),
    )
