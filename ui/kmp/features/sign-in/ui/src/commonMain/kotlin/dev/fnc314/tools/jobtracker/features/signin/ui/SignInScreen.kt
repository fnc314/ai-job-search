package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.Res
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_action_google
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_action_retry
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_app_title
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_failed
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_progress
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
public fun SignInScreen(
    viewModel: SignInViewModel,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val uiState: SignInUiState by viewModel.uiState.collectAsState()
    SignInScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onSignInSuccess = onSignInSuccess,
        modifier = modifier,
    )
}

@Composable
public fun SignInScreen(
    uiState: SignInUiState,
    onIntent: (SignInUiIntent) -> Unit,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier,
): Unit {
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SignInUiState.Authenticated -> {
                onSignInSuccess()
            }
            is SignInUiState.Error -> {
                // Show error in the UI directly; don't auto-clear so the user can see it
                snackbarHostState.showSnackbar(state.message)
            }
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Sign In",
                    modifier = Modifier.size(MaterialTheme.iconSizes.avatar),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = stringResource(Res.string.sign_in_app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Text(
                    text = stringResource(Res.string.sign_in_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                when (uiState) {
                    is SignInUiState.SigningIn -> {
                        CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.componentSizes.iconButtonSize))
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                        Text(
                            text = stringResource(Res.string.sign_in_progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is SignInUiState.Error -> {
                        Text(
                            text = stringResource(Res.string.sign_in_failed, uiState.message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.subMedium))
                        Button(
                            onClick = {
                                onIntent(SignInUiIntent.ClearError)
                                onIntent(SignInUiIntent.ClickSignIn)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.sign_in_action_retry))
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onIntent(SignInUiIntent.ClickSignIn) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.sign_in_action_google))
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
