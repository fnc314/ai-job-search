package dev.fnc314.tools.jobtracker.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.util.displayName

@Composable
fun StatusBadge(
    status: TrackerStatus,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = 1,
) {
    val displayName = status.displayName()

    // Determine standard M3 semantic container and content colors
    val (containerColor, contentColor) = when (status) {
        TrackerStatus.DRAFTING -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        TrackerStatus.APPLIED -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        TrackerStatus.INTERVIEW -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        TrackerStatus.OFFER_EXPECTED,
        TrackerStatus.HIRED -> Pair(
            Color(0xFFD1E7DD),
            Color(0xFF0F5132)
        )
        TrackerStatus.REJECTED -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        TrackerStatus.NO_RESPONSE -> Pair(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.outline
        )
        TrackerStatus.OFFER_DECLINED,
        TrackerStatus.APPLICATION_WITHDRAWN -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
