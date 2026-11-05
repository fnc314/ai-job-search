package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
public data object SignInRoute : NavKeyRoute

public val signInNavHook: FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<SignInRoute> {
        val viewModel = koinViewModel<SignInViewModel>()
        SignInScreen(
            viewModel = viewModel,
            onSignInSuccess = {},
        )
    }
}
