package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_action_cancel
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_action_close
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_action_record
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_action_update
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_actor
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_confidence
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_detail_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_email_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_na
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_note_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_note_placeholder
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_payload_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_source
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_timestamp
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_transition_initial
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_transition_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_transition_unchanged
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_transition_value
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_type_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_event_type_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_fit_max
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_fit_min
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_fit_score_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_fit_slider_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_fit_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_md_content_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_md_kind
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_md_path
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_status_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_status_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_status_note_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_status_note_placeholder
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.dialog_status_title
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

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

    val selectStatusCd: String = stringResource(Res.string.dialog_status_cd)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_status_title)) },
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
                        label = { Text(stringResource(Res.string.dialog_status_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .semantics { contentDescription = selectStatusCd },
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
                    label = { Text(stringResource(Res.string.dialog_status_note_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.dialog_status_note_placeholder)) },
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
                Text(stringResource(Res.string.dialog_action_update))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordEventDialog(
    onConfirm: (TrackerEventType, String?, Instant?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var selectedType by remember { mutableStateOf(TrackerEventType.APPLY) }
    var note by remember { mutableStateOf("") }
    var scheduledDateTime by remember { mutableStateOf("") }
    val selectEventCd: String = stringResource(Res.string.dialog_event_type_cd)

    val showScheduledAt = selectedType == TrackerEventType.SCHEDULE_INTERVIEW ||
        selectedType == TrackerEventType.VISIT_ONSITE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_event_title)) },
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
                        label = { Text(stringResource(Res.string.dialog_event_type_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                            .semantics { contentDescription = selectEventCd },
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
                    label = { Text(stringResource(Res.string.dialog_event_note_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.dialog_event_note_placeholder)) },
                )

                // Scheduled date-time field (only for interview/onsite events)
                if (showScheduledAt) {
                    OutlinedTextField(
                        value = scheduledDateTime,
                        onValueChange = { scheduledDateTime = it },
                        label = { Text("Scheduled Date & Time") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 2026-11-15T14:00:00Z") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val noteValue = note.ifBlank { null }
                    val scheduledAtValue = if (showScheduledAt && scheduledDateTime.isNotBlank()) {
                        runCatching { Instant.parse(scheduledDateTime) }.getOrNull()
                    } else {
                        null
                    }
                    onConfirm(selectedType, noteValue, scheduledAtValue)
                },
            ) {
                Text(stringResource(Res.string.dialog_action_record))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_action_cancel))
            }
        },
    )
}

@Composable
internal fun MarkdownPreviewDialog(
    artifact: TrackerArtifact,
    onDismiss: () -> Unit,
    fetchContent: suspend (String) -> String? = { null },
): Unit {
    var content by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artifact) {
        val storage = artifact.storage
        val externalUrl = artifact.externalUrl
        val localPath = artifact.localPath
        val inlineContent = artifact.content
        var fetched = false

        // Try 1: Use inline content stored in Firestore (best for mobile)
        if (!fetched && inlineContent != null) {
            content = inlineContent
            fetched = true
        }

        // Try 2: Fetch from external URL (signed URL or public link)
        if (!fetched && externalUrl != null) {
            val urlContent = fetchContent(externalUrl)
            if (urlContent != null) {
                content = urlContent
                fetched = true
            }
        }

        // Try 3: Fall back to GCS storage URL
        if (!fetched && storage != null) {
            val gcsUrl = "https://storage.googleapis.com/${storage.bucket}/${storage.path}"
            val gcsContent = fetchContent(gcsUrl)
            if (gcsContent != null) {
                content = gcsContent
                fetched = true
            }
        }

        // Try 4: Read from local filesystem (desktop/JVM only)
        if (!fetched && localPath != null) {
            val localContent = PlatformFileReader.readText(localPath)
            if (localContent != null) {
                content = localContent
                fetched = true
            }
        }

        // All attempts failed
        if (!fetched) {
            if (localPath != null) {
                loadError = "Failed to read this artifact from local storage.\n\nPath: $localPath"
            } else if (storage != null) {
                loadError = "Failed to fetch this artifact from cloud storage.\n\nStorage: ${storage.bucket}/${storage.path}"
            } else if (externalUrl != null) {
                loadError = "Failed to fetch this artifact.\n\nURL: $externalUrl"
            } else {
                loadError = "No storage location available for this artifact."
            }
        }
    }

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
                            text = stringResource(Res.string.dialog_md_kind, artifact.kind.displayName()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        artifact.localPath?.let {
                            Text(
                                text = stringResource(Res.string.dialog_md_path, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(Res.string.dialog_md_content_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = MaterialTheme.componentSizes.hairline,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when {
                        content != null -> {
                            Markdown(
                                content = content!!,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.subMedium),
                            )
                        }
                        loadError != null -> {
                            Text(
                                text = loadError!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(MaterialTheme.spacing.subMedium),
                            )
                        }
                        else -> {
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(MaterialTheme.spacing.subMedium),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_action_close))
            }
        },
    )
}

@Composable
internal fun EventDetailDialog(
    event: TrackerEvent,
    onDismiss: () -> Unit,
): Unit {
    val naText: String = stringResource(Res.string.dialog_event_na)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.dialog_event_detail_title, event.type.displayName()),
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
                            text = stringResource(Res.string.dialog_event_timestamp, event.createdAt.takeUnless { it == Instant.DISTANT_PAST }?.toString() ?: naText),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(Res.string.dialog_event_actor, event.actor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(Res.string.dialog_event_source, event.derivedFrom),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(Res.string.dialog_event_confidence, event.confidence.toString()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Status transition if applicable
                if (event.statusFrom != null || event.statusTo != null) {
                    val initialText: String = stringResource(Res.string.dialog_event_transition_initial)
                    val unchangedText: String = stringResource(Res.string.dialog_event_transition_unchanged)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(MaterialTheme.spacing.subMedium)) {
                            Text(
                                text = stringResource(Res.string.dialog_event_transition_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                            Text(
                                text = stringResource(
                                    Res.string.dialog_event_transition_value,
                                    event.statusFrom?.displayName() ?: initialText,
                                    event.statusTo?.displayName() ?: unchangedText,
                                ),
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
                                text = stringResource(Res.string.dialog_event_email_title),
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
                                text = stringResource(Res.string.dialog_event_payload_title),
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
                Text(stringResource(Res.string.dialog_action_close))
            }
        },
    )
}
