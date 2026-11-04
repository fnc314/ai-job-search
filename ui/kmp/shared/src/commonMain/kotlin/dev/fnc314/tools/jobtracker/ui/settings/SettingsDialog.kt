package dev.fnc314.tools.jobtracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInUiState
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import kotlinx.serialization.Serializable

@Serializable
enum class UiMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Serializable
data class AppSettings(
    val showDate: Boolean = true,
    val showCompany: Boolean = true,
    val showStatus: Boolean = true,
    val showFit: Boolean = true,
    val showRole: Boolean = true,
    val uiMode: UiMode = UiMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val seedColorHex: String = "#2F5597",
) {
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (uiMode) {
        UiMode.SYSTEM -> systemInDarkTheme
        UiMode.LIGHT -> false
        UiMode.DARK -> true
    }

    val darkTheme: Boolean
        get() = uiMode == UiMode.DARK
}

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SignInViewModel? = null,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                // Theme & Appearance
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // UI Mode Selector (System / Light / Dark)
                    Text(
                        text = "UI Mode",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        UiMode.entries.forEach { mode ->
                            val isSelected = settings.uiMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSettingsChange(settings.copy(uiMode = mode)) },
                                label = {
                                    Text(
                                        when (mode) {
                                            UiMode.SYSTEM -> "System"
                                            UiMode.LIGHT -> "Light"
                                            UiMode.DARK -> "Dark"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Seed Color Swatches
                    Text(
                        text = "Accent Color Seed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SeedColors.All.forEach { (name, color) ->
                            val hexString = color.toHexString()
                            val isSelected = settings.seedColorHex.equals(hexString, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        onSettingsChange(settings.copy(seedColorHex = hexString))
                                    }
                                    .semantics { contentDescription = "$name theme color" }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                width = 3.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }

                    // Custom Seed Hex Field
                    OutlinedTextField(
                        value = settings.seedColorHex,
                        onValueChange = {
                            keyboardController?.hide()
                            onSettingsChange(settings.copy(seedColorHex = it))
                        },
                        label = { Text("Custom Seed Hex") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Custom theme seed color hex value" },
                        singleLine = true,
                    )
                }

                HorizontalDivider()

                // Column visibility
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
                    Text(
                        text = "Column Visibility",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    CheckboxRow(
                        label = "Date",
                        checked = settings.showDate,
                        onCheckedChange = { onSettingsChange(settings.copy(showDate = it)) },
                    )
                    CheckboxRow(
                        label = "Company",
                        checked = settings.showCompany,
                        onCheckedChange = { onSettingsChange(settings.copy(showCompany = it)) },
                    )
                    CheckboxRow(
                        label = "Status",
                        checked = settings.showStatus,
                        onCheckedChange = { onSettingsChange(settings.copy(showStatus = it)) },
                    )
                    CheckboxRow(
                        label = "Fit Rating",
                        checked = settings.showFit,
                        onCheckedChange = { onSettingsChange(settings.copy(showFit = it)) },
                    )
                    CheckboxRow(
                        label = "Role",
                        checked = settings.showRole,
                        onCheckedChange = { onSettingsChange(settings.copy(showRole = it)) },
                    )
                }

                HorizontalDivider()

                // Account settings
                if (viewModel != null) {
                    val uiState by viewModel.uiState.collectAsState()
                    val authenticatedUser = (uiState as? SignInUiState.Authenticated)?.user
                    if (authenticatedUser != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = authenticatedUser.displayName ?: authenticatedUser.email ?: "Signed in",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(
                                onClick = {
                                    viewModel.onSignOutClicked()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = "Close settings" },
            ) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$label setting" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Color.toHexString(): String {
    val r = (this.red * 255).toInt().coerceIn(0, 255)
    val g = (this.green * 255).toInt().coerceIn(0, 255)
    val b = (this.blue * 255).toInt().coerceIn(0, 255)
    return "#" +
            r.toString(16).padStart(2, '0') +
            g.toString(16).padStart(2, '0') +
            b.toString(16).padStart(2, '0')
}
