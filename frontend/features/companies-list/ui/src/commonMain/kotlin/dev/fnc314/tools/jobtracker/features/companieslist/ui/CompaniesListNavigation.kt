package dev.fnc314.tools.jobtracker.features.companieslist.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.CompanyId
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
public data object JobApplicationCompaniesRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.companies_route_title
}

internal class CompaniesEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
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
