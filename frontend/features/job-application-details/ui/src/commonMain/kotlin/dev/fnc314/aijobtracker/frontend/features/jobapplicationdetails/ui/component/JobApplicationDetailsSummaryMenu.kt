package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.data.model.EventActor
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.widgets.formatRelativeInstant
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.JobApplicationDetailUiModel

/**
 * Summary dropdown popup content for the Job Application Details screen.
 *
 * Displays the last event which changed the application status, including
 * status transition, timestamp, actor, and event note.
 */
@Composable
public fun JobApplicationDetailsSummaryMenu(
    model: JobApplicationDetailUiModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Find the latest status-changing event
    val lastStatusEvent: TrackerEvent? = model.timelineEvents.lastOrNull { event ->
        event.statusTo != null && event.statusTo != TrackerStatus.Known.UNSPECIFIED
    }

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = "Status Transition History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        HorizontalDivider(color = DividerDefaults.color)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        if (lastStatusEvent != null) {
            val fromStatus = lastStatusEvent.statusFrom?.displayName
                ?.takeUnless { it.isBlank() || lastStatusEvent.statusFrom == TrackerStatus.Known.UNSPECIFIED }
                ?: "Initial"
            val toStatus = lastStatusEvent.statusTo?.displayName ?: "Unknown"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(12.dp),
                ) {}
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = "$fromStatus → $toStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            val relativeTime = formatRelativeInstant(lastStatusEvent.createdAt) ?: "Recent"
            Text(
                text = "Occurred $relativeTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val actorDisplay = when (lastStatusEvent.actor) {
                EventActor.Known.USER -> "Candidate"
                EventActor.Known.AGENT -> "Autonomous Agent"
                EventActor.Known.EMAIL_SYNC -> "Email Sync"
                EventActor.Known.SCHEDULER -> "Scheduler"
                EventActor.Known.IMPORT -> "Data Import"
                else -> null
            }
            if (actorDisplay != null) {
                Text(
                    text = "Initiated by $actorDisplay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val note = lastStatusEvent.note
            if (!note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "“$note”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                text = "Current Status: ${model.statusDisplayText}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = "No recorded status transition events yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
