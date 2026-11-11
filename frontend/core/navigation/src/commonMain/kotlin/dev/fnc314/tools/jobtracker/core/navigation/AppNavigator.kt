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
    public fun navigateToApplicationDetails(applicationId: ApplicationId)

    /** Navigate to a company detail screen. */
    public fun navigateToCompanyDetails(companyId: CompanyId)

    /** Navigate to a top-level destination (bottom nav targets). */
    public fun navigateTo(topLevelDestination: TopLevelDestinationEnum)

    /** Navigate to the settings screen. */
    public fun navigateToSettings()

    /** Navigate to an arbitrary [JobApplicationsTrackerNavKey] destination. */
    public fun navigate(key: JobApplicationsTrackerNavKey)

    /** Log in — switch root to the main application screen, clearing the back stack. */
    public fun login()

    /** Log out — switch root to the sign-in screen, clearing the back stack. */
    public fun logout()

    /** Pop the top entry from the back stack if more than one entry exists. */
    public fun popBackStack()
}
