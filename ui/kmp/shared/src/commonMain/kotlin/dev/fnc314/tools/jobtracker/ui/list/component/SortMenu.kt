package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortMenu(
    sortColumn: SortJobApplicationsUseCase.SortColumn,
    sortAscending: Boolean,
    onSortChange: (SortJobApplicationsUseCase.SortColumn, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val columnLabel = when (sortColumn) {
        SortJobApplicationsUseCase.SortColumn.DATE -> "Date"
        SortJobApplicationsUseCase.SortColumn.COMPANY -> "Company"
        SortJobApplicationsUseCase.SortColumn.STATUS -> "Status"
        SortJobApplicationsUseCase.SortColumn.FIT -> "Fit Score"
        SortJobApplicationsUseCase.SortColumn.ROLE -> "Role"
    }

    val directionLabel = if (sortAscending) "Ascending" else "Descending"

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text("Change sorting column and order")
                }
            },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    contentDescription = "Sort by $columnLabel, $directionLabel"
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "$columnLabel ($directionLabel)",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Layered Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Layer 1: Select Column
            Text(
                text = "SORT BY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            val columns = listOf(
                SortJobApplicationsUseCase.SortColumn.COMPANY to "Company",
                SortJobApplicationsUseCase.SortColumn.ROLE to "Role",
                SortJobApplicationsUseCase.SortColumn.STATUS to "Status",
                SortJobApplicationsUseCase.SortColumn.FIT to "Fit Score",
                SortJobApplicationsUseCase.SortColumn.DATE to "Date",
            )

            columns.forEach { (col, label) ->
                val isSelected = sortColumn == col
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        onSortChange(col, sortAscending)
                    },
                )
            }

            HorizontalDivider()

            // Layer 2: Select Direction
            Text(
                text = "DIRECTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            DropdownMenuItem(
                text = { Text("Ascending (A → Z, Oldest first)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (sortAscending) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                onClick = {
                    onSortChange(sortColumn, true)
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = { Text("Descending (Z → A, Newest first)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (!sortAscending) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                },
                onClick = {
                    onSortChange(sortColumn, false)
                    expanded = false
                },
            )
        }
    }
}
