package dev.fnc314.tools.jobtracker.app.core.impl

import dev.fnc314.tools.jobtracker.core.navigation.AppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.ApplicationId
import dev.fnc314.tools.jobtracker.core.navigation.CompanyId
import dev.fnc314.tools.jobtracker.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.tools.jobtracker.core.navigation.Navigator
import dev.fnc314.tools.jobtracker.core.navigation.TopLevelDestinationEnum
import dev.fnc314.tools.jobtracker.features.companydetails.ui.JobApplicationCompanyDetailsRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.JobApplicationDetailRoute
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui.JobApplicationListRoute
import dev.fnc314.tools.jobtracker.features.settings.ui.JobApplicationSettingsRoute
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInRoute

/**
 * Internal implementation of [AppNavigator] that delegates to [Navigator] for back stack operations.
 *
 * Must remain [internal] and reside in the `.impl` package to satisfy Konsist architecture tests.
 *
 * @param navigator Wrapper around the Navigation 3 back stack.
 * @param routeResolver Maps [TopLevelDestinationEnum] to actual [JobApplicationsTrackerNavKey] instances.
 */
internal class AppNavigatorImpl(
    private val navigator: Navigator,
    private val routeResolver: (TopLevelDestinationEnum) -> JobApplicationsTrackerNavKey,
) : AppNavigator {
    override fun navigateToApplicationDetails(applicationId: ApplicationId) {
        navigator.navigate(JobApplicationDetailRoute(applicationId.value))
    }

    override fun navigateToCompanyDetails(companyId: CompanyId) {
        navigator.navigate(JobApplicationCompanyDetailsRoute(companyId.value))
    }

    override fun navigateTo(topLevelDestination: TopLevelDestinationEnum) {
        val route = routeResolver(topLevelDestination)
        navigator.navigate(route)
    }

    override fun navigateToSettings() {
        navigator.navigate(JobApplicationSettingsRoute)
    }

    override fun navigate(key: JobApplicationsTrackerNavKey) {
        navigator.navigate(key)
    }

    override fun login() {
        // Idempotent: skip switchRoot if the back stack root is already JobApplicationListRoute
        // to avoid clearing a Navigation3-restored back stack after config changes.
        val currentRoot = navigator.backStack.firstOrNull()
        if (currentRoot !is JobApplicationListRoute) {
            navigator.switchRoot(JobApplicationListRoute)
        }
    }

    override fun logout() {
        navigator.switchRoot(SignInRoute)
    }

    override fun popBackStack() {
        navigator.popBackStack()
    }
}
