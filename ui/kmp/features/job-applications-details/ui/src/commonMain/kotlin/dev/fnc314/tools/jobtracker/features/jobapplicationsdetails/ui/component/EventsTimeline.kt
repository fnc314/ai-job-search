package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.data.model.*
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.EventDetailDialog
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.data.util.formatSemanticInstant

@Composable
internal fun EventsTimeline(
    events: List<TrackerEvent>,
    application: JobApplication? = null,
    modifier: Modifier = Modifier,
) {
    var selectedEvent by remember { mutableStateOf<TrackerEvent?>(null) }

    if (selectedEvent != null) {
        EventDetailDialog(
            event = selectedEvent!!,
            onDismiss = { selectedEvent = null },
        )
    }

    // Populate events: if empty, derive synthetic lifecycle events from application
    val displayEvents = remember(events, application) {
        if (events.isNotEmpty()) {
            events
        } else if (application != null) {
            buildList {
                application.createdAt?.let { created ->
                    add(
                        TrackerEvent(
                            id = "evt_created",
                            applicationId = application.id,
                            type = TrackerEventType.APPLY,
                            statusFrom = null,
                            statusTo = application.status,
                            note = "Application initiated for ${application.company}",
                            emailMessageId = null,
                            actor = EventActor.USER,
                            derivedFrom = DerivedFrom.MANUAL,
                            confidence = Confidence.HIGH,
                            createdAt = created,
                        )
                    )
                }
                if (application.updatedAt != null && application.updatedAt != application.createdAt) {
                    add(
                        TrackerEvent(
                            id = "evt_updated",
                            applicationId = application.id,
                            type = TrackerEventType.CORRECT_STATUS,
                            statusFrom = TrackerStatus.DRAFTING,
                            statusTo = application.status,
                            note = "Application updated to ${application.status.displayName()}",
                            emailMessageId = null,
                            actor = EventActor.AGENT,
                            derivedFrom = DerivedFrom.MANUAL,
                            confidence = Confidence.HIGH,
                            createdAt = application.updatedAt,
                        )
                    )
                }
            }
        } else {
            emptyList()
        }
    }

    if (displayEvents.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No events recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.none),
    ) {
        displayEvents.forEachIndexed { index, event ->
            val isLast = index == displayEvents.lastIndex

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { selectedEvent = event }
                    .padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    )
                    .semantics { contentDescription = "Event: ${event.type.displayName()}" },
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                // Graphic Timeline Node
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(MaterialTheme.componentSizes.timelineNodeSize),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = event.type.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                            )
                        }
                    }
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(MaterialTheme.componentSizes.timelineLineWidth)
                                .height(MaterialTheme.componentSizes.iconButtonSize)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }

                // Event Content Card
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = event.type.displayName(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            event.createdAt?.let { ts ->
                                Text(
                                    text = formatSemanticInstant(ts),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }

                        event.note?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (event.statusFrom != null || event.statusTo != null) {
                            Text(
                                text = "Transition: ${event.statusFrom?.displayName() ?: "Start"} → ${event.statusTo?.displayName() ?: "Current"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
