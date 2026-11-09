package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationItemUiModel
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListUiIntent
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_not_applied
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_not_available
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.data_not_evaluated
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_action_view_details
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_click_cd
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created_applied
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_created_unavailable
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_applied_on
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_fit_score
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_header_updated
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_no_role
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_row_unknown_company
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3
private const val GRID_ROWS = 3
private const val MIN_LOGO_DIMENSION_PX = 64f
private const val MIN_SQUARE_ASPECT_RATIO = 0.8f
private const val MAX_SQUARE_ASPECT_RATIO = 1.25f
private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val CHEVRON_ROTATION_EXPANDED = 90f
private const val VISIBLE_ALPHA = 1f
private const val HIDDEN_ALPHA = 0f

private val COMPACT_LOGO_SIZE = 90.dp
private val WIDE_LOGO_SIZE = 70.dp
private val CHEVRON_TOUCH_WIDTH = 40.dp
private val CHEVRON_ICON_SIZE = 32.dp
private val TEXT_STACK_SPACING = 4.dp
private val WIDE_TEXT_STACK_SPACING = 2.dp

private fun resolveLogoUrl(companyName: String): String? {
    val trimmed = companyName.trim()
    if (trimmed.isEmpty() || trimmed.equals("Unknown Company", ignoreCase = true)) {
        return null
    }
    val clean = trimmed.lowercase()
        .replace(" financial services", "")
        .replace(" technologies", "")
        .replace(" inc", "")
        .replace(" corp", "")
        .replace(" llc", "")
        .replace(" l.p.", "")
        .replace(Regex("[^a-z0-9]"), "")
    return clean.takeIf { it.isNotEmpty() }?.let { "https://logos.hunter.io/$it.com" }
}

private fun isAcceptableSquareLogo(intrinsicSize: Size): Boolean {
    if (!intrinsicSize.isSpecified || intrinsicSize.width < MIN_LOGO_DIMENSION_PX || intrinsicSize.height < MIN_LOGO_DIMENSION_PX) {
        return false
    }
    val aspectRatio = intrinsicSize.width / intrinsicSize.height
    return aspectRatio in MIN_SQUARE_ASPECT_RATIO..MAX_SQUARE_ASPECT_RATIO
}

@Composable
private fun CompanyLogoImage(
    companyName: String,
    companyInitial: String,
    size: Dp,
    textStyle: TextStyle,
) {
    val logoUrl = remember(companyName) { resolveLogoUrl(companyName) }
    var isValidLogoLoaded by remember(logoUrl) { mutableStateOf(false) }
    var isLogoRejected by remember(logoUrl) { mutableStateOf(logoUrl == null) }
    val cornerShape = RoundedCornerShape(MaterialTheme.shapes.small.topStart)
    val containerColor = if (isValidLogoLoaded) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(
                color = containerColor,
                shape = cornerShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isValidLogoLoaded) {
            Text(
                text = companyInitial,
                style = textStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (logoUrl != null && !isLogoRejected) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "$companyName Logo",
                modifier = Modifier
                    .size(size)
                    .clip(cornerShape)
                    .alpha(if (isValidLogoLoaded) VISIBLE_ALPHA else HIDDEN_ALPHA),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            if (isAcceptableSquareLogo(state.painter.intrinsicSize)) {
                                isValidLogoLoaded = true
                            } else {
                                isValidLogoLoaded = false
                                isLogoRejected = true
                            }
                        }
                        is AsyncImagePainter.State.Error -> {
                            isValidLogoLoaded = false
                            isLogoRejected = true
                        }
                        is AsyncImagePainter.State.Loading,
                        is AsyncImagePainter.State.Empty,
                        -> {
                            isValidLogoLoaded = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun GridHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GridValue(
    text: String,
    isPlaceholder: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isPlaceholder) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun JobApplicationRow(
    model: JobApplicationItemUiModel,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val unknownCompanyText = stringResource(Res.string.list_row_unknown_company)
    val displayCompany = model.companyName.trim().ifEmpty { unknownCompanyText }
    val clickLabel = stringResource(Res.string.list_row_click_cd, displayCompany)
    val noRoleText = stringResource(Res.string.list_row_no_role)
    val roleDesc = model.roleTitle.trim().ifEmpty { noRoleText }
    val fitDesc = model.fitScoreText?.let { ", $it" }.orEmpty()
    val rowContentDescription = stringResource(
        Res.string.list_row_cd,
        displayCompany,
        roleDesc,
        model.statusDisplayName,
        fitDesc,
    )

    if (isCompact) {
        CompactJobApplicationRow(
            model = model,
            displayCompany = displayCompany,
            roleDesc = roleDesc,
            clickLabel = clickLabel,
            rowContentDescription = rowContentDescription,
            onIntent = onIntent,
            modifier = modifier,
        )
    } else {
        WideJobApplicationRow(
            model = model,
            displayCompany = displayCompany,
            roleDesc = roleDesc,
            clickLabel = clickLabel,
            rowContentDescription = rowContentDescription,
            onIntent = onIntent,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactJobApplicationRow(
    model: JobApplicationItemUiModel,
    displayCompany: String,
    roleDesc: String,
    clickLabel: String,
    rowContentDescription: String,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) CHEVRON_ROTATION_EXPANDED else CHEVRON_ROTATION_COLLAPSED,
    )
    val createdDate = model.createdDateOrNull()
    val createdLineText = if (createdDate != null) {
        stringResource(Res.string.list_row_created, createdDate)
    } else {
        stringResource(Res.string.list_row_created_unavailable)
    }

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
            CompanyLogoImage(
                companyName = displayCompany,
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
                StatusBadge(
                    status = model.status,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))

            Box(
                modifier = Modifier
                    .width(CHEVRON_TOUCH_WIDTH)
                    .height(COMPACT_LOGO_SIZE)
                    .clickable { isExpanded = !isExpanded },
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
                onIntent = onIntent,
            )
        }
    }
}

@OptIn(ExperimentalGridApi::class)
@Composable
private fun ExpandedApplicationMetadataGrid(
    model: JobApplicationItemUiModel,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
) {
    val gridGap = MaterialTheme.spacing.small
    val fitScoreValue = model.fitScoreOrNull()
    val appliedDateValue = model.appliedDateOrNull()
    val updatedDateValue = model.updatedDateOrNull()

    val notEvaluatedText = stringResource(Res.string.data_not_evaluated)
    val notAppliedText = stringResource(Res.string.data_not_applied)
    val notAvailableText = stringResource(Res.string.data_not_available)

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

        GridValue(
            text = fitScoreValue ?: notEvaluatedText,
            isPlaceholder = fitScoreValue == null,
        )
        GridValue(
            text = appliedDateValue ?: notAppliedText,
            isPlaceholder = appliedDateValue == null,
        )
        GridValue(
            text = updatedDateValue ?: notAvailableText,
            isPlaceholder = updatedDateValue == null,
        )

        Button(
            onClick = { onIntent(model.onClickIntent) },
            modifier = Modifier.gridItem(columnSpan = GRID_COLUMNS),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(stringResource(Res.string.list_row_action_view_details))
        }
    }
}

@Composable
private fun WideJobApplicationRow(
    model: JobApplicationItemUiModel,
    displayCompany: String,
    roleDesc: String,
    clickLabel: String,
    rowContentDescription: String,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notAvailableText = stringResource(Res.string.data_not_available)
    val notAppliedText = stringResource(Res.string.data_not_applied)
    val createdDate = model.createdDateDisplay(backup = notAvailableText)
    val appliedDate = model.appliedDateDisplay(backup = notAppliedText)

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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                CompanyLogoImage(
                    companyName = displayCompany,
                    companyInitial = model.companyInitial,
                    size = WIDE_LOGO_SIZE,
                    textStyle = MaterialTheme.typography.titleMedium,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(WIDE_TEXT_STACK_SPACING),
                ) {
                    Text(
                        text = roleDesc,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displayCompany,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(
                            Res.string.list_row_created_applied,
                            createdDate,
                            appliedDate,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = model.updatedDateDisplay(backup = notAvailableText),
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
