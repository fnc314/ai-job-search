package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInUiState
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    signInViewModel: SignInViewModel? = null,
) {
    val settings by viewModel.settings.collectAsState()
    SettingsScreen(
        settings = settings,
        onSettingsChange = viewModel::updateSettings,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
        signInViewModel = signInViewModel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    signInViewModel: SignInViewModel? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaterialTheme.componentSizes.formMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                // Section 1: Appearance & Theme
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = "UI Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
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
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

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
                                        .size(MaterialTheme.componentSizes.iconButtonSize)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable {
                                            onSettingsChange(settings.copy(seedColorHex = hexString))
                                        }
                                        .semantics { contentDescription = "$name theme color" }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = MaterialTheme.spacing.compact,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = CircleShape,
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }

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
                }

                // Section 2: Column Visibility
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    ) {
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
                }

                // Section 3: Artifacts & Documents
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                    ) {
                        Text(
                            text = "Artifacts & Documents",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        CheckboxRow(
                            label = "Render Markdown internally in app",
                            checked = settings.renderMarkdownInternally,
                            onCheckedChange = { onSettingsChange(settings.copy(renderMarkdownInternally = it)) },
                        )
                    }
                }

                // Section 4: Account & Session
                if (signInViewModel != null) {
                    val uiState by signInViewModel.uiState.collectAsState()
                    val authenticatedUser = (uiState as? SignInUiState.Authenticated)?.user
                    if (authenticatedUser != null) {
                        OutlinedCard(
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.medium),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            ) {
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
                                        signInViewModel.onSignOutClicked()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Sign Out")
                                }
                            }
                        }
                    }
                }

                // Spacing at bottom
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
        }
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
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
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
