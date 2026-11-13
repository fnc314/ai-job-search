package dev.fnc314.tools.jobtracker.features.signin.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.core.navigation.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.tools.jobtracker.core.navigation.TopBarActions
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.Res
import fnc314jobsearchtracker.features_sign_in_ui.generated.resources.sign_in_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destination route for the authentication screen.
 */
@Serializable
public data object SignInRoute : JobApplicationsTrackerNavKey {
  @Transient
  override val titleRes: StringResource = Res.string.sign_in_route_title

  @Transient
  override val topBarActions: TopBarActions = TopBarActions.None
}

public class SignInEntryRegistry : FeatureEntryRegistry {
  override fun register(scope: EntryProviderScope<PlatformNavKey>) {
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
