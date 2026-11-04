package dev.fnc314.tools.jobtracker.ui.edits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.data.model.TrackerEditKind
import dev.fnc314.tools.jobtracker.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings
import dev.fnc314.tools.jobtracker.util.displayName
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerEditsScreen(
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    viewModel: TrackerEditsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var rejectDialogEdit by remember { mutableStateOf<TrackerEdit?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Edits") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Back to list" },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.expireStaleEdits() },
                        enabled = !uiState.actionInProgress,
                        modifier = Modifier.semantics { contentDescription = "Expire stale edits" },
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Expire stale edits",
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadEdits() },
                        enabled = !uiState.loading,
                        modifier = Modifier.semantics { contentDescription = "Refresh edits" },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh edits",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            when {
                uiState.loading && uiState.edits.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.edits.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No pending edits",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(uiState.edits, key = { it.id }) { edit ->
                            EditCard(
                                edit = edit,
                                onApprove = {
                                    viewModel.approveEdit(edit.id)
                                },
                                onReject = {
                                    rejectDialogEdit = edit
                                },
                                onViewApplication = { onEditClick(edit.applicationId) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Reject confirmation dialog
    rejectDialogEdit?.let { edit ->
        AlertDialog(
            onDismissRequest = { rejectDialogEdit = null },
            title = { Text("Reject Edit") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            viewModel.rejectEdit(edit.id, rejectReason)
                            rejectDialogEdit = null
                            rejectReason = ""
                        }
                    },
                    enabled = rejectReason.isNotBlank(),
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectDialogEdit = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun EditCard(
    edit: TrackerEdit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewApplication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header: kind + confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = edit.kind.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
                Text(
                    text = "Confidence: ${edit.confidence.displayName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Application ID
            Text(
                text = "Application: ${edit.applicationId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { contentDescription = "View application ${edit.applicationId}" }
                    .clickableArea(onClick = onViewApplication),
            )

            // Changes list
            if (edit.changes.isNotEmpty()) {
                Text(
                    text = "Changes:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                edit.changes.forEach { change ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = change.field,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            )
                            Text(
                                text = "${change.oldValue} → ${change.newValue}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (change.reason.isNotBlank()) {
                                Text(
                                    text = "Reason: ${change.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Source email info
            edit.sourceEmail?.let { email ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                        .semantics { contentDescription = "Approve this edit" },
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
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
            role = androidx.compose.ui.semantics.Role.Button,
        ),
    )
