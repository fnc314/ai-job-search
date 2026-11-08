package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationItemUiModel
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListUiIntent
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.list_row_cd
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.list_row_click_cd
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.list_row_no_role
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun JobApplicationRow(
    model: JobApplicationItemUiModel,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val clickLabel: String = stringResource(Res.string.list_row_click_cd, model.companyName)
    val noRoleText: String = stringResource(Res.string.list_row_no_role)
    val roleDesc: String = if (model.roleTitle.isNotEmpty()) model.roleTitle else noRoleText
    val fitDesc: String = model.fitScoreText?.let { ", $it" } ?: ""
    val rowContentDescription: String = stringResource(
        Res.string.list_row_cd,
        model.companyName,
        roleDesc,
        model.statusDisplayName,
        fitDesc,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onIntent(model.onClickIntent) },
                onClickLabel = clickLabel,
            )
            .padding(MaterialTheme.spacing.medium)
            .semantics {
                contentDescription = rowContentDescription
            },
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
    ) {
        // Top line: Company and Status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.companyName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            StatusBadge(
                status = model.status,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }

        // Subtitle line: Role and Timestamp / Fit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (model.roleTitle.isNotEmpty()) {
                Text(
                    text = model.roleTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                if (model.fitScore != null) {
                    FitBadge(score = model.fitScore)
                }

                if (model.formattedCreatedDate.isNotEmpty()) {
                    Text(
                        text = model.formattedCreatedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
