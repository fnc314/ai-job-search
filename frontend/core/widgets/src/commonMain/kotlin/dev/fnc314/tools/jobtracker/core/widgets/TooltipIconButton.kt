package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Accessible Material 3 icon button wrapping a [PlainTooltip] displayed upon pointer hover and accessibility focus.
 *
 * Enforces accessibility standards by attaching the tooltip text to the button's semantic [contentDescription].
 *
 * @param tooltip Descriptive text shown in the tooltip and read by accessibility screen readers.
 * @param icon The [ImageVector] rendered inside the button.
 * @param onClick Invoked when the user clicks or taps the button.
 * @param modifier Layout modifier applied to the button.
 * @param enabled Whether the button is interactive. Defaults to `true`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun TooltipIconButton(
    tooltip: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
): Unit {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.semantics { contentDescription = tooltip },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
            )
        }
    }
}
