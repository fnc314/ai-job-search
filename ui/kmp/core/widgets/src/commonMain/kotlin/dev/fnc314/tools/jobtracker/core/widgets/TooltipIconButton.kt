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
 * Standard Material 3 Icon Button that exposes clear accessibility descriptions
 * and displays an M3 PlainTooltip on hover and keyboard focus.
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
