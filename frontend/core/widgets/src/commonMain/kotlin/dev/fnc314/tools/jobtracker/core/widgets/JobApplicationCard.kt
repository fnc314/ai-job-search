@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.card_action_view_details
import fnc314jobsearchtracker.core_widgets.generated.resources.card_applied
import fnc314jobsearchtracker.core_widgets.generated.resources.card_auto_synced
import fnc314jobsearchtracker.core_widgets.generated.resources.card_cd
import fnc314jobsearchtracker.core_widgets.generated.resources.card_click_cd
import fnc314jobsearchtracker.core_widgets.generated.resources.card_created
import fnc314jobsearchtracker.core_widgets.generated.resources.card_header_applied_on
import fnc314jobsearchtracker.core_widgets.generated.resources.card_header_fit_score
import fnc314jobsearchtracker.core_widgets.generated.resources.card_header_updated
import fnc314jobsearchtracker.core_widgets.generated.resources.card_no_role
import fnc314jobsearchtracker.core_widgets.generated.resources.card_not_applied
import fnc314jobsearchtracker.core_widgets.generated.resources.card_not_available
import fnc314jobsearchtracker.core_widgets.generated.resources.card_not_evaluated
import fnc314jobsearchtracker.core_widgets.generated.resources.card_unknown_company
import fnc314jobsearchtracker.core_widgets.generated.resources.card_updated
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 2
private const val JOB_TITLE_NUMBER_OF_LINES: Int = 2
private const val JOB_UPDATED_AT_NUMBER_OF_LINES: Int = 2

/**
 * Reusable job application card widget built on [TopLevelCard].
 *
 * Renders an adaptive [Grid]-based card presenting application details. Displays an expandable
 * compact view on narrow viewports (revealing fit score, applied/updated dates, and a "View Details"
 * action button) and an expanded direct-navigation layout on wider screens.
 *
 * @param model UI model representing the application.
 * @param onApplicationClick Callback invoked when an application card or "View Details" button is tapped.
 * @param modifier Layout modifier.
 * @param onToggleExpand Optional callback for managing expansion externally. When `null`, managed internally.
 * @param variant Surface color and elevation treatment ([TopLevelCardSurfaceVariant]).
 */
@Composable
public fun JobApplicationCard(
    model: JobApplicationCardUiModel,
    onApplicationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleExpand: (() -> Unit)? = null,
    variant: TopLevelCardSurfaceVariant = TopLevelCardSurfaceVariant.Surface,
): Unit {
    val isCompact = LocalWindowSizeClass.current.isCompact

    val unknownCompanyText = stringResource(Res.string.card_unknown_company)
    val displayCompany = model.companyName.trim().ifEmpty { unknownCompanyText }
    val noRoleText = stringResource(Res.string.card_no_role)
    val roleDesc = model.roleTitle.trim().ifEmpty { noRoleText }
    val clickLabel = stringResource(Res.string.card_click_cd, roleDesc, displayCompany)
    val rowContentDescription = stringResource(
        Res.string.card_cd,
        displayCompany,
        roleDesc,
        model.statusDisplayName,
    )
    val notAvailableText = stringResource(Res.string.card_not_available)
    val notAppliedText = stringResource(Res.string.card_not_applied)
    val createdDate = model.createdDateDisplay(backup = notAvailableText)
    val appliedDate = model.appliedDateDisplay(backup = notAppliedText)
    val updatedDate = model.updatedDateDisplay(backup = notAvailableText)
    val viewDetailsLabel = stringResource(Res.string.card_action_view_details)

    val logoSize = if (isCompact) CompanyLogoSize.Medium else CompanyLogoSize.Large

    TopLevelCard(
        modifier = modifier,
        onClick = { onApplicationClick(model.id) },
        contentDescription = rowContentDescription,
        onClickLabel = clickLabel,
        variant = variant,
        isExpanded = model.isExpanded,
        onToggleExpand = onToggleExpand,
        thumbnail = {
            CompanyLogoImage(
                companyName = displayCompany,
                logoUrl = model.logoUrl,
                companyInitial = model.companyInitial,
                logoSize = logoSize,
            )
        },
        title = {
            Text(
                text = roleDesc,
                style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isCompact) 1 else JOB_TITLE_NUMBER_OF_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitle = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                Text(
                    text = displayCompany,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isCompact) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    ) {
                        StatusBadge(
                            status = model.status,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                        )
                        if (model.fitScore != null) {
                            FitBadge(score = model.fitScore)
                        }
                        if (model.isAutoSynced) {
                            Text(
                                text = stringResource(Res.string.card_auto_synced),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        compactSummary = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
                Text(
                    text = stringResource(Res.string.card_created, createdDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                ) {
                    StatusBadge(
                        status = model.status,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                    )
                    if (model.isAutoSynced) {
                        Text(
                            text = stringResource(Res.string.card_auto_synced),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
        wideTrailingHeader = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
            ) {
                Text(
                    text = stringResource(Res.string.card_created, createdDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = stringResource(Res.string.card_applied, appliedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = JOB_UPDATED_AT_NUMBER_OF_LINES,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = stringResource(Res.string.card_updated, updatedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = JOB_UPDATED_AT_NUMBER_OF_LINES,
                    textAlign = TextAlign.End,
                )
            }
        },
        expandedContent = {
            ExpandedApplicationMetadataGrid(model = model)
        },
        actions = persistentListOf(
            TopLevelCardAction(
                label = viewDetailsLabel,
                onClick = { onApplicationClick(model.id) },
            ),
        ),
    )
}

@Composable
private fun ExpandedApplicationMetadataGrid(
    model: JobApplicationCardUiModel,
) {
    val gridGap = MaterialTheme.spacing.small
    val fitScoreValue = model.fitScoreOrNull()
    val appliedDateValue = model.appliedDateOrNull()
    val updatedDateValue = model.updatedDateOrNull()

    val notEvaluatedText = stringResource(Res.string.card_not_evaluated)
    val notAppliedText = stringResource(Res.string.card_not_applied)
    val notAvailableText = stringResource(Res.string.card_not_available)

    Grid(
        modifier = Modifier.fillMaxWidth(),
        config = {
            repeat(GRID_COLUMNS) { column(1.fr) }
            repeat(GRID_ROWS) { row(GridTrackSize.Auto) }
            gap(gridGap)
        },
    ) {
        Text(
            text = stringResource(Res.string.card_header_fit_score),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.card_header_applied_on),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.card_header_updated),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = fitScoreValue ?: notEvaluatedText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (fitScoreValue == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = appliedDateValue ?: notAppliedText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (appliedDateValue == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = updatedDateValue ?: notAvailableText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (updatedDateValue == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
