package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing

/**
 * Centered empty state placeholder composable displayed when a list, filter query, or section yields zero items.
 *
 * @param title Primary heading message (e.g. `"No Applications Found"`).
 * @param modifier Layout modifier applied to the container.
 * @param subtitle Secondary explanatory text providing guidance on how to populate data.
 * @param icon Optional decorative hero icon [ImageVector].
 * @param action Optional action button slot (e.g., `"Reset Filters"` or `"Add Application"`).
 */
@Composable
public fun EmptyStateWidget(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
): Unit {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.iconSizes.extraLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                action()
            }
        }
    }
}
