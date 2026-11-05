package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.core.data.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterMenu(
    selectedStatus: TrackerStatus,
    selectedAge: ApplicationAge,
    onFilterChange: (TrackerStatus, ApplicationAge) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val filterLabel = when {
        selectedStatus != TrackerStatus.NONE && selectedAge != ApplicationAge.NONE ->
            "${selectedStatus.displayName()} · ${selectedAge.displayName}"
        selectedStatus != TrackerStatus.NONE -> "Status: ${selectedStatus.displayName()}"
        selectedAge != ApplicationAge.NONE -> "Age: ${selectedAge.displayName}"
        else -> "Filter: All"
    }

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
            tooltip = {
                PlainTooltip {
                    Text("Filter applications by status and age")
                }
            },
            state = rememberTooltipState(),
        ) {
            FilledTonalButton(
                onClick = { expanded = true },
                modifier = Modifier.semantics {
                    contentDescription = "Active filter: $filterLabel"
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
                            text = "Clear All Filters",
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
                text = "STATUS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall
                ),
            )

            DropdownMenuItem(
                text = { Text("All Statuses") },
                trailingIcon = {
                    if (selectedStatus == TrackerStatus.NONE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
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
                                contentDescription = "Active",
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
                text = "AGE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.extraSmall
                ),
            )

            DropdownMenuItem(
                text = { Text("All Time") },
                trailingIcon = {
                    if (selectedAge == ApplicationAge.NONE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
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
                                contentDescription = "Active",
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
