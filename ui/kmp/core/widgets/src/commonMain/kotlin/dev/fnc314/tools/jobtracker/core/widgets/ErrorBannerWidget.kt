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

@Composable
public fun ErrorBannerWidget(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
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
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = message.ifBlank { stringResource(Res.string.widget_error_default) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(Res.string.widget_retry_action))
                }
            }
        }
    }
}
