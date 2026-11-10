@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.features.companieslist.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.CompanyLogoImage
import dev.fnc314.tools.jobtracker.core.widgets.CompanyMetricsTextRow
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
import org.jetbrains.compose.resources.stringResource

private val COMPACT_LOGO_SIZE = 64.dp
private val WIDE_LOGO_SIZE = 72.dp
private val CHEVRON_ICON_SIZE = 28.dp

/**
 * Adaptive employer card tile rendering company summary and application metrics.
 *
 * Expands on narrow viewports when clicked and displays full metric distribution on wide screens.
 *
 * @param model UI model representing the company.
 * @param onCompanyClick Callback invoked to navigate to company details.
 * @param modifier Layout modifier.
 */
@Composable
public fun CompanyCard(
    model: CompanyCardUiModel,
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    if (isCompact) {
        CompactCompanyCard(
            model = model,
            onCompanyClick = onCompanyClick,
            modifier = modifier,
        )
    } else {
        WideCompanyCard(
            model = model,
            onCompanyClick = onCompanyClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactCompanyCard(
    model: CompanyCardUiModel,
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(model.isExpanded) }
    val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

    val contentDesc = stringResource(
        Res.string.company_card_cd,
        model.name,
        model.totalApplications,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .semantics { contentDescription = contentDesc },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            // Header: Logo | Info | Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CompanyLogoImage(
                    companyName = model.name,
                    logoUrl = model.logoUrl,
                    companyInitial = model.companyInitial,
                    size = COMPACT_LOGO_SIZE,
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = model.industry ?: stringResource(Res.string.company_card_industry_na),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${stringResource(Res.string.company_card_submitted_label)}: ${model.submittedApplications} · ${stringResource(Res.string.company_card_total_label)}: ${model.totalApplications}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Suppressed when 0 applications submitted; vertically stacked when present
                    if (!model.lastApplicationSubmittedDate.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(CHEVRON_ICON_SIZE)
                        .rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.small),
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

                    // Complete metrics row with % breakdown
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

                    Button(
                        onClick = { onCompanyClick(model.id, model.name, model.website) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(Res.string.company_card_action_view_details))
                    }
                }
            }
        }
    }
}

@Composable
private fun WideCompanyCard(
    model: CompanyCardUiModel,
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDesc = stringResource(
        Res.string.company_card_cd,
        model.name,
        model.totalApplications,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCompanyClick(model.id, model.name, model.website) }
            .semantics { contentDescription = contentDesc },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            // Top Section: Logo, Title, Website, Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    modifier = Modifier.weight(1f),
                ) {
                    CompanyLogoImage(
                        companyName = model.name,
                        logoUrl = model.logoUrl,
                        companyInitial = model.companyInitial,
                        size = WIDE_LOGO_SIZE,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = model.industry ?: stringResource(Res.string.company_card_industry_na),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!model.website.isNullOrBlank()) {
                            Text(
                                text = model.website,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

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
                    // Suppressed when 0 applications submitted; vertically stacked date
                    if (!model.lastApplicationSubmittedDate.isNullOrBlank()) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(1.dp),
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
            }

            // Middle: Complete semantic metrics row
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

            // Bottom metadata grid
            val gridGap = MaterialTheme.spacing.extraSmall
            Grid(
                modifier = Modifier.fillMaxWidth(),
                config = {
                    repeat(2) { column(1.fr) }
                    repeat(2) { row(GridTrackSize.Auto) }
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
                        modifier = Modifier.gridItem(columnSpan = 2),
                    )
                }
            }
        }
    }
}
