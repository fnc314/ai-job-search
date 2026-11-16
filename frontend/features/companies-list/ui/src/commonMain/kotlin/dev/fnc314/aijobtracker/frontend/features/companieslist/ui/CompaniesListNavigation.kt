package dev.fnc314.aijobtracker.frontend.features.companieslist.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.CompanyId
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.features_companies_list_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_companies_list_ui.generated.resources.companies_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for the companies list directory.
 */
@Serializable
public data object JobApplicationCompaniesRoute : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.companies_route_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.TopLevel
}

/**
 * Navigation entry registry providing routes for the companies list feature.
 */
public class CompaniesEntryRegistry : FeatureEntryRegistry {
    override operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationCompaniesRoute> {
            val navigator = LocalAppNavigator.current
            CompaniesListScreen(
                onCompanyClick = { companyId ->
                    navigator.navigateToCompanyDetails(CompanyId(companyId))
                },
            )
        }
    }
}
