package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.designsystem.toHexString
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.core.designsystem.toRgbaChannels
import dev.fnc314.tools.jobtracker.core.designsystem.toColor
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import fnc314jobsearchtracker.features_settings_ui.generated.resources.Res
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_disabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_enabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_label
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_subtitle
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_label
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_enabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_disabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_action_sign_out
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_cd_custom_seed_hex
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_cd_theme_color
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_label_accent_seed
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_label_custom_seed_hex
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_label_theme
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_section_account
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_section_appearance
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_theme_dark
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_theme_light
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_theme_system
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Top-level Stateful composable hosting the application settings screen.
 *
 * @param onNavigateBack Callback invoked when exiting settings.
 * @param onSignOut Callback invoked upon user sign out.
 * @param modifier Composable layout modifier.
 * @param viewModel [SettingsViewModel] driving presentation state.
 */
@Composable
public fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
): Unit {
    val uiState: SettingsUiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is SettingsUiState.NavigateBack -> onNavigateBack()
            is SettingsUiState.NavigateToSignIn -> onSignOut()
            else -> {}
        }
    }

    SettingsScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/**
 * Stateless composable displaying settings configuration options.
 *
 * @param uiState Current [SettingsUiState] to render.
 * @param onIntent Event handler for user interaction intents.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    when (uiState) {
        is SettingsUiState.Loading -> {
            LoadingWidget(modifier = modifier)
        }

        is SettingsUiState.NavigateBack,
        is SettingsUiState.NavigateToSignIn -> {
            Box(modifier = modifier.fillMaxSize())
        }

        is SettingsUiState.Content -> {
            SettingsContent(
                model = uiState.model,
                onIntent = onIntent,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun SettingsContent(
    model: SettingsUiModel,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    var showColorPickerDialog by remember { mutableStateOf(false) }

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
                        text = stringResource(Res.string.settings_section_appearance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text = stringResource(Res.string.settings_label_theme, model.uiMode.name),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        FilterChip(
                            selected = model.uiMode == UiMode.SYSTEM,
                            onClick = { onIntent(SettingsUiIntent.ChangeUiMode(UiMode.SYSTEM)) },
                            label = { Text(stringResource(Res.string.settings_theme_system)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = model.uiMode == UiMode.LIGHT,
                            onClick = { onIntent(SettingsUiIntent.ChangeUiMode(UiMode.LIGHT)) },
                            label = { Text(stringResource(Res.string.settings_theme_light)) },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = model.uiMode == UiMode.DARK,
                            onClick = { onIntent(SettingsUiIntent.ChangeUiMode(UiMode.DARK)) },
                            label = { Text(stringResource(Res.string.settings_theme_dark)) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

                    Text(
                        text = stringResource(Res.string.settings_label_accent_seed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SeedColors.All.forEach { (name, color) ->
                            val hexString: String = color.toHexString()
                            val isSelected: Boolean = model.seedColorHex.equals(hexString, ignoreCase = true)
                            val colorCd: String = stringResource(Res.string.settings_cd_theme_color, name)
                            Box(
                                modifier = Modifier
                                    .size(MaterialTheme.componentSizes.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        onIntent(SettingsUiIntent.ChangeSeedColor(hex = hexString))
                                    }
                                    .semantics { contentDescription = colorCd }
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
                                    ),
                            )
                        }
                    }

                    val customSeedCd: String = stringResource(Res.string.settings_cd_custom_seed_hex)
                    val currentPreviewColor = model.seedColorHex.toColorOrNull()

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    OutlinedCard(
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColorPickerDialog = true }
                            .semantics { contentDescription = customSeedCd },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(MaterialTheme.componentSizes.iconButtonSize)
                                    .clip(CircleShape)
                                    .then(
                                        if (currentPreviewColor != null) {
                                            Modifier.background(currentPreviewColor)
                                        } else {
                                            Modifier.background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            )
                                        }
                                    ),
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                            ) {
                                Text(
                                    text = stringResource(Res.string.settings_label_custom_seed_hex),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = model.seedColorHex.toColorOrNull()?.toHexString() ?: model.seedColorHex,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    if (showColorPickerDialog) {
                        ColorPickerDialog(
                            currentHex = model.seedColorHex,
                            onConfirm = { hex: String ->
                                showColorPickerDialog = false
                                onIntent(SettingsUiIntent.ChangeSeedColor(hex = hex))
                            },
                            onDismiss = { showColorPickerDialog = false },
                        )
                    }
                }
            }

            // Section 2: Account & Session
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
                        text = stringResource(Res.string.settings_section_account),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    OutlinedButton(
                        onClick = { onIntent(SettingsUiIntent.ClickSignOut) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.settings_action_sign_out))
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            OutlinedCard(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    Text(
                        text = stringResource(Res.string.settings_kmp_inspector_toggle_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            modifier = Modifier
                                .fillMaxWidth(0.25F),
                            checked = model.kmpInspectorEnabled,
                            onCheckedChange = { isChecked: Boolean ->
                                onIntent(SettingsUiIntent.ToggleKmpInspector(enabled = isChecked))
                            }
                        )
                        Text(
                            text = if (model.kmpInspectorEnabled) {
                                stringResource(Res.string.settings_kmp_inspector_toggle_enabled)
                            } else {
                                stringResource(Res.string.settings_kmp_inspector_toggle_disabled)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.fillMaxWidth(0.75F)
                        )
                    }

                }
            }

            // Section: In-App Document Viewer (Mobile only)
            if (model.showInAppTextViewerSetting) {
                OutlinedCard(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_in_app_text_viewer_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(Res.string.settings_in_app_text_viewer_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                modifier = Modifier
                                    .fillMaxWidth(0.25F),
                                checked = model.inAppTextViewerEnabled,
                                onCheckedChange = { isChecked: Boolean ->
                                    onIntent(SettingsUiIntent.ToggleInAppTextViewer(enabled = isChecked))
                                }
                            )
                            Text(
                                text = if (model.inAppTextViewerEnabled) {
                                    stringResource(Res.string.settings_in_app_text_viewer_enabled)
                                } else {
                                    stringResource(Res.string.settings_in_app_text_viewer_disabled)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.fillMaxWidth(0.75F)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Color picker dialog allowing the user to adjust R, G, and B channels independently.
 *
 * Inspired by Android's TimePickerDialog — large preview, channel inputs, and Submit/Cancel.
 *
 * @param currentHex The current seed color hex (`#RRGGBB`).
 * @param onConfirm Called when the user submits a valid color. Receives the hex string.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ColorPickerDialog(
    currentHex: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    // Parse initial RGB values from the current hex
    val initialColor = currentHex.toColorOrNull() ?: SeedColors.ProfessionalBlue
    val initialChannels = initialColor.toRgbaChannels()

    var r by remember { mutableStateOf(initialChannels[0]) }
    var g by remember { mutableStateOf(initialChannels[1]) }
    var b by remember { mutableStateOf(initialChannels[2]) }

    // Text input state for each channel
    var rText by remember { mutableStateOf(initialChannels[0].toString()) }
    var gText by remember { mutableStateOf(initialChannels[1].toString()) }
    var bText by remember { mutableStateOf(initialChannels[2].toString()) }

    // Decimal vs Hex mode toggle
    var isHexMode by remember { mutableStateOf(false) }

    // Validate each channel independently
    val decimalRegex = Regex("^\\d+$")
    val hexRegex = Regex("^[0-9a-fA-F]{1,2}$")

    fun isValidChannel(text: String): Boolean {
        return if (isHexMode) {
            text.matches(hexRegex)
        } else {
            text.matches(decimalRegex) && text.toInt() in 0..255
        }
    }

    val rValid = isValidChannel(rText)
    val gValid = isValidChannel(gText)
    val bValid = isValidChannel(bText)
    val allValid = rValid && gValid && bValid

    // Parse channel value to 0-255 int, defaulting to current value if invalid
    fun parseChannel(text: String, default: Int): Int {
        return try {
            if (isHexMode) {
                text.toInt(16).coerceIn(0, 255)
            } else {
                text.toInt().coerceIn(0, 255)
            }
        } catch (_: Exception) {
            default
        }
    }

    val previewR = parseChannel(rText, r)
    val previewG = parseChannel(gText, g)
    val previewB = parseChannel(bText, b)
    val previewColor = intArrayOf(previewR, previewG, previewB).toColor()

    // Update numeric state when text becomes valid
    if (rValid) r = parseChannel(rText, r)
    if (gValid) g = parseChannel(gText, g)
    if (bValid) b = parseChannel(bText, b)

    // Sync text when toggling mode
    fun intToDisplay(value: Int): String {
        return if (isHexMode) {
            value.toString(16).padStart(2, '0').uppercase()
        } else {
            value.toString()
        }
    }

    // When mode changes, sync all text fields
    val isHexModeKey = isHexMode
    LaunchedEffect(isHexModeKey) {
        rText = intToDisplay(r)
        gText = intToDisplay(g)
        bText = intToDisplay(b)
    }

    fun buildHexString(): String {
        val finalR = parseChannel(rText, r)
        val finalG = parseChannel(gText, g)
        val finalB = parseChannel(bText, b)
        return "#" + finalR.toString(16).padStart(2, '0').uppercase() +
                finalG.toString(16).padStart(2, '0').uppercase() +
                finalB.toString(16).padStart(2, '0').uppercase()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Theme Color") },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                // Large color preview region (≥50% of dialog)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Color preview overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .background(previewColor),
                    )
                }

                // Decimal/Hex mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isHexMode) "Hex (00–FF)" else "Decimal (0–255)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Switch(
                        checked = isHexMode,
                        onCheckedChange = { isHexMode = it },
                    )
                }

                // 2x2 Grid of channel inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    ChannelInputField(
                        label = "Red",
                        value = rText,
                        onValueChange = { rText = it },
                        isValid = rValid,
                        isHexMode = isHexMode,
                        modifier = Modifier.weight(1f),
                    )
                    ChannelInputField(
                        label = "Green",
                        value = gText,
                        onValueChange = { gText = it },
                        isValid = gValid,
                        isHexMode = isHexMode,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    ChannelInputField(
                        label = "Blue",
                        value = bText,
                        onValueChange = { bText = it },
                        isValid = bValid,
                        isHexMode = isHexMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(buildHexString()) },
                enabled = allValid,
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ChannelInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isValid: Boolean,
    isHexMode: Boolean,
    modifier: Modifier = Modifier,
): Unit {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = if (isHexMode) {
            KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters,
            )
        } else {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        },
        isError = value.isNotEmpty() && !isValid,
        supportingText = if (value.isNotEmpty() && !isValid) {
            @Composable { Text("Enter 0–255 or 00–FF") }
        } else {
            null
        },
        modifier = modifier,
        singleLine = true,
    )
}
