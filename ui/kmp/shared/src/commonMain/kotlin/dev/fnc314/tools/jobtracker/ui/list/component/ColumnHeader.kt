package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase

@Composable
fun ColumnHeader(
    sortColumn: SortJobApplicationsUseCase.SortColumn,
    sortAscending: Boolean,
    onSortClick: (SortJobApplicationsUseCase.SortColumn) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val columns = listOf(
                SortJobApplicationsUseCase.SortColumn.DATE to "Date",
                SortJobApplicationsUseCase.SortColumn.COMPANY to "Company",
                SortJobApplicationsUseCase.SortColumn.STATUS to "Status",
                SortJobApplicationsUseCase.SortColumn.FIT to "Fit",
                SortJobApplicationsUseCase.SortColumn.ROLE to "Role",
            )

            columns.forEach { (column, label) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onSortClick(column) },
                            onClickLabel = "Sort by $label",
                        )
                        .semantics { contentDescription = "Sort by $label" }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (sortColumn == column) {
                        Text(
                            text = if (sortAscending) "▲" else "▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { contentDescription = if (sortAscending) "Ascending" else "Descending" },
                        )
                    }
                }
            }
        }
    }
}
