@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.features.companieslist.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.CompanyLogoImage
import dev.fnc314.tools.jobtracker.core.widgets.CompanyLogoSize
import dev.fnc314.tools.jobtracker.core.widgets.CompanyMetricsTextRow
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCard
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCardAction
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCardSurfaceVariant
import dev.fnc314.tools.jobtracker.features.companieslist.ui.CompanyCardUiModel
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_action_view_details
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_cd
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_industry_na
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_last_submitted_label
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_locations_label
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_salary_label
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_source_label
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_submitted_label
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.company_card_total_label
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

private const val METADATA_GRID_COLUMNS = 2
private const val METADATA_GRID_ROWS = 2
private val SUBTITLE_LINE_SPACING = 2.dp
private val DATE_STACK_SPACING = 1.dp

/**
 * Adaptive employer card tile built on [TopLevelCard], rendering company summary and application metrics.
 *
 * Expands on narrow viewports when clicked and displays full metric distribution and direct navigation
 * on wide screens.
 *
 * @param model UI model representing the company.
 * @param onCompanyClick Callback invoked to navigate to company details.
 * @param modifier Layout modifier.
 * @param onToggleExpand Optional callback for managing expansion externally.
 * @param variant Surface color and elevation treatment ([TopLevelCardSurfaceVariant]).
 */
@Composable
public fun CompanyCard(
    model: CompanyCardUiModel,
    onCompanyClick: (companyId: String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleExpand: (() -> Unit)? = null,
    variant: TopLevelCardSurfaceVariant = TopLevelCardSurfaceVariant.Surface,
): Unit {
    val isCompact = LocalWindowSizeClass.current.isCompact
    val contentDesc = stringResource(
        Res.string.company_card_cd,
        model.name,
        model.totalApplications,
    )
    val viewDetailsLabel = stringResource(Res.string.company_card_action_view_details)
    val logoSize = if (isCompact) CompanyLogoSize.Medium else CompanyLogoSize.Large

    TopLevelCard(
        modifier = modifier,
        onClick = { onCompanyClick(model.id) },
        contentDescription = contentDesc,
        variant = variant,
        isExpanded = model.isExpanded,
        onToggleExpand = onToggleExpand,
        thumbnail = {
            CompanyLogoImage(
                companyName = model.name,
                logoUrl = model.logoUrl,
                companyInitial = model.companyInitial,
                logoSize = logoSize,
            )
        },
        title = {
            Text(
                text = model.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCompact) FontWeight.Normal else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitle = {
            Column(verticalArrangement = Arrangement.spacedBy(SUBTITLE_LINE_SPACING)) {
                Text(
                    text = model.industry ?: stringResource(Res.string.company_card_industry_na),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isCompact && !model.website.isNullOrBlank()) {
                    Text(
                        text = model.website,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        compactSummary = {
            Column(verticalArrangement = Arrangement.spacedBy(SUBTITLE_LINE_SPACING)) {
                Text(
                    text = "${stringResource(Res.string.company_card_submitted_label)}: ${model.submittedApplications} · ${stringResource(Res.string.company_card_total_label)}: ${model.totalApplications}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!model.lastApplicationSubmittedDate.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(DATE_STACK_SPACING)) {
                        Text(
                            text = stringResource(Res.string.company_card_last_submitted_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = model.lastApplicationSubmittedDate,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        wideTrailingHeader = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                Text(
                    text = "${stringResource(Res.string.company_card_submitted_label)}: ${model.submittedApplications} · ${stringResource(Res.string.company_card_total_label)}: ${model.totalApplications}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!model.lastApplicationSubmittedDate.isNullOrBlank()) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(DATE_STACK_SPACING),
                    ) {
                        Text(
                            text = stringResource(Res.string.company_card_last_submitted_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                        Text(
                            text = model.lastApplicationSubmittedDate,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        },
        wideBody = {
            CompanyWideMetadataSection(model = model)
        },
        expandedContent = {
            CompanyExpandedMetadataSection(model = model)
        },
        actions = persistentListOf(
            TopLevelCardAction(
                label = viewDetailsLabel,
                onClick = { onCompanyClick(model.id) },
            ),
        ),
    )
}

@Composable
private fun CompanyExpandedMetadataSection(
    model: CompanyCardUiModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        if (!model.website.isNullOrBlank()) {
            Text(
                text = model.website,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CompanyMetricsTextRow(
            draftCount = model.draftApplications,
            draftPercentage = model.draftPercentage,
            submittedCount = model.submittedApplications,
            submittedPercentage = model.submittedPercentage,
            interviewingCount = model.interviewApplications,
            interviewingPercentage = model.interviewPercentage,
            offerCount = model.offerApplications,
            offerPercentage = model.offerPercentage,
            rejectedCount = model.rejectedApplications,
            rejectedPercentage = model.rejectedPercentage,
        )

        if (model.salaryRangeDisplay.isNotBlank()) {
            Text(
                text = "${stringResource(Res.string.company_card_salary_label)}: ${model.salaryRangeDisplay}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (model.primaryChannelDisplay.isNotBlank()) {
            Text(
                text = "${stringResource(Res.string.company_card_source_label)}: ${model.primaryChannelDisplay}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (model.locationsText.isNotBlank()) {
            Text(
                text = "${stringResource(Res.string.company_card_locations_label)}: ${model.locationsText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompanyWideMetadataSection(
    model: CompanyCardUiModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        CompanyMetricsTextRow(
            draftCount = model.draftApplications,
            draftPercentage = model.draftPercentage,
            submittedCount = model.submittedApplications,
            submittedPercentage = model.submittedPercentage,
            interviewingCount = model.interviewApplications,
            interviewingPercentage = model.interviewPercentage,
            offerCount = model.offerApplications,
            offerPercentage = model.offerPercentage,
            rejectedCount = model.rejectedApplications,
            rejectedPercentage = model.rejectedPercentage,
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall),
        )

        val gridGap = MaterialTheme.spacing.extraSmall
        Grid(
            modifier = Modifier.fillMaxWidth(),
            config = {
                repeat(METADATA_GRID_COLUMNS) { column(1.fr) }
                repeat(METADATA_GRID_ROWS) { row(GridTrackSize.Auto) }
                gap(gridGap)
            },
        ) {
            if (model.salaryRangeDisplay.isNotBlank()) {
                Text(
                    text = "${stringResource(Res.string.company_card_salary_label)}: ${model.salaryRangeDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (model.primaryChannelDisplay.isNotBlank()) {
                Text(
                    text = "${stringResource(Res.string.company_card_source_label)}: ${model.primaryChannelDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }

            if (model.locationsText.isNotBlank()) {
                Text(
                    text = "${stringResource(Res.string.company_card_locations_label)}: ${model.locationsText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.gridItem(columnSpan = METADATA_GRID_COLUMNS),
                )
            }
        }
    }
}
