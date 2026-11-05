package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public sealed interface SignInUiState {
    public data object Unauthenticated : SignInUiState
    public data object SigningIn : SignInUiState
    public data class Authenticated(public val user: UserAccount) : SignInUiState
    public data class Error(public val message: String) : SignInUiState
}

public class SignInViewModel(
    private val signInUseCase: SignInUseCase,
    private val signOutUseCase: SignOutUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _isSigningIn = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    public val uiState: StateFlow<SignInUiState> = combine(
        observeAuthStateUseCase(),
        _isSigningIn,
        _errorMessage
    ) { authState, isSigningIn, errorMessage ->
        when {
            isSigningIn -> SignInUiState.SigningIn
            errorMessage != null -> SignInUiState.Error(errorMessage)
            authState is AuthState.Authenticated -> SignInUiState.Authenticated(authState.user)
            authState is AuthState.Error -> SignInUiState.Error(authState.message)
            authState is AuthState.Loading -> SignInUiState.SigningIn
            else -> SignInUiState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SignInUiState.Unauthenticated
    )

    public fun onSignInClicked(): Unit {
        println("[SignInViewModel] onSignInClicked: Starting sign-in")
        viewModelScope.launch {
            _isSigningIn.value = true
            _errorMessage.value = null
            val result = signInUseCase()
            result.onSuccess { user ->
                println("[SignInViewModel] onSignInClicked: Success - ${user.displayName ?: user.email}")
            }
            result.onFailure { error ->
                println("[SignInViewModel] onSignInClicked: Failed - ${error.message}")
                _errorMessage.value = error.message ?: "Sign-in failed"
            }
            _isSigningIn.value = false
        }
    }

    public fun onSignInWithGoogleClicked(): Unit = onSignInClicked()

    public fun onSignOutClicked(): Unit {
        println("[SignInViewModel] onSignOutClicked: Signing out")
        viewModelScope.launch {
            signOutUseCase()
            println("[SignInViewModel] onSignOutClicked: Complete")
        }
    }

    public fun clearError(): Unit {
        _errorMessage.value = null
    }
}
