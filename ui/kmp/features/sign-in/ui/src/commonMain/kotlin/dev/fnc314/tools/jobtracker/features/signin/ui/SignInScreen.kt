package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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

@Composable
public fun SignInScreen(
    viewModel: SignInViewModel,
    onSignInSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
        contentAlignment = Alignment.Center
    ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Sign In",
                        modifier = Modifier.size(MaterialTheme.iconSizes.avatar),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    Text(
                        text = "Job Search Tracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    Text(
                        text = "Sign in to access your applications, companies, and timeline directly from Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

                    when (uiState) {
                        is SignInUiState.SigningIn -> {
                            CircularProgressIndicator(modifier = Modifier.size(MaterialTheme.componentSizes.iconButtonSize))
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = "Signing in...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is SignInUiState.Error -> {
                            Text(
                                text = "Sign-in failed: ${(uiState as SignInUiState.Error).message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.subMedium))
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    viewModel.onSignInWithGoogleClicked()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Try Again")
                            }
                        }
                        else -> {
                            Button(
                                onClick = { viewModel.onSignInWithGoogleClicked() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign In with Google")
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
