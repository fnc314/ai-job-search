package dev.fnc314.aijobtracker.frontend.features.companieslist.ui.component

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
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.CompanyCardUiModel

/**
 * Summary dropdown popup content for the Companies list screen.
 *
 * Displays total companies listed along with a breakdown by industry.
 */
@Composable
public fun CompaniesListSummaryMenu(
    companies: List<CompanyCardUiModel>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCompanies = companies.size
    val industryCounts = companies
        .map { it.industry?.takeIf { ind -> ind.isNotBlank() } ?: "Unspecified" }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = "Companies Summary",
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
                text = "Total Companies",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$totalCompanies",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = "Industry Breakdown",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        if (industryCounts.isEmpty()) {
            Text(
                text = "No companies found.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            industryCounts.take(8).forEach { (industry, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(
                            text = industry,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (industryCounts.size > 8) {
                val remainingCount = industryCounts.drop(8).sumOf { it.second }
                Text(
                    text = "+${industryCounts.size - 8} more (${remainingCount} companies)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
