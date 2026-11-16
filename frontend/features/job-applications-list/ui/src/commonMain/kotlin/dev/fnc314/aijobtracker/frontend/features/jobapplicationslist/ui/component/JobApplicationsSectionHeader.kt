package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.Res
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_applied
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_drafting
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_interviewing
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_jump_menu_title
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_jump_tooltip
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_section_rejected
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves the localized display title for a section order (1..4).
 */
@Composable
public fun resolveSectionTitle(sectionOrder: Int): String = when (sectionOrder) {
    1 -> stringResource(Res.string.list_section_interviewing)
    2 -> stringResource(Res.string.list_section_drafting)
    3 -> stringResource(Res.string.list_section_applied)
    4 -> stringResource(Res.string.list_section_rejected)
    else -> stringResource(Res.string.list_section_applied)
}

/**
 * Resolves the category icon for a section order.
 */
public fun resolveSectionIcon(sectionOrder: Int): ImageVector = when (sectionOrder) {
    1 -> Icons.Default.Star
    2 -> Icons.Default.Edit
    3 -> Icons.Default.CheckCircle
    4 -> Icons.Default.Cancel
    else -> Icons.Default.Bookmark
}

/**
 * Section header composable for the Job Applications list.
 *
 * When rendered in a `LazyColumn.stickyHeader`, sticks beneath the `CenterAlignedTopAppBar`
 * and provides a clickable jump action to quickly scroll to any other active section.
 *
 * @param sectionOrder Section index (1=Interviewing, 2=In Draft, 3=Applied, 4=Rejected).
 * @param count Number of application items contained in this section.
 * @param availableSections List of all active section orders present in the list.
 * @param onJumpToSection Callback invoked when the user selects a target section to jump to.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun JobApplicationsSectionHeader(
    sectionOrder: Int,
    count: Int,
    availableSections: List<Int>,
    onJumpToSection: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val title = resolveSectionTitle(sectionOrder)
    val icon = resolveSectionIcon(sectionOrder)

    val containerColor = when (sectionOrder) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        3 -> MaterialTheme.colorScheme.surfaceContainerHigh
        4 -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val contentColor = when (sectionOrder) {
        1 -> MaterialTheme.colorScheme.onPrimaryContainer
        2 -> MaterialTheme.colorScheme.onTertiaryContainer
        3 -> MaterialTheme.colorScheme.onSurface
        4 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (sectionOrder == 1) 4.dp else 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.medium,
                    vertical = MaterialTheme.spacing.small,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Surface(
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            if (availableSections.size > 1) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { menuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.list_section_jump_tooltip),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.list_section_jump_tooltip),
                            modifier = Modifier.size(16.dp),
                            tint = contentColor,
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        availableSections
                            .filter { it != sectionOrder }
                            .forEach { targetOrder ->
                                val targetTitle = resolveSectionTitle(targetOrder)
                                val targetIcon = resolveSectionIcon(targetOrder)
                                DropdownMenuItem(
                                    text = { Text(targetTitle) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = targetIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onJumpToSection(targetOrder)
                                    },
                                )
                            }
                    }
                }
            }
        }
    }
}
