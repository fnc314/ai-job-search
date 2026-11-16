package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.JobApplicationItemUiModel

/**
 * Summary dropdown popup content for the Job Applications list screen.
 *
 * Displays total counts broken down by Applied, Interviewing, Rejected, and Draft.
 */
@Composable
public fun JobApplicationsSummaryMenu(
    applications: List<JobApplicationItemUiModel>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appliedCount = applications.count {
        it.status == TrackerStatus.Known.APPLIED
    }
    val interviewingCount = applications.count {
        it.status == TrackerStatus.Known.INTERVIEW
    }
    val rejectedCount = applications.count {
        it.status == TrackerStatus.Known.REJECTED ||
            it.status == TrackerStatus.Known.NO_RESPONSE ||
            it.status == TrackerStatus.Known.APPLICATION_WITHDRAWN ||
            it.status == TrackerStatus.Known.OFFER_DECLINED
    }
    val draftCount = applications.count {
        it.status == TrackerStatus.Known.DRAFTING || it.status == TrackerStatus.Known.LEAD
    }

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = "Applications Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${applications.size} total tracked applications",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        SummaryRow(
            label = "Applied / Submitted",
            count = appliedCount,
            dotColor = Color(0xFF2563EB),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            label = "Interviewing",
            count = interviewingCount,
            dotColor = Color(0xFF10B981),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            label = "Rejected / Archived",
            count = rejectedCount,
            dotColor = Color(0xFFEF4444),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryRow(
            label = "Draft / Saved",
            count = draftCount,
            dotColor = Color(0xFFF59E0B),
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    count: Int,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(10.dp),
                shape = CircleShape,
                color = dotColor,
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = dotColor.copy(alpha = 0.12f),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = dotColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
