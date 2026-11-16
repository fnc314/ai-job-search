package dev.fnc314.aijobtracker.frontend.features.companydetails.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.widgets.formatRelativeInstant
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.CompanyDetailsUiModel
import kotlin.time.Instant

/**
 * Summary dropdown popup content for the Company Details screen.
 *
 * Displays total open applications, industry, and the most recent event date across company applications.
 */
@Composable
internal fun CompanyDetailsSummaryMenu(
    details: CompanyDetailsUiModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openAppsCount = details.draftCount + details.submittedCount + details.interviewingCount
    val mostRecentInstant = details.applications
        .mapNotNull { app ->
            listOf(app.updatedAt, app.createdAt, app.firstActivityAt)
                .filter { it != Instant.DISTANT_PAST && it != Instant.DISTANT_FUTURE }
                .maxOrNull()
        }
        .maxOrNull()

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = "Company Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        HorizontalDivider(color = DividerDefaults.color)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total Open Applications",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$openAppsCount",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Industry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = details.industry ?: "Unspecified",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Most Recent Event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val eventDateText = mostRecentInstant?.let { formatRelativeInstant(it) } ?: "None"
            Text(
                text = eventDateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
