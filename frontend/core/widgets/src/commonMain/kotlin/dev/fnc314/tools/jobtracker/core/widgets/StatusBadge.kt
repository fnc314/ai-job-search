package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.designsystem.spacing

/**
 * Material 3 pill badge rendering an application lifecycle [TrackerStatus].
 *
 * Uses the status's theme-aware [TrackerStatus.containerColor] and [TrackerStatus.contentColor]
 * to convey semantic progression states (e.g. green for offer/hired, red for rejected, blue for applied).
 *
 * @param status The [TrackerStatus] to display.
 * @param modifier Layout modifier applied to the outer badge [Surface].
 * @param textAlign Text alignment within the badge container. Defaults to [TextAlign.Start].
 * @param maxLines Maximum text lines. Defaults to `1`.
 */
@Composable
public fun StatusBadge(
    status: TrackerStatus,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
): Unit {
    val displayName = status.displayName()
    val containerColor = status.containerColor()
    val contentColor = status.contentColor()

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
          .border(
            width = MaterialTheme.spacing.hairline,
            color = contentColor,
            shape = MaterialTheme.shapes.extraSmall
          ),
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelMediumEmphasized,
            textAlign = textAlign,
            maxLines = maxLines,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.componentSizes.badgePaddingHorizontal,
                vertical = MaterialTheme.componentSizes.badgePaddingVertical,
            ),
        )
    }
}
