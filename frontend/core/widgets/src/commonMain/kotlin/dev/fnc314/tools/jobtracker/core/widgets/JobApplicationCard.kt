@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Fr
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.card_action_view_details
import fnc314jobsearchtracker.core_widgets.generated.resources.card_applied
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
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 3
private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val CHEVRON_ROTATION_EXPANDED = 90f
private const val JOB_TITLE_NUMBER_OF_LINES: Int = 3
private const val JOB_UPDATED_AT_NUMBER_OF_LINES: Int = 2
private val COMPACT_LOGO_SIZE = 90.dp
private val WIDE_LOGO_SIZE = 70.dp
private val CHEVRON_TOUCH_WIDTH = 40.dp
private val CHEVRON_ICON_SIZE = 32.dp
private val TEXT_STACK_SPACING = 4.dp

/**
 * Reusable job application card widget.
 *
 * Renders an adaptive card presenting application details. Displays an expandable compact view
 * on narrow viewports and an expanded responsive multi-column layout on wider screens.
 *
 * @param model UI model representing the application.
 * @param onApplicationClick Callback invoked when an application card or "View Details" button is tapped.
 * @param modifier Layout modifier.
 * @param onToggleExpand Optional callback for managing expansion externally. When null, managed internally.
 */
@Composable
public fun JobApplicationCard(
    model: JobApplicationCardUiModel,
    onApplicationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleExpand: (() -> Unit)? = null,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

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

    if (isCompact) {
        CompactJobApplicationCard(
            model = model,
            displayCompany = displayCompany,
            roleDesc = roleDesc,
            clickLabel = clickLabel,
            rowContentDescription = rowContentDescription,
            onApplicationClick = onApplicationClick,
            onToggleExpand = onToggleExpand,
            modifier = modifier,
        )
    } else {
        WideJobApplicationCard(
            model = model,
            displayCompany = displayCompany,
            roleDesc = roleDesc,
            clickLabel = clickLabel,
            rowContentDescription = rowContentDescription,
            onApplicationClick = onApplicationClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactJobApplicationCard(
    model: JobApplicationCardUiModel,
    displayCompany: String,
    roleDesc: String,
    clickLabel: String,
    rowContentDescription: String,
    onApplicationClick: (String) -> Unit,
    onToggleExpand: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var internalExpanded by remember { mutableStateOf(model.isExpanded) }
    val isExpanded = onToggleExpand?.let { model.isExpanded } ?: internalExpanded
    val toggleAction = onToggleExpand ?: { internalExpanded = !internalExpanded }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) CHEVRON_ROTATION_EXPANDED else CHEVRON_ROTATION_COLLAPSED,
    )
    val createdDate = model.createdDateOrNull()
    val notAvailableText = stringResource(Res.string.card_not_available)
    val createdLineText = stringResource(
        Res.string.card_created,
        createdDate ?: notAvailableText,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = toggleAction,
                onClickLabel = clickLabel,
            )
            .semantics {
                contentDescription = rowContentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompanyLogoImage(
                    companyName = displayCompany,
                    logoUrl = model.logoUrl,
                    companyInitial = model.companyInitial,
                    size = COMPACT_LOGO_SIZE,
                    textStyle = MaterialTheme.typography.titleLarge,
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TEXT_STACK_SPACING),
                ) {
                    Text(
                        text = roleDesc,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displayCompany,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(TEXT_STACK_SPACING))
                    Text(
                        text = createdLineText,
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
                                text = "[AUTO-SYNCED]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))

                Box(
                    modifier = Modifier
                        .width(CHEVRON_TOUCH_WIDTH)
                        .height(COMPACT_LOGO_SIZE)
                        .clickable(onClick = toggleAction),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(CHEVRON_ICON_SIZE)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                ExpandedApplicationMetadataGrid(
                    model = model,
                    onApplicationClick = onApplicationClick,
                )
            }
        }
    }
}

@Composable
private fun ExpandedApplicationMetadataGrid(
    model: JobApplicationCardUiModel,
    onApplicationClick: (String) -> Unit,
) {
    val gridGap = MaterialTheme.spacing.small
    val fitScoreValue = model.fitScoreOrNull()
    val appliedDateValue = model.appliedDateOrNull()
    val updatedDateValue = model.updatedDateOrNull()

    val notEvaluatedText = stringResource(Res.string.card_not_evaluated)
    val notAppliedText = stringResource(Res.string.card_not_applied)
    val notAvailableText = stringResource(Res.string.card_not_available)

    Grid(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.medium),
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

        Button(
            onClick = { onApplicationClick(model.id) },
            modifier = Modifier.gridItem(columnSpan = GRID_COLUMNS),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(Res.string.card_action_view_details))
        }
    }
}

@Composable
private fun WideJobApplicationCard(
    model: JobApplicationCardUiModel,
    displayCompany: String,
    roleDesc: String,
    clickLabel: String,
    rowContentDescription: String,
    onApplicationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notAvailableText = stringResource(Res.string.card_not_available)
    val notAppliedText = stringResource(Res.string.card_not_applied)
    val createdDate = model.createdDateDisplay(backup = notAvailableText)
    val appliedDate = model.appliedDateDisplay(backup = notAppliedText)

    val gridSizes = object {
        val columns = object {
            val avatar = GridTrackSize.Fixed(size = WIDE_LOGO_SIZE)
            val roleCompany = GridTrackSize.Flex(weight = Fr(value = 2.0F))
            val dates = GridTrackSize.Flex(weight = Fr(value = 1.5F))
            val tags = GridTrackSize.Flex(weight = Fr(value = 1.0F))
        }
        val rows = object {
            val size = GridTrackSize.Fixed(size = WIDE_LOGO_SIZE.div(2))
        }
        val gaps = object {
            val column = MaterialTheme.spacing.small
            val row = MaterialTheme.spacing.none
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onApplicationClick(model.id) },
                onClickLabel = clickLabel,
            )
            .semantics {
                contentDescription = rowContentDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Grid(
            modifier = Modifier.padding(all = MaterialTheme.spacing.small),
            config = {
                column(size = gridSizes.columns.avatar)
                column(size = gridSizes.columns.roleCompany)
                column(size = gridSizes.columns.dates)
                column(size = gridSizes.columns.tags)
                gap(column = gridSizes.gaps.column, row = gridSizes.gaps.row)
                row(size = gridSizes.rows.size)
                row(size = gridSizes.rows.size)

                area(areaId = "thumbnail", row = 1, rowSpan = 2, column = 1, columnSpan = 1)
                area(areaId = "statusTag", row = 2, rowSpan = 1, column = 1, columnSpan = 1)
                area(areaId = "role", row = 1, rowSpan = 1, column = 2, columnSpan = 1)
                area(areaId = "company", row = 2, rowSpan = 1, column = 2, columnSpan = 1)
                area(areaId = "fitTag", row = 1, rowSpan = 1, column = 3, columnSpan = 1)
                area(areaId = "updated", row = 2, rowSpan = 1, column = 3, columnSpan = 1)
                area(areaId = "created", row = 1, rowSpan = 1, column = 4, columnSpan = 1)
                area(areaId = "applied", row = 2, rowSpan = 1, column = 4, columnSpan = 1)
            },
        ) {
            Box(
                modifier = Modifier
                    .gridItem(areaId = "thumbnail")
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CompanyLogoImage(
                    companyName = displayCompany,
                    logoUrl = model.logoUrl,
                    companyInitial = model.companyInitial,
                    size = WIDE_LOGO_SIZE,
                    textStyle = MaterialTheme.typography.titleMedium,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "role")
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = roleDesc,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = JOB_TITLE_NUMBER_OF_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "company")
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = displayCompany,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "created")
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(Res.string.card_created, createdDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "applied")
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(Res.string.card_applied, appliedDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = JOB_UPDATED_AT_NUMBER_OF_LINES,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "updated")
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(
                        Res.string.card_updated,
                        model.updatedDateDisplay(backup = notAvailableText),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = JOB_UPDATED_AT_NUMBER_OF_LINES,
                )
            }
            Box(
                modifier = Modifier
                    .gridItem(areaId = "statusTag")
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomStart,
            ) {
                StatusBadge(
                    status = model.status,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
            if (model.fitScore != null) {
                Box(
                    modifier = Modifier
                        .gridItem(areaId = "fitTag")
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    FitBadge(
                        score = model.fitScore,
                    )
                }
            }
        }
    }
}
