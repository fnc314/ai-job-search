package dev.fnc314.aijobtracker.frontend.features.companydetails.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformFileOpener
import dev.fnc314.aijobtracker.frontend.core.navigation.ApplicationId
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.features_company_details_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_company_details_ui.generated.resources.company_details_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for viewing details of an employer and its openings.
 *
 * @property companyId Canonical company slug (minimal input; ViewModel loads display data).
 */
@Serializable
public data class JobApplicationCompanyDetailsRoute(
    public val companyId: String,
) : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.company_details_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.Detail
}

/**
 * Navigation entry registry providing routes for the company details feature.
 */
public class CompanyDetailsEntryRegistry : FeatureEntryRegistry {
    override operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationCompanyDetailsRoute> { key ->
            val navigator = LocalAppNavigator.current
            CompanyDetailsScreen(
                companyId = key.companyId,
                onNavigateBack = navigator::popBackStack,
                onNavigateToApplication = { id ->
                    navigator.navigateToApplicationDetails(ApplicationId(id))
                },
                onOpenUrl = { url -> PlatformFileOpener().openUrl(url) },
            )
        }
    }
}
