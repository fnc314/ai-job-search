package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import kotlin.math.roundToInt

/**
 * Renders a complete row of application status metric counts paired with a second row of
 * percentage breakdowns underneath in matching semantic colors.
 *
 * Adheres to Material 3 dynamic color scheme and avoids hardcoded color literals:
 * - Draft: [androidx.compose.material3.ColorScheme.onSurfaceVariant]
 * - Submitted: [androidx.compose.material3.ColorScheme.tertiary]
 * - Interviewing: [androidx.compose.material3.ColorScheme.secondary]
 * - Offer: [androidx.compose.material3.ColorScheme.primary]
 * - Rejected: [androidx.compose.material3.ColorScheme.error]
 */
@Composable
public fun CompanyMetricsTextRow(
    draftCount: Int,
    draftPercentage: Float,
    submittedCount: Int,
    submittedPercentage: Float,
    interviewingCount: Int,
    interviewingPercentage: Float,
    offerCount: Int,
    offerPercentage: Float,
    rejectedCount: Int,
    rejectedPercentage: Float,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        MetricItem(
            label = "DRAFT",
            count = draftCount,
            percentage = draftPercentage,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        MetricItem(
            label = "SUBMITTED",
            count = submittedCount,
            percentage = submittedPercentage,
            color = MaterialTheme.colorScheme.tertiary
        ),
        MetricItem(
            label = "INTERVIEW",
            count = interviewingCount,
            percentage = interviewingPercentage,
            color = MaterialTheme.colorScheme.secondary
        ),
        MetricItem(
            label = "OFFER",
            count = offerCount,
            percentage = offerPercentage,
            color = MaterialTheme.colorScheme.primary
        ),
        MetricItem(
            label = "REJECTED",
            count = rejectedCount,
            percentage = rejectedPercentage,
            color = MaterialTheme.colorScheme.error
        ),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
    ) {
        // Row 1: Label and Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in items) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${item.label} (${item.count})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = item.color
                    )
                    Text(
                        text = "${(item.percentage * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = item.color
                    )
                }
            }
        }
    }
}

private data class MetricItem(
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
)
