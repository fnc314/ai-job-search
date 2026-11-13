@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Fr
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridScope
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.formatRelativeInstant
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.EventDetailDialog
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.data_missing
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_empty
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_event_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_scheduled
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_transition
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_transition_current
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.timeline_transition_start
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EventsTimeline(
  events: ImmutableList<TrackerEvent>,
  modifier: Modifier = Modifier,
): Unit {
  var selectedEvent by remember { mutableStateOf<TrackerEvent?>(null) }

  if (selectedEvent != null) {
    EventDetailDialog(
      event = selectedEvent!!,
      onDismiss = { selectedEvent = null },
    )
  }

  if (events.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = stringResource(Res.string.timeline_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.none),
  ) {
    events.forEachIndexed { index, event ->
      val isFirst = index == 0
      val isLast = index == events.lastIndex
      val eventCd: String = stringResource(Res.string.timeline_event_cd, event.type.displayName())
      val gridSizes = object {
        val columns = object {
          val graphicNode = GridTrackSize.MinContent
          val textNode = GridTrackSize.Flex(Fr(1.0F))
        }
        val gaps = object {
          val column = MaterialTheme.spacing.medium
          val row = MaterialTheme.spacing.none
        }
        val rows = object {
          val all = GridTrackSize.Fixed(MaterialTheme.componentSizes.timelineNodeSize)
        }
      }
      Grid(
        config = {
          column(
            size = gridSizes.columns.graphicNode,
          )
          column(
            size = gridSizes.columns.textNode
          )
          gap(
            column = gridSizes.gaps.column,
            row = gridSizes.gaps.row
          )
          row(
            size = gridSizes.rows.all
          )
          row(
            size = gridSizes.rows.all
          )
          row(
            size = gridSizes.rows.all
          )

          area(
            areaId = "graphic-top",
            row = 1,
            rowSpan = 1,
            column = 1,
            columnSpan = 1,
          )
          area(
            areaId = "graphic-icon",
            row = 2,
            rowSpan = 1,
            column = 1,
            columnSpan = 1,
          )
          area(
            areaId = "graphic-bottom",
            row = 3,
            rowSpan = 1,
            column = 1,
            columnSpan = 1,
          )
          area(
            areaId = "text",
            row = 1,
            rowSpan = 3,
            column = 2,
            columnSpan = 1,
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .clip(MaterialTheme.shapes.small)
          .clickable { selectedEvent = event }
          .padding(
            horizontal = MaterialTheme.spacing.medium,
          )
          .semantics { contentDescription = eventCd }
      ) {

        context(this) {
          EventsTimelineGraphic(
            event = event,
            isFirst = isFirst,
            isLast = isLast,
          )

          EventsTimelineDescription(
            event = event,
          )
        }

      }
    }
  }
}

@Composable
context(gridScope: GridScope)
private fun EventsTimelineDescription(
  event: TrackerEvent,
) {
  with(gridScope) {
    // Event Content Card
    OutlinedCard(
      shape = MaterialTheme.shapes.medium,
      modifier = Modifier
        .gridItem(areaId = "text")
        .fillMaxSize()
        .padding(vertical = MaterialTheme.spacing.small),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start,
      ) {
        Text(
          text = event.type.displayName(),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )

        event.note?.let { note ->

          Text(
            text = note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
            maxLines = 8,
            modifier = Modifier.wrapContentWidth(
              align = Alignment.Start,
            )
          )
        }

        event.scheduledAt?.let { scheduledAt ->
          if (scheduledAt != Instant.DISTANT_PAST) {
            Text(
              text = stringResource(
                Res.string.timeline_scheduled,
                scheduledAt
              ),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        if (event.statusFrom != null || event.statusTo != null) {
          val transitionFrom = event.statusFrom?.displayName()
            ?: stringResource(Res.string.timeline_transition_start)
          val transitionTo = event.statusTo?.displayName()
            ?: stringResource(Res.string.timeline_transition_current)

          Text(
            text = stringResource(
              Res.string.timeline_transition,
              transitionFrom,
              transitionTo,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        Text(
          text = formatRelativeInstant(event.createdAt)
            ?: stringResource(Res.string.data_missing),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
context(gridScope: GridScope)
private fun EventsTimelineGraphic(
  event: TrackerEvent,
  isFirst: Boolean,
  isLast: Boolean,
) {
  with(gridScope) {
    if (!isFirst) {
      Box(
        modifier = Modifier
          .gridItem(areaId = "graphic-top")
          .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        Box(
          modifier = Modifier
            .width(MaterialTheme.componentSizes.timelineLineWidth)
            .height(MaterialTheme.componentSizes.timelineNodeSize)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
    }
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier
        .gridItem(areaId = "graphic-icon")
        .size(MaterialTheme.componentSizes.timelineNodeSize)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = event.type.icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(MaterialTheme.iconSizes.medium),
        )
      }
    }
    if (!isLast) {
      Box(
        modifier = Modifier
          .gridItem(areaId = "graphic-bottom")
          .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
      ) {
        Box(
          modifier = Modifier
            .width(MaterialTheme.componentSizes.timelineLineWidth)
            .height(MaterialTheme.componentSizes.timelineNodeSize)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
    }
  }
}