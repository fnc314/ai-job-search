package dev.fnc314.tools.jobtracker.features.signin.domain

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.GetCurrentUserUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.ObserveAuthStateUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.SignInUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.SignOutUseCaseImpl
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

        override suspend fun signIn(): Result<UserAccount> {
            val user = UserAccount("test-uid", "test@example.com", "Test User", null)
            _authState.value = AuthState.Authenticated(user)
            return Result.success(user)
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
        val useCase: ObserveAuthStateUseCase = ObserveAuthStateUseCaseImpl(repo)
        assertEquals(AuthState.Unauthenticated, useCase().value)
    }

    @Test
    fun testGetCurrentUser_ReturnsNullWhenUnauthenticated() {
        val repo = FakeAuthRepository()
        val useCase: GetCurrentUserUseCase = GetCurrentUserUseCaseImpl(repo)
        assertNull(useCase())
    }

    @Test
    fun testGetCurrentUser_ReturnsUserWhenAuthenticated() {
        val user = UserAccount(uid = "user-123", email = "test@example.com", displayName = "Test Candidate")
        val repo = FakeAuthRepository(initialUser = user)
        val useCase: GetCurrentUserUseCase = GetCurrentUserUseCaseImpl(repo)

        val current = useCase()
        assertEquals("user-123", current?.uid)
        assertEquals("test@example.com", current?.email)
        assertEquals("Test Candidate", current?.displayName)
    }

    @Test
    fun testSignOut_TransitionsFromAuthenticatedToUnauthenticated() = runTest {
        val user = UserAccount(uid = "user-456", email = "logout@example.com")
        val repo = FakeAuthRepository(initialUser = user)
        val observeUseCase: ObserveAuthStateUseCase = ObserveAuthStateUseCaseImpl(repo)
        val signOutUseCase: SignOutUseCase = SignOutUseCaseImpl(repo)

        assertTrue(observeUseCase().value is AuthState.Authenticated)

        val result = signOutUseCase()
        assertTrue(result.isSuccess)
        assertTrue(repo.signOutCalled)
        assertEquals(AuthState.Unauthenticated, observeUseCase().value)
    }

    @Test
    fun testSignOut_IdempotentWhenAlreadyUnauthenticated() = runTest {
        val repo = FakeAuthRepository()
        val signOutUseCase: SignOutUseCase = SignOutUseCaseImpl(repo)
        val observeUseCase: ObserveAuthStateUseCase = ObserveAuthStateUseCaseImpl(repo)

        val result = signOutUseCase()
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, observeUseCase().value)
    }

    @Test
    fun testSignInUseCase_Success() = runTest {
        val user = UserAccount(uid = "user-789", email = "signin@example.com")
        val repo = FakeAuthRepository(initialUser = user)
        val signInUseCase: SignInUseCase = SignInUseCaseImpl(repo)
        val result = signInUseCase()
        assertTrue(result.isSuccess)
        assertEquals("test-uid", result.getOrNull()?.uid)
        assertEquals("test@example.com", result.getOrNull()?.email)
    }
}
