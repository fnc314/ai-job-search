package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes

@Composable
public fun FitBadge(
    score: Int,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    // Semantic M3 color coding based on fit score tiers:
    // >= 80: Strong fit (primary container)
    // >= 60: Moderate fit (secondary container)
    // < 60: Stretch / lower fit (surface variant)
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
