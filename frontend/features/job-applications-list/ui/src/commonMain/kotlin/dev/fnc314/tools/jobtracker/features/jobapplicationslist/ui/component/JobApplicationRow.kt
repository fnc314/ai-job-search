package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.fnc314.tools.jobtracker.core.widgets.JobApplicationCard
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationItemUiModel
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationsListUiIntent
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.toCardUiModel

/**
 * Renders an application row using the canonical shared [JobApplicationCard] widget.
 */
@Composable
internal fun JobApplicationRow(
    model: JobApplicationItemUiModel,
    onIntent: (JobApplicationsListUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    JobApplicationCard(
        model = model.toCardUiModel(),
        onApplicationClick = { onIntent(model.onClickIntent) },
        modifier = modifier,
    )
}
