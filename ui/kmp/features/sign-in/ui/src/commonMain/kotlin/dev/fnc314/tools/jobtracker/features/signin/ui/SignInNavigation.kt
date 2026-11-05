package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.Res
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
public data object SignInRoute : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.sign_in_route_title
}

public val signInNavHook: FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<SignInRoute> {
        val viewModel = koinViewModel<SignInViewModel>()
        SignInScreen(
            viewModel = viewModel,
            onSignInSuccess = {},
        )
    }
}
