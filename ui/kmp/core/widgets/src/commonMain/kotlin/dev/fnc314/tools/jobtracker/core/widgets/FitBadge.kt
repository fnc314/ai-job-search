package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes

/**
 * Material 3 pill badge displaying a numerical candidate-to-role fit rating score (0 to 100).
 *
 * Automatically applies tiered semantic container colors:
 * - Score >= 80: Strong fit (Primary container)
 * - Score >= 60: Moderate fit (Secondary container)
 * - Score < 60: Stretch or lower alignment (Surface variant)
 *
 * @param score Calculated fit rating (0..100).
 * @param modifier Layout modifier applied to the outer badge [Surface].
 * @param textAlign Text alignment within the badge container. Defaults to [TextAlign.Start].
 * @param maxLines Maximum text lines. Defaults to `1`.
 */
@Composable
public fun FitBadge(
    score: Int,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
): Unit {
    val (containerColor, contentColor) = when {
        score >= 80 -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        score >= 60 -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Text(
            text = "Fit $score",
            style = MaterialTheme.typography.labelSmall,
            textAlign = textAlign,
            maxLines = maxLines,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.componentSizes.badgePaddingHorizontal,
                vertical = MaterialTheme.componentSizes.badgePaddingVertical,
            ),
        )
    }
}
