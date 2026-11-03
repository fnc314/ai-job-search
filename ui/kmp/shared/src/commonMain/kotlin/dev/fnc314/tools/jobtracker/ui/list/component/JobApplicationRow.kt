package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.ui.component.StatusBadge
import dev.fnc314.tools.jobtracker.util.displayName

@Composable
fun JobApplicationRow(
    application: JobApplication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusName = application.status.displayName()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(
                onClick = onClick,
                onClickLabel = "View $${application.company} details",
            )
            .semantics { contentDescription = "${application.company}, ${application.role ?: "No role"}, $statusName" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = application.company,
                style = MaterialTheme.typography.titleMedium,
            )
            application.role?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StatusBadge(status = application.status)
    }
}
