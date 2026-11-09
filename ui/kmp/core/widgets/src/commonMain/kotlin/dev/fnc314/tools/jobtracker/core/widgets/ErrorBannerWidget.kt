package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.widget_error_default
import fnc314jobsearchtracker.core_widgets.generated.resources.widget_retry_action
import org.jetbrains.compose.resources.stringResource

/**
 * Prominent Material 3 error card alerting the user to operation failures or network disconnects.
 *
 * Utilizes [MaterialTheme.colorScheme.errorContainer] and provides optional retry and secondary action buttons.
 *
 * @param message Descriptive error message describing the failure.
 * @param modifier Layout modifier applied to the card.
 * @param onRetry Optional callback invoked when the user taps the retry button.
 * @param secondaryAction Optional label and callback pair for an alternative action (e.g. "Go Back").
 */
@Composable
public fun ErrorBannerWidget(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
): Unit {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message.ifBlank { stringResource(Res.string.widget_error_default) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (onRetry != null || secondaryAction != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    if (secondaryAction != null) {
                        Button(onClick = secondaryAction.second) {
                            Text(secondaryAction.first)
                        }
                    }
                    if (onRetry != null) {
                        Button(onClick = onRetry) {
                            Text(stringResource(Res.string.widget_retry_action))
                        }
                    }
                }
            }
        }
    }
}
