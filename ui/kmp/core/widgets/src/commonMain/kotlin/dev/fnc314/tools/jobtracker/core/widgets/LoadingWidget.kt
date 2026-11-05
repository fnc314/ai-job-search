package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.widget_loading_default
import org.jetbrains.compose.resources.stringResource

@Composable
public fun LoadingWidget(
    modifier: Modifier = Modifier,
    message: String? = null,
): Unit {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(MaterialTheme.componentSizes.iconButtonSize),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Text(
                text = message ?: stringResource(Res.string.widget_loading_default),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
