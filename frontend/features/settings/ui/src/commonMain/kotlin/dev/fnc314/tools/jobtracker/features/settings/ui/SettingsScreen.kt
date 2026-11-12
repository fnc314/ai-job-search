package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.data.settings.UiMode
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.designsystem.toColor
import dev.fnc314.tools.jobtracker.core.designsystem.toColorOrNull
import dev.fnc314.tools.jobtracker.core.designsystem.toHexString
import dev.fnc314.tools.jobtracker.core.designsystem.toRgbaChannels
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import fnc314jobsearchtracker.features_settings_ui.generated.resources.Res
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_action_sign_out
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_cd_custom_seed_hex
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_cd_theme_color
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_disabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_enabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_label
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_in_app_text_viewer_subtitle
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_disabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_enabled
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_kmp_inspector_toggle_label
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
                        shape = MaterialTheme.shapes.medium,
                        onClick = { onIntent(SettingsUiIntent.ClickSignOut) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.settings_action_sign_out))
                    }
                }
            }

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
                            .fillMaxWidth()
                            .toggleable(
                                value = model.kmpInspectorEnabled,
                                onValueChange = { isChecked: Boolean ->
                                  onIntent(SettingsUiIntent.ToggleKmpInspector(enabled = isChecked))
                                },
                                role = Role.Checkbox
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            modifier = Modifier,
                            checked = model.kmpInspectorEnabled,
                            onCheckedChange = null,
                        )
                        Text(
                          text = if (model.kmpInspectorEnabled) {
                              stringResource(Res.string.settings_kmp_inspector_toggle_enabled)
                          } else {
                              stringResource(Res.string.settings_kmp_inspector_toggle_disabled)
                          },
                          style = MaterialTheme.typography.labelLarge,
                          modifier = Modifier.fillMaxWidth()
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
                                .fillMaxWidth()
                                .toggleable(
                                    value = model.inAppTextViewerEnabled,
                                    onValueChange = { isChecked: Boolean ->
                                        onIntent(SettingsUiIntent.ToggleInAppTextViewer(enabled = isChecked))
                                    },
                                    role = Role.Checkbox
                                ),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                              modifier = Modifier,
                              checked = model.inAppTextViewerEnabled,
                              onCheckedChange = null,
                            )
                            Text(
                              text = if (model.inAppTextViewerEnabled) {
                                  stringResource(Res.string.settings_in_app_text_viewer_enabled)
                              } else {
                                  stringResource(Res.string.settings_in_app_text_viewer_disabled)
                              },
                              style = MaterialTheme.typography.labelLarge,
                              modifier = Modifier.fillMaxWidth()
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
 * Uses dropdown-based inputs: filterable 0-255 dropdowns for decimal mode (horizontal layout),
 * paired 0-F digit dropdowns for hex mode (vertical layout).
 *
 * @param currentHex The current seed color hex (`#RRGGBB`).
 * @param onConfirm Called when the user submits a valid color. Receives the hex string.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // Canonical integer values (0-255) — always valid
    var r by remember { mutableStateOf(initialChannels[0]) }
    var g by remember { mutableStateOf(initialChannels[1]) }
    var b by remember { mutableStateOf(initialChannels[2]) }

    // Hex digit state (0-15 each) for hex mode dropdowns
    var rHigh by remember { mutableStateOf(r.shr(4) and 0xF) }
    var rLow by remember { mutableStateOf(r and 0xF) }
    var gHigh by remember { mutableStateOf(g.shr(4) and 0xF) }
    var gLow by remember { mutableStateOf(g and 0xF) }
    var bHigh by remember { mutableStateOf(b.shr(4) and 0xF) }
    var bLow by remember { mutableStateOf(b and 0xF) }

    // Decimal vs Hex mode toggle
    var isHexMode by remember { mutableStateOf(false) }

    // Sync hex digits when mode changes
    fun syncHexDigitsFromInt() {
        rHigh = r.shr(4) and 0xF
        rLow = r and 0xF
        gHigh = g.shr(4) and 0xF
        gLow = g and 0xF
        bHigh = b.shr(4) and 0xF
        bLow = b and 0xF
    }

    // Sync int values when mode changes (from hex digits)
    fun syncIntFromHexDigits() {
        r = (rHigh * 16) + rLow
        g = (gHigh * 16) + gLow
        b = (bHigh * 16) + bLow
    }

    // When mode changes, sync the display values
    val wasHexMode = remember { mutableStateOf(isHexMode) }
    if (isHexMode != wasHexMode.value) {
        wasHexMode.value = isHexMode
        if (isHexMode) {
            syncHexDigitsFromInt()
        } else {
            syncIntFromHexDigits()
        }
    }

    // Preview color is always derived from canonical int values
    val previewColor = intArrayOf(r, g, b).toColor()

    fun buildHexString(): String {
        return "#" + r.toString(16).padStart(2, '0').uppercase() +
                g.toString(16).padStart(2, '0').uppercase() +
                b.toString(16).padStart(2, '0').uppercase()
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

                // Channel inputs: horizontal dropdowns for decimal, vertical hex rows for hex
                if (isHexMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        HexChannelRow(
                            label = "R",
                            highDigit = rHigh,
                            lowDigit = rLow,
                            onHighDigit = { digit ->
                                rHigh = digit
                                r = (rHigh * 16) + rLow
                            },
                            onLowDigit = { digit ->
                                rLow = digit
                                r = (rHigh * 16) + rLow
                            },
                        )
                        HexChannelRow(
                            label = "G",
                            highDigit = gHigh,
                            lowDigit = gLow,
                            onHighDigit = { digit ->
                                gHigh = digit
                                g = (gHigh * 16) + gLow
                            },
                            onLowDigit = { digit ->
                                gLow = digit
                                g = (gHigh * 16) + gLow
                            },
                        )
                        HexChannelRow(
                            label = "B",
                            highDigit = bHigh,
                            lowDigit = bLow,
                            onHighDigit = { digit ->
                                bHigh = digit
                                b = (bHigh * 16) + bLow
                            },
                            onLowDigit = { digit ->
                                bLow = digit
                                b = (bHigh * 16) + bLow
                            },
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        DecimalChannelDropdown(
                            label = "Red",
                            selected = r,
                            onSelected = { r = it },
                        )
                        DecimalChannelDropdown(
                            label = "Green",
                            selected = g,
                            onSelected = { g = it },
                        )
                        DecimalChannelDropdown(
                            label = "Blue",
                            selected = b,
                            onSelected = { b = it },
                            isLastInput = true,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(buildHexString()) },
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

/**
 * Filterable dropdown for selecting a decimal channel value (0-255).
 *
 * Uses [ExposedDropdownMenuBox] with an editable anchor so the user can type to filter
 * the list of 256 values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecimalChannelDropdown(
    label: String,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isLastInput: Boolean = false,
): Unit {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember(selected) { mutableStateOf(selected.toString()) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = filterText,
            onValueChange = { newText ->
                filterText = newText
                // If the input is a valid number in range, select it immediately
                newText.toIntOrNull()?.let { value ->
                    if (value in 0..255) {
                        onSelected(value)
                    }
                }
            },
            label = { Text(label) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
            trailingIcon = {
              ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Number,
              imeAction = if (isLastInput) ImeAction.Done else ImeAction.Next
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // Filter items based on user input to avoid rendering all 256 items
            val filtered = (0..255).filter { value ->
                value.toString().contains(filterText, ignoreCase = true)
            }
            filtered.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.toString()) },
                    onClick = {
                        onSelected(value)
                        filterText = value.toString()
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Compact dropdown for selecting a single hex digit (0-F).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HexDigitDropdown(
    selected: Int, // 0-15
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val hexChars = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = hexChars[selected].toString(),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            hexChars.forEachIndexed { index, char ->
                DropdownMenuItem(
                    text = { Text(char.toString()) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Two hex digit dropdowns side-by-side forming a single channel value.
 */
@Composable
private fun HexChannelRow(
    label: String,
    highDigit: Int, // 0-15
    lowDigit: Int, // 0-15
    onHighDigit: (Int) -> Unit,
    onLowDigit: (Int) -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(24.dp),
        )
        HexDigitDropdown(
            selected = highDigit,
            onSelected = onHighDigit,
            modifier = Modifier.weight(1f),
        )
        HexDigitDropdown(
            selected = lowDigit,
            onSelected = onLowDigit,
            modifier = Modifier.weight(1f),
        )
    }
}
