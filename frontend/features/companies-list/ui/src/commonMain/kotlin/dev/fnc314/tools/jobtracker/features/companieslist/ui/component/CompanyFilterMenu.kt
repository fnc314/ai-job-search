package dev.fnc314.tools.jobtracker.features.companieslist.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.TooltipAnchorPosition
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
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanyStatusFilter
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_all
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_cd
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_menu_tooltip
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_ongoing_interview
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_open_apps
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_filter_open_offer
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompanyFilterMenu(
    selectedFilter: CompanyStatusFilter,
    onFilterChange: (CompanyStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val filterLabel: String = when (selectedFilter) {
        CompanyStatusFilter.ALL -> stringResource(Res.string.companies_filter_all)
        CompanyStatusFilter.OPEN_APPLICATIONS -> stringResource(Res.string.companies_filter_open_apps)
        CompanyStatusFilter.ONGOING_INTERVIEW -> stringResource(Res.string.companies_filter_ongoing_interview)
        CompanyStatusFilter.OPEN_OFFER -> stringResource(Res.string.companies_filter_open_offer)
    }

    val filterCd: String = stringResource(Res.string.companies_filter_cd, filterLabel)

    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
            ),
            tooltip = {
                PlainTooltip {
                    Text(stringResource(Res.string.companies_filter_menu_tooltip))
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
                    modifier = Modifier.size(MaterialTheme.iconSizes.small),
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.companies_filter_all)) },
                onClick = {
                    onFilterChange(CompanyStatusFilter.ALL)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedFilter == CompanyStatusFilter.ALL) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        )
                    }
                },
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.companies_filter_open_apps)) },
                onClick = {
                    onFilterChange(CompanyStatusFilter.OPEN_APPLICATIONS)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedFilter == CompanyStatusFilter.OPEN_APPLICATIONS) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        )
                    }
                },
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.companies_filter_ongoing_interview)) },
                onClick = {
                    onFilterChange(CompanyStatusFilter.ONGOING_INTERVIEW)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedFilter == CompanyStatusFilter.ONGOING_INTERVIEW) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        )
                    }
                },
            )

            DropdownMenuItem(
                text = { Text(stringResource(Res.string.companies_filter_open_offer)) },
                onClick = {
                    onFilterChange(CompanyStatusFilter.OPEN_OFFER)
                    expanded = false
                },
                leadingIcon = {
                    if (selectedFilter == CompanyStatusFilter.OPEN_OFFER) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        )
                    }
                },
            )
        }
    }
}
