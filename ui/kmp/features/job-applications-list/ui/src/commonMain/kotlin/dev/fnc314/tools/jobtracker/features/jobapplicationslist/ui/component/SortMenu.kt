package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase

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
        SortJobApplicationsUseCase.SortColumn.DATE_CREATED -> "Date Created"
        SortJobApplicationsUseCase.SortColumn.DATE_UPDATED -> "Date Updated"
        SortJobApplicationsUseCase.SortColumn.COMPANY -> "Company"
        SortJobApplicationsUseCase.SortColumn.STATUS -> "Status"
        SortJobApplicationsUseCase.SortColumn.FIT -> "Fit Score"
        SortJobApplicationsUseCase.SortColumn.ROLE -> "Role"
    }

    val directionLabel = if (sortAscending) "Ascending" else "Descending"

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
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
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.subMedium,
                    vertical = MaterialTheme.spacing.subSmall
                ),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "$columnLabel ($directionLabel)",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Icon(
                    imageVector = if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.iconSizes.small),
                )
            }
        }

        // Layered Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Layer 1: Date options (Created At vs Updated At)
            Text(
                text = "DATE SORT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall
                ),
            )

            DropdownMenuItem(
                text = { Text("Created At") },
                trailingIcon = {
                    if (sortColumn == SortJobApplicationsUseCase.SortColumn.DATE_CREATED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                        )
                    }
                },
                onClick = {
                    onSortChange(SortJobApplicationsUseCase.SortColumn.DATE_CREATED, sortAscending)
                },
            )

            DropdownMenuItem(
                text = { Text("Updated At") },
                trailingIcon = {
                    if (sortColumn == SortJobApplicationsUseCase.SortColumn.DATE_UPDATED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                        )
                    }
                },
                onClick = {
                    onSortChange(SortJobApplicationsUseCase.SortColumn.DATE_UPDATED, sortAscending)
                },
            )

            HorizontalDivider()

            // Layer 2: Field options
            Text(
                text = "OTHER FIELDS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall
                ),
            )

            val otherColumns = listOf(
                SortJobApplicationsUseCase.SortColumn.COMPANY to "Company",
                SortJobApplicationsUseCase.SortColumn.ROLE to "Role",
                SortJobApplicationsUseCase.SortColumn.STATUS to "Status",
                SortJobApplicationsUseCase.SortColumn.FIT to "Fit Score",
            )

            otherColumns.forEach { (col, label) ->
                val isSelected = sortColumn == col
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                            )
                        }
                    },
                    onClick = {
                        onSortChange(col, sortAscending)
                    },
                )
            }

            HorizontalDivider()

            // Layer 3: Select Direction
            Text(
                text = "DIRECTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall
                ),
            )

            DropdownMenuItem(
                text = { Text("Ascending (Oldest first / A → Z)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                    )
                },
                trailingIcon = {
                    if (sortAscending) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                        )
                    }
                },
                onClick = {
                    onSortChange(sortColumn, true)
                    expanded = false
                },
            )

            DropdownMenuItem(
                text = { Text("Descending (Newest first / Z → A)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                    )
                },
                trailingIcon = {
                    if (!sortAscending) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
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
