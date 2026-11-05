package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateStatusDialog(
    currentStatus: TrackerStatus?,
    onConfirm: (TrackerStatus, String?, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var selectedStatus by remember { mutableStateOf(currentStatus ?: TrackerStatus.APPLIED) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Status") },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.subMedium),
            ) {
                // Status dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .semantics { contentDescription = "Select new status" },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        TrackerStatus.entries.filter { it != TrackerStatus.UNSPECIFIED }.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayName()) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                // Note field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Reason for status change") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val noteValue = note.ifBlank { null }
                    onConfirm(selectedStatus, null, noteValue)
                },
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordEventDialog(
    onConfirm: (TrackerEventType, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var selectedType by remember { mutableStateOf(TrackerEventType.APPLY) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Event") },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.subMedium),
            ) {
                // Event type dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .semantics { contentDescription = "Select event type" },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        TrackerEventType.entries.filter { it != TrackerEventType.UNSPECIFIED }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                // Note field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Event details") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val noteValue = note.ifBlank { null }
                    onConfirm(selectedType, noteValue)
                },
            ) {
                Text("Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun UpdateFitScoreDialog(
    currentScore: Int? = null,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var score by remember(currentScore) { mutableIntStateOf(currentScore ?: 50) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Fit Score") },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = "Score: $score",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Slider(
                    value = score.toFloat(),
                    onValueChange = { score = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 99,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Fit score slider" },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("0", style = MaterialTheme.typography.bodySmall)
                    Text("100", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(score) }) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun MarkdownPreviewDialog(
    artifact: TrackerArtifact,
    onDismiss: () -> Unit,
): Unit {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = artifact.filename,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.subMedium),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.subMedium)) {
                        Text(
                            text = "Kind: ${artifact.kind.displayName()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        artifact.localPath?.let {
                            Text(
                                text = "Path: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = "Markdown Document Content",
                    style = MaterialTheme.typography.titleMedium,
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = MaterialTheme.componentSizes.hairline,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "# ${artifact.filename}\n\nDocument rendered internally according to App Preferences.\n\nType: ${artifact.kind}\nStored: ${artifact.storage ?: artifact.localPath ?: "Remote"}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(MaterialTheme.spacing.subMedium),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
internal fun EventDetailDialog(
    event: TrackerEvent,
    onDismiss: () -> Unit,
): Unit {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Event: ${event.type.displayName()}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.subMedium),
            ) {
                // Event Metadata Card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        Text(
                            text = "Timestamp: ${event.createdAt?.toString() ?: "N/A"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Actor: ${event.actor}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Source: ${event.derivedFrom}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Confidence: ${event.confidence}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Status transition if applicable
                if (event.statusFrom != null || event.statusTo != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.subMedium)) {
                            Text(
                                text = "Affected State Transition",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                            Text(
                                text = "${event.statusFrom?.displayName() ?: "Initial"} → ${event.statusTo?.displayName() ?: "Unchanged"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                // Linked email ID
                event.emailMessageId?.let { emailId ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.subMedium)) {
                            Text(
                                text = "Linked Email Message",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.micro))
                            Text(
                                text = emailId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }

                // Note
                event.note?.let { note ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        tonalElevation = MaterialTheme.componentSizes.hairline,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.subMedium)) {
                            Text(
                                text = "Note / Event Payload",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
