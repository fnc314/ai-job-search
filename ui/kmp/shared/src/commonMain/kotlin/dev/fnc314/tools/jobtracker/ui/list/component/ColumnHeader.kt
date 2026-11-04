package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.ui.settings.AppSettings

@Composable
fun ColumnHeader(
    sortColumn: SortJobApplicationsUseCase.SortColumn,
    sortAscending: Boolean,
    onSortClick: (SortJobApplicationsUseCase.SortColumn) -> Unit,
    settings: AppSettings = AppSettings(),
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    Surface(
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            val allColumns = buildList {
                if (settings.showCompany) add(SortJobApplicationsUseCase.SortColumn.COMPANY to "Company")
                if (settings.showRole && !isCompact) add(SortJobApplicationsUseCase.SortColumn.ROLE to "Role")
                if (settings.showStatus) add(SortJobApplicationsUseCase.SortColumn.STATUS to "Status")
                if (settings.showFit && !isCompact) add(SortJobApplicationsUseCase.SortColumn.FIT to "Fit")
                if (settings.showDate && !isCompact) add(SortJobApplicationsUseCase.SortColumn.DATE to "Date")
            }

            allColumns.forEach { (column, label) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { onSortClick(column) },
                            onClickLabel = "Sort by $label",
                        )
                        .semantics { contentDescription = "Sort by $label" }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sortColumn == column) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (sortColumn == column) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                            contentDescription = if (sortAscending) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
