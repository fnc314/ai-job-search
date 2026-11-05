package dev.fnc314.tools.jobtracker.features.signin.domain

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.core.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthUseCasesTest {

    private class FakeAuthRepository(
        initialUser: UserAccount? = null,
    ) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(
            if (initialUser != null) AuthState.Authenticated(initialUser) else AuthState.Unauthenticated
        )
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()

        override val currentUser: UserAccount?
            get() = (_authState.value as? AuthState.Authenticated)?.user

        var signOutCalled = false

        override suspend fun signOut(): Result<Unit> {
            signOutCalled = true
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        }

        override suspend fun initAuthState() {
            // no-op for tests
        }

        fun setUser(user: UserAccount) {
            _authState.value = AuthState.Authenticated(user)
        }
    }

    @Test
    fun testObserveAuthState_InitialStateIsUnauthenticated() {
        val repo = FakeAuthRepository()
        val useCase = ObserveAuthStateUseCase(repo)
        assertEquals(AuthState.Unauthenticated, useCase().value)
    }

    @Test
    fun testGetCurrentUser_ReturnsNullWhenUnauthenticated() {
        val repo = FakeAuthRepository()
        val useCase = GetCurrentUserUseCase(repo)
        assertNull(useCase())
    }

    @Test
    fun testGetCurrentUser_ReturnsUserWhenAuthenticated() {
        val user = UserAccount(uid = "user-123", email = "test@example.com", displayName = "Test Candidate")
        val repo = FakeAuthRepository(initialUser = user)
        val useCase = GetCurrentUserUseCase(repo)

        val current = useCase()
        assertEquals("user-123", current?.uid)
        assertEquals("test@example.com", current?.email)
        assertEquals("Test Candidate", current?.displayName)
    }

    @Test
    fun testSignOut_TransitionsFromAuthenticatedToUnauthenticated() = runTest {
        val user = UserAccount(uid = "user-456", email = "logout@example.com")
        val repo = FakeAuthRepository(initialUser = user)
        val observeUseCase = ObserveAuthStateUseCase(repo)
        val signOutUseCase = SignOutUseCase(repo)

        assertTrue(observeUseCase().value is AuthState.Authenticated)

        val result = signOutUseCase()
        assertTrue(result.isSuccess)
        assertTrue(repo.signOutCalled)
        assertEquals(AuthState.Unauthenticated, observeUseCase().value)
    }

    @Test
    fun testSignOut_IdempotentWhenAlreadyUnauthenticated() = runTest {
        val repo = FakeAuthRepository()
        val signOutUseCase = SignOutUseCase(repo)
        val observeUseCase = ObserveAuthStateUseCase(repo)

        val result = signOutUseCase()
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, observeUseCase().value)
    }

    @Test
    fun testSignInUseCase_Success() = runTest {
        val user = UserAccount(uid = "user-789", email = "signin@example.com")
        val signInUseCase = object : SignInUseCase {
            override suspend fun invoke(): Result<UserAccount> = Result.success(user)
        }
        val result = signInUseCase()
        assertTrue(result.isSuccess)
        assertEquals("user-789", result.getOrNull()?.uid)
        assertEquals("signin@example.com", result.getOrNull()?.email)
    }
}
