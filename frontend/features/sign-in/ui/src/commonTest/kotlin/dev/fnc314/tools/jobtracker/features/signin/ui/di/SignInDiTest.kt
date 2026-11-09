package dev.fnc314.tools.jobtracker.features.signin.ui.di

import dev.fnc314.tools.jobtracker.core.data.model.AuthState
import dev.fnc314.tools.jobtracker.core.data.model.UserAccount
import dev.fnc314.tools.jobtracker.features.signin.data.repository.AuthRepository
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class SignInDiTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private class FakeAuthRepository : AuthRepository {
        private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
        override val authState: StateFlow<AuthState> = _authState.asStateFlow()
        override val currentUser: UserAccount? = null
        override suspend fun signOut(): Result<Unit> = Result.success(Unit)
        override suspend fun signIn(): Result<UserAccount> = Result.success(UserAccount("uid", "email"))
        override suspend fun initAuthState() {}
    }

    @Test
    fun testSignInFeatureModule_resolvesSignInViewModelSuccessfully() {
        val testOverrideModule = module {
            single<AuthRepository> { FakeAuthRepository() }
        }

        val app = startKoin {
            modules(
                testOverrideModule,
                signInFeatureModule,
            )
        }

        val viewModel: SignInViewModel = app.koin.get()
        assertNotNull(viewModel)
    }
}
