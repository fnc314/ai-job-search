package dev.fnc314.aijobtracker.frontend.features.signin.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.features_sign_in_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_sign_in_ui.generated.resources.sign_in_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destination route for authentication and sign in.
 */
@Serializable
public data object SignInRoute : JobApplicationsTrackerNavKey {
  @Transient
  override val titleRes: StringResource = Res.string.sign_in_route_title

  @Transient
  override val topBarActions: TopBarActions = TopBarActions.None
}

/**
 * Navigation entry registry providing routes for the sign-in feature.
 */
public class SignInEntryRegistry : FeatureEntryRegistry {
  override operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) {
    scope.entry<SignInRoute> {
      val viewModel = koinViewModel<SignInViewModel>()
      val navigator = LocalAppNavigator.current
      SignInScreen(
        viewModel = viewModel,
        onSignInSuccess = navigator::login,
      )
    }
  }
}
