package dev.fnc314.tools.jobtracker.core.navigation

/**
 * High-level navigation interface for the application.
 *
 * Feature screens inject this interface to perform navigation without knowing
 * the underlying back stack implementation. All navigation methods are
 * side-effect only and return [Unit].
 */
public interface AppNavigator {
    /** Navigate to a job application detail screen. */
    fun navigateToApplicationDetails(applicationId: ApplicationId)

    /** Navigate to a company detail screen. */
    fun navigateToCompanyDetails(companyId: CompanyId)

    /** Navigate to a top-level destination (bottom nav targets). */
    fun navigateTo(topLevelDestination: TopLevelDestinationEnum)

    /** Navigate to the settings screen. */
    fun navigateToSettings()

    /** Navigate to an arbitrary [NavKey] destination. */
    fun navigate(key: NavKey)

    /** Log in — switch root to the main application screen, clearing the back stack. */
    fun login()

    /** Log out — switch root to the sign-in screen, clearing the back stack. */
    fun logout()

    /** Pop the top entry from the back stack if more than one entry exists. */
    fun popBackStack()
}
