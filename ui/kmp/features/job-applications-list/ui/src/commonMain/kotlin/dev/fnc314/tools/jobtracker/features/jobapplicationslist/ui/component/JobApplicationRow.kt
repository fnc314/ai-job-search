package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationItemUiModel
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListUiIntent
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_missing
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_action_view_details
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_click_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created_applied
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_applied_on
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_fit_score
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_updated
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_no_role
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 3

@Composable
private fun GridHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GridValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@OptIn(ExperimentalGridApi::class)
@Composable
internal fun JobApplicationRow(
    model: JobApplicationItemUiModel,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val clickLabel: String = stringResource(Res.string.list_row_click_cd, model.companyName)
    val noRoleText: String = stringResource(Res.string.list_row_no_role)
    val roleDesc: String = if (model.roleTitle.isNotEmpty()) model.roleTitle else noRoleText
    val fitDesc: String = model.fitScoreText?.let { ", $it" } ?: ""
    val rowContentDescription: String = stringResource(
        Res.string.list_row_cd,
        model.companyName,
        roleDesc,
        model.statusDisplayName,
        fitDesc,
    )
    val dataMissing: String = stringResource(Res.string.data_missing)

    if (isCompact) {
        var isExpanded by remember { mutableStateOf(false) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { isExpanded = !isExpanded },
                    onClickLabel = clickLabel,
                )
                .padding(MaterialTheme.spacing.medium)
                .semantics {
                    contentDescription = rowContentDescription
                },
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Logo at START (90x90 square)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(MaterialTheme.shapes.small.topStart),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = model.companyName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

                // 2 & 3. Title (top-end of logo) and Company (bottom-end of logo) + Created on / StatusTag
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = roleDesc,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = model.companyName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.list_row_created,
                            model.createdDateDisplay(backup = dataMissing),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    StatusBadge(
                        status = model.status,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

                // 4. Chevron (90x90 space at END)
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clickable { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(if (isExpanded) 0f else 0f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // EXPANDED STATE: flat 3x3 grid (headers / values / action)
            AnimatedVisibility(visible = isExpanded) {
                val gridGap = MaterialTheme.spacing.small
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
                    GridHeader(stringResource(Res.string.list_row_header_fit_score))
                    GridHeader(stringResource(Res.string.list_row_header_applied_on))
                    GridHeader(stringResource(Res.string.list_row_header_updated))

                    GridValue(model.fitScoreDisplay(backup = dataMissing))
                    GridValue(model.appliedDateDisplay(backup = dataMissing))
                    GridValue(model.updatedDateDisplay(backup = dataMissing))

                    Button(
                        onClick = { onIntent(model.onClickIntent) },
                        modifier = Modifier.gridItem(columnSpan = GRID_COLUMNS),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(Res.string.list_row_action_view_details))
                    }
                }
            }
        }
    } else {
        // WIDER SCREEN WIDTHS: Non-collapsing card, tapping launches details
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { onIntent(model.onClickIntent) },
                    onClickLabel = clickLabel,
                )
                .padding(MaterialTheme.spacing.medium)
                .semantics {
                    contentDescription = rowContentDescription
                },
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(MaterialTheme.shapes.small.topStart),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = model.companyName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = roleDesc,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = model.companyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                // Data alignment order on wider screens:
                // 1. Created on -> Applied on
                // 2. StatusTag -> FitTag
                // 3. Updated ...
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(
                                Res.string.list_row_created_applied,
                                model.createdDateDisplay(backup = dataMissing),
                                model.appliedDateDisplay(backup = dataMissing),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = model.updatedDateDisplay(backup = dataMissing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        StatusBadge(
                            status = model.status,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                        if (model.fitScore != null) {
                            FitBadge(score = model.fitScore)
                        }
                    }
                }
            }
        }
    }
}
