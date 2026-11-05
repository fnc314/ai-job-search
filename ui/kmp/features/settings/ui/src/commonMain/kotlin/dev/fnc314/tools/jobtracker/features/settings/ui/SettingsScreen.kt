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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.SeedColors
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.designsystem.toHexString
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInUiIntent
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInUiState
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
public fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    signInViewModel: SignInViewModel? = null,
): Unit {
    val uiState: SettingsUiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.NavigateBack) {
            onNavigateBack()
        }
    }

    SettingsScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        signInViewModel = signInViewModel,
    )
}

@Composable
public fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    signInViewModel: SignInViewModel? = null,
): Unit {
    when (uiState) {
        is SettingsUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is SettingsUiState.NavigateBack -> {
            Box(modifier = modifier.fillMaxSize())
        }
        is SettingsUiState.Content -> {
            SettingsContent(
                model = uiState.model,
                onIntent = onIntent,
                modifier = modifier,
                signInViewModel = signInViewModel,
            )
        }
    }
}

@Composable
internal fun SettingsContent(
    model: SettingsUiModel,
    onIntent: (SettingsUiIntent) -> Unit,
    modifier: Modifier = Modifier,
    signInViewModel: SignInViewModel? = null,
): Unit {
    val keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current

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
            // Header with Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                IconButton(
                    onClick = { onIntent(SettingsUiIntent.ClickBack) },
                    modifier = Modifier.semantics { contentDescription = "Navigate back" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

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
                        text = "Theme: ${model.themeModeName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    ) {
                        FilterChip(
                            selected = !model.isSystemDark,
                            onClick = { onIntent(SettingsUiIntent.ChangeTheme(isDark = false)) },
                            label = { Text("Light") },
                            modifier = Modifier.weight(1f),
                        )
                        FilterChip(
                            selected = model.isSystemDark,
                            onClick = { onIntent(SettingsUiIntent.ChangeTheme(isDark = true)) },
                            label = { Text("Dark") },
                            modifier = Modifier.weight(1f),
                        )
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
                            val hexString: String = color.toHexString()
                            val isSelected: Boolean = model.seedColorHex.equals(hexString, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(MaterialTheme.componentSizes.iconButtonSize)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable {
                                        onIntent(SettingsUiIntent.ChangeSeedColor(hex = hexString))
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
                                    ),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = model.seedColorHex,
                        onValueChange = { newHex: String ->
                            keyboardController?.hide()
                            onIntent(SettingsUiIntent.ChangeSeedColor(hex = newHex))
                        },
                        label = { Text("Custom Seed Hex") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Custom theme seed color hex value" },
                        singleLine = true,
                    )
                }
            }

            // Section 2: Account & Session
            if (signInViewModel != null) {
                val signInState: SignInUiState by signInViewModel.uiState.collectAsState()
                val authenticatedUser = (signInState as? SignInUiState.Authenticated)?.user
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
                                text = authenticatedUser.displayName ?: authenticatedUser.email.ifEmpty { "Signed in" },
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            OutlinedButton(
                                onClick = {
                                    signInViewModel.onIntent(SignInUiIntent.ClickSignOut)
                                    onIntent(SettingsUiIntent.ClickBack)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}
