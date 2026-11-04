package dev.fnc314.tools.jobtracker.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInUiState
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import dev.fnc314.tools.jobtracker.domain.usecase.SortJobApplicationsUseCase

data class AppSettings(
    val showDate: Boolean = true,
    val showCompany: Boolean = true,
    val showStatus: Boolean = true,
    val showFit: Boolean = true,
    val showRole: Boolean = true,
    val darkTheme: Boolean = false,
    val dynamicColor: Boolean = true,
    val seedColorHex: String = "#6750A4",
)

@Composable
fun SettingsDialog(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
    viewModel: SignInViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Column visibility
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Column Visibility",
                        style = MaterialTheme.typography.titleSmall,
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = authenticatedUser.displayName ?: authenticatedUser.email ?: "Signed in",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(
                                onClick = {
                                    viewModel.onSignOutClicked()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign Out")
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // Theme settings
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    CheckboxRow(
                        label = "Dark Mode",
                        checked = settings.darkTheme,
                        onCheckedChange = { onSettingsChange(settings.copy(darkTheme = it)) },
                    )
                    CheckboxRow(
                        label = "Dynamic Color (Material You)",
                        checked = settings.dynamicColor,
                        onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) },
                    )
                    if (!settings.dynamicColor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Seed Color",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = settings.seedColorHex,
                                onValueChange = {
                                    keyboardController?.hide()
                                    onSettingsChange(settings.copy(seedColorHex = it))
                                },
                                label = { Text("Hex") },
                                modifier = Modifier
                                    .semantics { contentDescription = "Theme seed color hex value" }
                                    .width(120.dp),
                                singleLine = true,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
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
