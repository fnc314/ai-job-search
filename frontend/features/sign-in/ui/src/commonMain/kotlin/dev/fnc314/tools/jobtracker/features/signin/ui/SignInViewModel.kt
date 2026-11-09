package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Presentation ViewModel managing authentication flows and session monitoring.
 *
 * @param signInUseCase Use case triggering authentication.
 * @param signOutUseCase Use case clearing authenticated credentials.
 * @param observeAuthStateUseCase Use case observing realtime session states.
 */
public class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase,
) : ViewModel() {

    private val _isSigningIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    /**
     * Observable stream of [SignInUiState] reflecting current session credentials, in-progress signs, and errors.
     */
    public val uiState: StateFlow<SignInUiState> = combine(
        observeAuthStateUseCase(),
        _isSigningIn,
        _errorMessage,
    ) { authState, isSigningIn, errorMessage ->
        when {
            isSigningIn -> SignInUiState.SigningIn
            errorMessage != null -> SignInUiState.Error(errorMessage)
            authState is AuthState.Authenticated -> SignInUiState.Authenticated(authState.user.toUiModel())
            authState is AuthState.Error -> SignInUiState.Error(authState.message)
            authState is AuthState.Loading -> SignInUiState.SigningIn
            else -> SignInUiState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SignInUiState.Unauthenticated,
    )

    /**
     * Dispatches a user intent to be processed by this ViewModel.
     *
     * @param intent The [SignInUiIntent] to process.
     */
    public fun onIntent(intent: SignInUiIntent): Unit {
        when (intent) {
            is SignInUiIntent.ClickSignIn -> handleSignIn()
            is SignInUiIntent.ClickSignOut -> handleSignOut()
            is SignInUiIntent.ClearError -> clearError()
        }
    }

    private fun handleSignIn(): Unit {
        println("[SignInViewModel] handleSignIn: Starting sign-in")
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = signInUseCase()
            result.onSuccess { user ->
                println("[SignInViewModel] handleSignIn: Success - ${user.displayName ?: user.email}")
            }
            result.onFailure { error ->
                println("[SignInViewModel] handleSignIn: Failed - ${error.message}")
                _errorMessage.value = error.message ?: "Sign-in failed"
            }
            _isSigningIn.value = false
        }
    }

    private fun handleSignOut(): Unit {
        println("[SignInViewModel] handleSignOut: Signing out")
        viewModelScope.launch {
            signOutUseCase()
            println("[SignInViewModel] handleSignOut: Complete")
        }
    }

    private fun clearError(): Unit {
        _errorMessage.value = null
    }
}
