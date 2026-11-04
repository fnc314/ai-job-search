package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.ui.component.StatusBadge
import dev.fnc314.tools.jobtracker.ui.detail.component.formatTimestamp
import dev.fnc314.tools.jobtracker.util.displayName
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalGridApi::class)
@Composable
fun JobApplicationRow(
    application: JobApplication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusName = application.status.displayName()
    val extraSmallGap = MaterialTheme.spacing.extraSmall
    Grid(
        config = {
            gap(row = extraSmallGap, column = extraSmallGap)
            area(
                areaId = "company",
                row = 1,
                column = 1
            )
            area(
                areaId = "role",
                row = 2,
                column = 1,
            )
            area(
                areaId = "status",
                row = 1,
                column = 2
            )
            area(
                areaId = "timestamp",
                row = 2,
                column = 2,
            )
        },
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.small)
            .clickable(
                onClick = onClick,
                onClickLabel = "View $${application.company} details",
            )
            .semantics {
                contentDescription =
                    "${application.company}, ${application.role ?: "No role"}, $statusName"
            },
    ) {
        Text(
            text = application.company,
            modifier = Modifier.fillMaxWidth().gridItem(areaId = "company", alignment = Alignment.CenterStart).border(width = 1.dp, color = Color.Red),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
        application.role?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth().gridItem(areaId = "role", alignment = Alignment.CenterStart).border(width = 1.dp, color = Color.Red),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 2,
            )
        }
        StatusBadge(
            status = application.status,
            modifier = Modifier.fillMaxWidth().gridItem(areaId = "status", alignment = Alignment.CenterEnd).border(width = 1.dp, color = Color.Red),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = when {
                application.createdAt != null -> "Created ${formatTimestamp(instant = application.createdAt)}"
                application.updatedAt != null -> "Updated ${formatTimestamp(instant = application.updatedAt)}"
                else -> "No Time Information"
            },
            modifier = Modifier.fillMaxWidth().gridItem(areaId = "timestamp", alignment = Alignment.CenterEnd).border(width = 1.dp, color = Color.Red),
            color = MaterialTheme.colorScheme.secondaryFixed,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            maxLines = 2
        )
    }

}
