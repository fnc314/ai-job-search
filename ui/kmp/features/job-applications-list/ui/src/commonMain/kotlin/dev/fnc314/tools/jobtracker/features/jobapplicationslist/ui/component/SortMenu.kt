package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_active_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_button_label
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_company
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_date_created
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_date_updated
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_fit
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_role
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_col_status
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_created_at
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_dir_ascending
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_dir_descending
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_order_ascending
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_order_descending
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_section_date
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_section_direction
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_section_other
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_tooltip
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_sort_updated_at
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortMenu(
    sortColumn: SortJobApplicationsUseCase.SortColumn,
    sortAscending: Boolean,
    onSortChange: (SortJobApplicationsUseCase.SortColumn, Boolean) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var expanded by remember { mutableStateOf(false) }

    val columnLabel: String = when (sortColumn) {
        SortJobApplicationsUseCase.SortColumn.DATE_CREATED -> stringResource(Res.string.list_sort_col_date_created)
        SortJobApplicationsUseCase.SortColumn.DATE_UPDATED -> stringResource(Res.string.list_sort_col_date_updated)
        SortJobApplicationsUseCase.SortColumn.COMPANY -> stringResource(Res.string.list_sort_col_company)
        SortJobApplicationsUseCase.SortColumn.STATUS -> stringResource(Res.string.list_sort_col_status)
        SortJobApplicationsUseCase.SortColumn.FIT -> stringResource(Res.string.list_sort_col_fit)
        SortJobApplicationsUseCase.SortColumn.ROLE -> stringResource(Res.string.list_sort_col_role)
    }

    val directionLabel: String = if (sortAscending) {
        stringResource(Res.string.list_sort_dir_ascending)
    } else {
        stringResource(Res.string.list_sort_dir_descending)
    }
    val activeCd: String = stringResource(Res.string.list_sort_active_cd)
    val sortCd: String = stringResource(Res.string.list_sort_cd, columnLabel, directionLabel)

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(Res.string.list_sort_tooltip))
                }
            },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    contentDescription = sortCd
                },
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.spacing.subMedium,
                    vertical = MaterialTheme.spacing.subSmall,
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
                    text = stringResource(Res.string.list_sort_button_label, columnLabel, directionLabel),
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
                text = stringResource(Res.string.list_sort_section_date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.list_sort_created_at)) },
                trailingIcon = {
                    if (sortColumn == SortJobApplicationsUseCase.SortColumn.DATE_CREATED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = activeCd,
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
                text = { Text(stringResource(Res.string.list_sort_updated_at)) },
                trailingIcon = {
                    if (sortColumn == SortJobApplicationsUseCase.SortColumn.DATE_UPDATED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = activeCd,
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
                text = stringResource(Res.string.list_sort_section_other),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            )

            val otherColumns = listOf(
                SortJobApplicationsUseCase.SortColumn.COMPANY to stringResource(Res.string.list_sort_col_company),
                SortJobApplicationsUseCase.SortColumn.ROLE to stringResource(Res.string.list_sort_col_role),
                SortJobApplicationsUseCase.SortColumn.STATUS to stringResource(Res.string.list_sort_col_status),
                SortJobApplicationsUseCase.SortColumn.FIT to stringResource(Res.string.list_sort_col_fit),
            )

            otherColumns.forEach { (col, label) ->
                val isSelected = sortColumn == col
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = activeCd,
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
                text = stringResource(Res.string.list_sort_section_direction),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.list_sort_order_ascending)) },
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
                            contentDescription = activeCd,
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
                text = { Text(stringResource(Res.string.list_sort_order_descending)) },
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
                            contentDescription = activeCd,
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
