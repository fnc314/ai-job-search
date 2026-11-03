package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase

@Composable
fun FilterChipItem(
    filterType: FilterJobApplicationsUseCase.FilterType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (filterType) {
        FilterJobApplicationsUseCase.FilterType.ALL -> "All"
        FilterJobApplicationsUseCase.FilterType.OPEN -> "Open"
        FilterJobApplicationsUseCase.FilterType.LEADS -> "Leads"
        FilterJobApplicationsUseCase.FilterType.REJECTED -> "Rejected"
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
            .wrapContentWidth()
            .semantics { contentDescription = "Filter: $label" },
    )
}
