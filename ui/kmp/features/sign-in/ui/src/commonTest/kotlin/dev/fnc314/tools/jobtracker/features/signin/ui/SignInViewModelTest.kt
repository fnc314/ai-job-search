package dev.fnc314.tools.jobtracker.features.signin.ui

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class SignInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository(
        initialUser: UserAccount? = null,
    ) : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(
            if (initialUser != null) AuthState.Authenticated(initialUser) else AuthState.Unauthenticated
        )
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()

        override val currentUser: UserAccount?
            get() = (_authState.value as? AuthState.Authenticated)?.user

        var signOutCalled: Boolean = false
        var signInResult: Result<UserAccount> = Result.success(
            UserAccount("test-uid", "test@example.com", "Test User", "https://example.com/avatar.png")
        )

        override suspend fun signOut(): Result<Unit> {
            signOutCalled = true
            _authState.value = AuthState.Unauthenticated
            return Result.success(Unit)
        }

        override suspend fun signIn(): Result<UserAccount> {
            return signInResult.also { result ->
                result.onSuccess { user ->
                    _authState.value = AuthState.Authenticated(user)
                }
            }
        }

        override suspend fun initAuthState(): Unit {}
    }

    private class FakeSignInUseCase(
        private val repository: FakeAuthRepository,
    ) : SignInUseCase {
        override suspend fun invoke(): Result<UserAccount> = repository.signIn()
    }

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: SignInViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeAuthRepository()
        viewModel = SignInViewModel(
            signInUseCase = FakeSignInUseCase(repository),
            signOutUseCase = SignOutUseCase(repository),
            observeAuthStateUseCase = ObserveAuthStateUseCase(repository),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsUnauthenticated() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        val state = viewModel.uiState.value
        assertEquals(SignInUiState.Unauthenticated, state)
    }

    @Test
    fun clickSignInIntentSuccessfullyAuthenticates() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.onIntent(SignInUiIntent.ClickSignIn)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<SignInUiState.Authenticated>(state)
        assertEquals("test@example.com", state.user.email)
        assertEquals("Test User", state.user.displayName)
        assertEquals("https://example.com/avatar.png", state.user.photoUrl)
    }

    @Test
    fun clickSignOutIntentInvokesSignOut() = runTest(testDispatcher) {
        val user = UserAccount("uid", "user@example.com", "User")
        val authenticatedRepo = FakeAuthRepository(initialUser = user)
        val vm = SignInViewModel(
            signInUseCase = FakeSignInUseCase(authenticatedRepo),
            signOutUseCase = SignOutUseCase(authenticatedRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(authenticatedRepo),
        )
        backgroundScope.launch { vm.uiState.collect() }

        vm.onIntent(SignInUiIntent.ClickSignOut)
        advanceUntilIdle()

        assertTrue(authenticatedRepo.signOutCalled)
        assertEquals(SignInUiState.Unauthenticated, vm.uiState.value)
    }

    @Test
    fun clickSignInIntentWithErrorProducesErrorStateAndClearErrorClearsIt() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }
        repository.signInResult = Result.failure(IllegalStateException("Auth network failure"))

        viewModel.onIntent(SignInUiIntent.ClickSignIn)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertIs<SignInUiState.Error>(errorState)
        assertEquals("Auth network failure", errorState.message)

        viewModel.onIntent(SignInUiIntent.ClearError)
        advanceUntilIdle()

        assertEquals(SignInUiState.Unauthenticated, viewModel.uiState.value)
    }

    @Test
    fun userAccountToUiModelMapsProperly() {
        val domainUser = UserAccount(
            uid = "123",
            email = "dev@example.com",
            displayName = "Dev Name",
            photoUrl = "photo.jpg",
        )
        val uiModel = domainUser.toUiModel()
        assertEquals("dev@example.com", uiModel.email)
        assertEquals("Dev Name", uiModel.displayName)
        assertEquals("photo.jpg", uiModel.photoUrl)

        val userWithoutEmail = UserAccount(uid = "456", email = null)
        val uiModelEmptyEmail = userWithoutEmail.toUiModel()
        assertEquals("", uiModelEmptyEmail.email)
    }
}
