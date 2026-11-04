package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.util.displayName

@Composable
fun FilterChipItem(
    filterType: FilterJobApplicationsUseCase.FilterType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = filterType.displayName(),
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier
            .wrapContentWidth()
            .semantics { contentDescription = "Filter: ${filterType.displayName()}" },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}
