package dev.fnc314.tools.jobtracker.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.util.displayName

@Composable
fun StatusBadge(
    status: TrackerStatus,
    modifier: Modifier = Modifier,
) {
    val displayName = status.displayName()
    val (icon, color) = when (status) {
        TrackerStatus.DRAFTING -> Pair(null, MaterialTheme.colorScheme.primary)
        TrackerStatus.APPLIED -> Pair(null, MaterialTheme.colorScheme.tertiary)
        TrackerStatus.INTERVIEW -> Pair(null, MaterialTheme.colorScheme.secondary)
        TrackerStatus.OFFER_EXPECTED -> Pair(null, MaterialTheme.colorScheme.inversePrimary)
        TrackerStatus.HIRED -> Pair(null, MaterialTheme.colorScheme.primary)
        TrackerStatus.REJECTED -> Pair(null, MaterialTheme.colorScheme.error)
        TrackerStatus.NO_RESPONSE -> Pair(null, MaterialTheme.colorScheme.outline)
        TrackerStatus.OFFER_DECLINED -> Pair(null, MaterialTheme.colorScheme.error)
        TrackerStatus.APPLICATION_WITHDRAWN -> Pair(null, MaterialTheme.colorScheme.outline)
        else -> Pair(null, MaterialTheme.colorScheme.outline)
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
        )
    }
}
