package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.data.util.formatSemanticTimestamp

@Composable
fun JobApplicationRow(
    application: JobApplication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusName = application.status.displayName()
    val fitScore = application.fitScore

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "View ${application.company} details",
            )
            .padding(MaterialTheme.spacing.medium)
            .semantics {
                contentDescription = "${application.company}, ${application.role ?: "No role"}, $statusName${fitScore?.let { ", Fit $it" } ?: ""}"
            },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        // Top line: Company and Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = application.company,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            StatusBadge(
                status = application.status,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }

        // Subtitle line: Role and Timestamp / Fit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            application.role?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                if (fitScore != null) {
                    FitBadge(score = fitScore)
                }

                val timeLabel = formatSemanticTimestamp(
                    updatedAt = application.updatedAt,
                    createdAt = application.createdAt,
                )
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
