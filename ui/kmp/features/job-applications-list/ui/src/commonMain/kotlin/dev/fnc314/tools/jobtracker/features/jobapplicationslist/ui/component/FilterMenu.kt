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
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_active_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_all_statuses
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_all_time
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_clear_all
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_label_age
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_label_all
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_label_status
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_label_status_age
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_section_age
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_section_status
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_filter_tooltip
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterMenu(
    selectedStatus: TrackerStatus,
    selectedAge: ApplicationAge,
    onFilterChange: (TrackerStatus, ApplicationAge) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var expanded by remember { mutableStateOf(false) }

    val filterLabel: String = when {
        selectedStatus != TrackerStatus.NONE && selectedAge != ApplicationAge.NONE ->
            stringResource(Res.string.list_filter_label_status_age, selectedStatus.displayName(), selectedAge.displayName)
        selectedStatus != TrackerStatus.NONE ->
            stringResource(Res.string.list_filter_label_status, selectedStatus.displayName())
        selectedAge != ApplicationAge.NONE ->
            stringResource(Res.string.list_filter_label_age, selectedAge.displayName)
        else ->
            stringResource(Res.string.list_filter_label_all)
    }
    val activeCd: String = stringResource(Res.string.list_filter_active_cd)
    val filterCd: String = stringResource(Res.string.list_filter_cd, filterLabel)

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(Res.string.list_filter_tooltip))
                }
            },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    contentDescription = filterCd
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
                    text = filterLabel,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
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
            // Reset option
            if (selectedStatus != TrackerStatus.NONE || selectedAge != ApplicationAge.NONE) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.list_filter_clear_all),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = {
                        onFilterChange(TrackerStatus.NONE, ApplicationAge.NONE)
                        expanded = false
                    },
                )
                HorizontalDivider()
            }

            // Status Section
            Text(
                text = stringResource(Res.string.list_filter_section_status),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.list_filter_all_statuses)) },
                trailingIcon = {
                    if (selectedStatus == TrackerStatus.NONE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = activeCd,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                        )
                    }
                },
                onClick = {
                    onFilterChange(TrackerStatus.NONE, selectedAge)
                },
            )

            val statuses = listOf(
                TrackerStatus.DRAFTING,
                TrackerStatus.APPLIED,
                TrackerStatus.INTERVIEW,
                TrackerStatus.OFFER_EXPECTED,
                TrackerStatus.HIRED,
                TrackerStatus.REJECTED,
                TrackerStatus.NO_RESPONSE,
                TrackerStatus.OFFER_DECLINED,
                TrackerStatus.APPLICATION_WITHDRAWN,
            )

            statuses.forEach { status ->
                val isSelected = selectedStatus == status
                DropdownMenuItem(
                    text = { Text(status.displayName()) },
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
                        onFilterChange(status, selectedAge)
                    },
                )
            }

            HorizontalDivider()

            // Age Section
            Text(
                text = stringResource(Res.string.list_filter_section_age),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall,
                ),
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.list_filter_all_time)) },
                trailingIcon = {
                    if (selectedAge == ApplicationAge.NONE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = activeCd,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.medium),
                        )
                    }
                },
                onClick = {
                    onFilterChange(selectedStatus, ApplicationAge.NONE)
                },
            )

            ApplicationAge.entries.filter { it != ApplicationAge.NONE }.forEach { age ->
                val isSelected = selectedAge == age
                DropdownMenuItem(
                    text = { Text(age.displayName) },
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
                        onFilterChange(selectedStatus, age)
                    },
                )
            }
        }
    }
}
