package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.aijobtracker.frontend.core.designsystem.iconSize
import dev.fnc314.aijobtracker.frontend.core.designsystem.iconSizes
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_applied
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_collapse_section
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_drafting
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_expand_section
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_interviewing
import dev.fnc314.aijobtracker.frontend.features_job_applications_list_ui.generated.resources.list_section_rejected
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
 * @param isExpanded Whether this section's application list is currently expanded.
 * @param onToggleExpand Callback invoked to expand or collapse this section.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun JobApplicationsSectionHeader(
  sectionOrder: Int,
  count: Int,
  modifier: Modifier = Modifier,
  isExpanded: Boolean = true,
  onToggleExpand: (() -> Unit),
) {
  val title = resolveSectionTitle(sectionOrder)
  val icon = resolveSectionIcon(sectionOrder)
  val chevronRotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

  val containerColor = when (sectionOrder) {
    1 -> MaterialTheme.colorScheme.primaryContainer
    2 -> MaterialTheme.colorScheme.tertiaryContainer
    3 -> MaterialTheme.colorScheme.surfaceContainerHigh
    4 -> MaterialTheme.colorScheme.surfaceContainerLow
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
    tonalElevation = if (sectionOrder == 1) {
      MaterialTheme.spacing.extraSmall
    } else {
      MaterialTheme.spacing.micro
    },
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = MaterialTheme.spacing.extraSmall),
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(
          role = Role.Button,
          onClick = onToggleExpand,
        )
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.small,
        ),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .clip(MaterialTheme.shapes.small)
          .padding(
            vertical = MaterialTheme.spacing.micro,
            horizontal = MaterialTheme.spacing.micro,
          ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = if (isExpanded) {
            Res.string.list_section_collapse_section
          } else {
            Res.string.list_section_expand_section
          }.let { stringResource(it) },
          modifier = Modifier
            .size(MaterialTheme.iconSize.standard)
            .rotate(degrees = chevronRotation),
          tint = contentColor,
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(
            size = MaterialTheme.iconSizes.standard,
          ),
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
          modifier = Modifier.padding(
            start = MaterialTheme.spacing.extraSmall,
          ),
        ) {
          Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(
              horizontal = MaterialTheme.spacing.small,
              vertical = MaterialTheme.spacing.micro,
            ),
          )
        }
      }
    }
  }
}
