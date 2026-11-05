package dev.fnc314.tools.jobtracker.core.widgets

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

@Composable
public fun StatusBadge(
    status: TrackerStatus,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    val displayName = status.displayName()

    // Determine standard M3 semantic container and content colors
    val containerColor = status.containerColor()
    val contentColor = status.contentColor()

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier,
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = textAlign,
            maxLines = maxLines,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.componentSizes.badgePaddingHorizontal,
                vertical = MaterialTheme.componentSizes.badgePaddingVertical
            ),
        )
    }
}
