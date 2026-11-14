@file:OptIn(ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.GridTrackSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val CHEVRON_ROTATION_EXPANDED = 90f
private const val AREA_THUMBNAIL = "thumbnail"
private const val AREA_HEADER_MAIN = "headerMain"
private const val AREA_HEADER_TRAILING = "headerTrailing"
private const val AREA_CHEVRON = "chevron"
private const val AREA_BODY = "body"
private const val AREA_ACTIONS = "actions"
private val HEADER_STACK_SPACING = 2.dp

/**
 * Surface color and elevation variations supported by [TopLevelCard].
 */
public enum class TopLevelCardSurfaceVariant {

  /** Default flat surface (`MaterialTheme.colorScheme.surface`, `0.dp` elevation). */
  Surface,

  /** Low-elevation container surface (`MaterialTheme.colorScheme.surfaceContainerLow`, `1.dp` elevation). */
  Elevated,

  /** Medium tonal container surface (`MaterialTheme.colorScheme.surfaceContainer`, `3.dp` elevation). */
  FilledTonal,

  /** High-emphasis container surface (`MaterialTheme.colorScheme.surfaceContainerHigh`, `6.dp` elevation). */
  Highlighted,
}

/**
 * Resolves the container [Color] for this [TopLevelCardSurfaceVariant].
 */
@Composable
public fun TopLevelCardSurfaceVariant.containerColor(): Color = when (this) {
  TopLevelCardSurfaceVariant.Surface -> MaterialTheme.colorScheme.surface
  TopLevelCardSurfaceVariant.Elevated -> MaterialTheme.colorScheme.surfaceContainerLow
  TopLevelCardSurfaceVariant.FilledTonal -> MaterialTheme.colorScheme.surfaceContainer
  TopLevelCardSurfaceVariant.Highlighted -> MaterialTheme.colorScheme.surfaceContainerHigh
}

@Composable
public fun TopLevelCardSurfaceVariant.cardColors(): CardColors =
  CardDefaults.cardColors()
    .copy(
      containerColor = containerColor(),
      contentColor = contentColorFor(
        backgroundColor = containerColor()
      ),
    )

/**
 * Resolves the default tonal/shadow [Dp] elevation for this [TopLevelCardSurfaceVariant].
 */
@Composable
public fun TopLevelCardSurfaceVariant.elevation(): Dp = when (this) {
  TopLevelCardSurfaceVariant.Surface -> MaterialTheme.componentSizes.cardElevationDefault
  TopLevelCardSurfaceVariant.Elevated -> MaterialTheme.componentSizes.cardElevationLow
  TopLevelCardSurfaceVariant.FilledTonal -> MaterialTheme.componentSizes.cardElevationMedium
  TopLevelCardSurfaceVariant.Highlighted -> MaterialTheme.componentSizes.cardElevationHigh
}

/**
 * Describes an action button rendered inside the action region of a [TopLevelCard].
 *
 * All actions passed to [TopLevelCard] receive equal horizontal track allocation (`1.fr`),
 * consistent button height, and uniform `MaterialTheme.shapes.small` corner geometry.
 *
 * @property label Localized text label displayed inside the button.
 * @property onClick Callback invoked when the action button is clicked.
 * @property enabled Whether the action button is currently interactive.
 * @property contentDescription Optional accessibility content description override.
 * @property isPrimary When `true`, renders as a filled primary [Button]; when `false`, renders as an [OutlinedButton].
 */
public data class TopLevelCardAction(
  public val label: String,
  public val onClick: () -> Unit,
  public val enabled: Boolean = true,
  public val contentDescription: String? = null,
  public val isPrimary: Boolean = true,
)

/**
 * Unified responsive top-level card component backed by Compose [Grid].
 *
 * Defines structured grid regions exposed to callers as `@Composable` slot inputs:
 * - [thumbnail]: Leading visual slot (such as [CompanyLogoImage]).
 * - [title]: Primary title slot.
 * - [subtitle]: Secondary subtitle slot.
 * - [compactSummary]: Summary metadata shown beneath [subtitle] on compact viewports.
 * - [wideTrailingHeader]: Trailing header metadata aligned to the top-right on wide viewports.
 * - [wideBody]: Full-width body region displayed on wide viewports (or when no [expandedContent] is used).
 * - [expandedContent]: Collapsible detail region revealed on compact viewports when expanded.
 * - [actions]: Equal-width action buttons rendered in a single [Grid] row.
 *
 * Interaction behavior adapts automatically to viewport width and hidden content presence:
 * 1. **Compact viewport with hidden content** (`isCompact && expandedContent != null`):
 *    Tapping the card or its trailing chevron toggles expansion of [expandedContent] and [actions].
 * 2. **Wide viewport or no hidden content** (`!isCompact || expandedContent == null`):
 *    The chevron is omitted and the card acts directly as a navigation target invoking [onClick]
 *    (when [onClick] is provided).
 *
 * @param title Primary title composable slot.
 * @param modifier Layout modifier applied to the root [Card].
 * @param onClick Optional navigation callback invoked when the card acts as a direct navigation target.
 * @param contentDescription Optional accessibility description applied to the root [Card].
 * @param onClickLabel Optional accessibility click label applied to the root [Card].
 * @param variant Surface color and elevation treatment ([TopLevelCardSurfaceVariant]).
 * @param isExpanded Optional external expansion state. When `null`, expansion state is managed internally.
 * @param onToggleExpand Optional callback to toggle external expansion state.
 * @param showActionsWhenNotCollapsible Whether [actions] should also be rendered when the card is in non-collapsible/wide mode.
 * @param thumbnail Optional leading visual slot (e.g., [CompanyLogoImage]).
 * @param subtitle Optional secondary title/metadata slot.
 * @param compactSummary Optional summary slot shown in the header stack on compact viewports.
 * @param wideTrailingHeader Optional trailing header slot shown on wide viewports (or non-collapsible cards).
 * @param wideBody Optional body region rendered on wide viewports (or non-collapsible cards).
 * @param expandedContent Optional collapsible region revealed on compact viewports when expanded.
 * @param actions List of [TopLevelCardAction] items given equal layout treatment in the card's action bar.
 */
@Composable
public fun TopLevelCard(
  title: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  contentDescription: String? = null,
  onClickLabel: String? = null,
  variant: TopLevelCardSurfaceVariant = TopLevelCardSurfaceVariant.Surface,
  isExpanded: Boolean? = null,
  onToggleExpand: (() -> Unit)? = null,
  showActionsWhenNotCollapsible: Boolean = false,
  thumbnail: (@Composable () -> Unit)? = null,
  subtitle: (@Composable () -> Unit)? = null,
  compactSummary: (@Composable () -> Unit)? = null,
  wideTrailingHeader: (@Composable () -> Unit)? = null,
  wideBody: (@Composable () -> Unit)? = null,
  expandedContent: (@Composable () -> Unit)? = null,
  actions: ImmutableList<TopLevelCardAction> = persistentListOf(),
) {
  val isCompact = LocalWindowSizeClass.current.isCompact
  val hasHiddenContent = isCompact && expandedContent != null

  var internalExpanded by rememberSaveable {
    mutableStateOf(
      isExpanded
        ?: false,
    )
  }
  val resolvedExpanded = if (onToggleExpand != null && isExpanded != null) {
    isExpanded
  } else {
    internalExpanded
  }
  val toggleAction: () -> Unit = onToggleExpand
    ?: { internalExpanded = !internalExpanded }

  val cardClickAction: (() -> Unit)? = when {
    hasHiddenContent -> toggleAction
    else -> onClick
  }

  val clickableModifier = if (cardClickAction != null) {
    Modifier.clickable(
      onClick = cardClickAction,
      onClickLabel = onClickLabel,
    )
  } else {
    Modifier
  }

  val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
    Modifier.semantics {
      this.contentDescription = contentDescription
    }
  } else {
    Modifier
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(clickableModifier)
      .then(semanticsModifier),
    colors = variant.cardColors(),
    elevation = CardDefaults.cardElevation(
      defaultElevation = variant.elevation(),
    ),
    shape = MaterialTheme.shapes.medium,
  ) {
    if (hasHiddenContent) {
      CompactCollapsibleCardGrid(
        resolvedExpanded = resolvedExpanded,
        onToggleExpand = toggleAction,
        thumbnail = thumbnail,
        title = title,
        subtitle = subtitle,
        compactSummary = compactSummary,
        expandedContent = expandedContent,
        actions = actions,
      )
    } else {
      DirectNavigationCardGrid(
        isCompact = isCompact,
        showActions = showActionsWhenNotCollapsible && actions.isNotEmpty(),
        thumbnail = thumbnail,
        title = title,
        subtitle = subtitle,
        compactSummary = compactSummary,
        wideTrailingHeader = wideTrailingHeader,
        wideBody = wideBody,
        actions = actions,
      )
    }
  }
}

@Composable
private fun CompactCollapsibleCardGrid(
  resolvedExpanded: Boolean,
  onToggleExpand: () -> Unit,
  thumbnail: (@Composable () -> Unit)?,
  title: @Composable () -> Unit,
  subtitle: (@Composable () -> Unit)?,
  compactSummary: (@Composable () -> Unit)?,
  expandedContent: (@Composable () -> Unit)?,
  actions: ImmutableList<TopLevelCardAction>,
) {
  val chevronRotation by animateFloatAsState(
    targetValue = if (resolvedExpanded) CHEVRON_ROTATION_EXPANDED else CHEVRON_ROTATION_COLLAPSED,
  )
  val chevronTouchWidth = MaterialTheme.componentSizes.chevronTouchWidth
  val chevronIconSize = MaterialTheme.iconSizes.large
  val columnGap = MaterialTheme.spacing.medium
  val rowGap = MaterialTheme.spacing.small

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(MaterialTheme.spacing.medium),
    verticalArrangement = Arrangement.spacedBy(rowGap),
  ) {
    Grid(
      modifier = Modifier.fillMaxWidth(),
      config = {
        if (thumbnail != null) {
          column(GridTrackSize.MinContent)
          column(1.fr)
          column(GridTrackSize.Fixed(chevronTouchWidth))
          row(GridTrackSize.Auto)
          gap(column = columnGap, row = 0.dp)
          area(areaId = AREA_THUMBNAIL, row = 1, column = 1)
          area(areaId = AREA_HEADER_MAIN, row = 1, column = 2)
          area(areaId = AREA_CHEVRON, row = 1, column = 3)
        } else {
          column(1.fr)
          column(GridTrackSize.Fixed(chevronTouchWidth))
          row(GridTrackSize.Auto)
          gap(column = columnGap, row = 0.dp)
          area(areaId = AREA_HEADER_MAIN, row = 1, column = 1)
          area(areaId = AREA_CHEVRON, row = 1, column = 2)
        }
      },
    ) {
      if (thumbnail != null) {
        Box(
          modifier = Modifier.gridItem(areaId = AREA_THUMBNAIL),
          contentAlignment = Alignment.Center,
        ) {
          thumbnail()
        }
      }

      Column(
        modifier = Modifier
          .gridItem(areaId = AREA_HEADER_MAIN)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HEADER_STACK_SPACING),
      ) {
        title()
        subtitle?.invoke()
        compactSummary?.invoke()
      }

      Box(
        modifier = Modifier
          .gridItem(areaId = AREA_CHEVRON)
          .width(chevronTouchWidth)
          .clickable(onClick = onToggleExpand),
        contentAlignment = Alignment.CenterEnd,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          modifier = Modifier
            .size(chevronIconSize)
            .rotate(chevronRotation),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    AnimatedVisibility(visible = resolvedExpanded) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        expandedContent?.invoke()
        if (actions.isNotEmpty()) {
          TopLevelCardActionsGrid(actions = actions)
        }
      }
    }
  }
}

@Composable
private fun DirectNavigationCardGrid(
  isCompact: Boolean,
  showActions: Boolean,
  thumbnail: (@Composable () -> Unit)?,
  title: @Composable () -> Unit,
  subtitle: (@Composable () -> Unit)?,
  compactSummary: (@Composable () -> Unit)?,
  wideTrailingHeader: (@Composable () -> Unit)?,
  wideBody: (@Composable () -> Unit)?,
  actions: ImmutableList<TopLevelCardAction>,
) {
  val columnGap = MaterialTheme.spacing.medium
  val rowGap = MaterialTheme.spacing.small
  val hasHeaderslots =
    thumbnail != null || subtitle != null || wideTrailingHeader != null || (isCompact && compactSummary != null)

  Grid(
    modifier = Modifier
      .fillMaxWidth()
      .padding(MaterialTheme.spacing.medium),
    config = {
      val totalColumns = when {
        thumbnail != null && wideTrailingHeader != null -> 3
        thumbnail != null || wideTrailingHeader != null -> 2
        else -> 1
      }

      when {
        thumbnail != null && wideTrailingHeader != null -> {
          column(GridTrackSize.MinContent)
          column(1.fr)
          column(GridTrackSize.MinContent)
        }

        thumbnail != null -> {
          column(GridTrackSize.MinContent)
          column(1.fr)
        }

        wideTrailingHeader != null -> {
          column(1.fr)
          column(GridTrackSize.MinContent)
        }

        else -> {
          column(1.fr)
        }
      }

      var currentRow = 0
      if (hasHeaderslots || wideBody == null) {
        currentRow++
        row(GridTrackSize.Auto)
        when {
          thumbnail != null && wideTrailingHeader != null -> {
            area(areaId = AREA_THUMBNAIL, row = currentRow, column = 1)
            area(areaId = AREA_HEADER_MAIN, row = currentRow, column = 2)
            area(areaId = AREA_HEADER_TRAILING, row = currentRow, column = 3)
          }

          thumbnail != null -> {
            area(areaId = AREA_THUMBNAIL, row = currentRow, column = 1)
            area(areaId = AREA_HEADER_MAIN, row = currentRow, column = 2)
          }

          wideTrailingHeader != null -> {
            area(areaId = AREA_HEADER_MAIN, row = currentRow, column = 1)
            area(areaId = AREA_HEADER_TRAILING, row = currentRow, column = 2)
          }

          else -> {
            area(areaId = AREA_HEADER_MAIN, row = currentRow, column = 1)
          }
        }
      }

      if (wideBody != null) {
        currentRow++
        row(GridTrackSize.Auto)
        area(areaId = AREA_BODY, row = currentRow, column = 1, columnSpan = totalColumns)
      }

      if (showActions) {
        currentRow++
        row(GridTrackSize.Auto)
        area(areaId = AREA_ACTIONS, row = currentRow, column = 1, columnSpan = totalColumns)
      }

      gap(column = columnGap, row = rowGap)
    },
  ) {
    if (hasHeaderslots || wideBody == null) {
      if (thumbnail != null) {
        Box(
          modifier = Modifier.gridItem(areaId = AREA_THUMBNAIL),
          contentAlignment = Alignment.Center,
        ) {
          thumbnail()
        }
      }

      Column(
        modifier = Modifier
          .gridItem(areaId = AREA_HEADER_MAIN)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HEADER_STACK_SPACING),
      ) {
        title()
        subtitle?.invoke()
        if (isCompact) {
          compactSummary?.invoke()
        }
      }

      if (wideTrailingHeader != null) {
        Box(
          modifier = Modifier.gridItem(areaId = AREA_HEADER_TRAILING),
          contentAlignment = Alignment.TopEnd,
        ) {
          wideTrailingHeader()
        }
      }
    }

    if (wideBody != null) {
      Box(
        modifier = Modifier
          .gridItem(areaId = AREA_BODY)
          .fillMaxWidth(),
      ) {
        wideBody()
      }
    }

    if (showActions) {
      Box(
        modifier = Modifier
          .gridItem(areaId = AREA_ACTIONS)
          .fillMaxWidth(),
      ) {
        TopLevelCardActionsGrid(actions = actions)
      }
    }
  }
}

/**
 * Renders a row of [TopLevelCardAction] buttons inside an equal-width (`1.fr`) [Grid] so every
 * action button receives identical width, height, and shape treatment.
 *
 * @param actions Immutable list of [TopLevelCardAction] specifications to render.
 * @param modifier Layout modifier applied to the actions [Grid].
 */
@Composable
public fun TopLevelCardActionsGrid(
  actions: ImmutableList<TopLevelCardAction>,
  modifier: Modifier = Modifier,
) {
  if (actions.isEmpty()) return

  val actionGap = MaterialTheme.spacing.small
  Grid(
    modifier = modifier.fillMaxWidth(),
    config = {
      repeat(actions.size) { column(1.fr) }
      row(GridTrackSize.Auto)
      gap(column = actionGap, row = 0.dp)
    },
  ) {
    actions.forEachIndexed { index, action ->
      val itemModifier = Modifier
        .gridItem(row = 1, column = index + 1)
        .fillMaxWidth()
        .let { base ->
          val cd = action.contentDescription
          if (!cd.isNullOrBlank()) {
            base.semantics { contentDescription = cd }
          } else {
            base
          }
        }

      if (action.isPrimary) {
        Button(
          onClick = action.onClick,
          enabled = action.enabled,
          modifier = itemModifier,
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            text = action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        OutlinedButton(
          onClick = action.onClick,
          enabled = action.enabled,
          modifier = itemModifier,
          shape = MaterialTheme.shapes.small,
        ) {
          Text(
            text = action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
