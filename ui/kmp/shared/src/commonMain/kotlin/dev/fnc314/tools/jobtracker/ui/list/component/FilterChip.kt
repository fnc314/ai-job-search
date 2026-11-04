package dev.fnc314.tools.jobtracker.ui.list.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
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
        label = { Text(filterType.displayName()) },
        modifier = modifier
            .wrapContentWidth()
            .semantics { contentDescription = "Filter: ${filterType.displayName()}" },
        contentPadding = PaddingValues(
            all = MaterialTheme.spacing.extraSmall
        ),
        elevation = SelectableChipElevation(
            elevation = 6.dp,
            pressedElevation = 2.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 4.dp,
            draggedElevation = 8.dp,
            disabledElevation = 0.dp
        )
    )
}
