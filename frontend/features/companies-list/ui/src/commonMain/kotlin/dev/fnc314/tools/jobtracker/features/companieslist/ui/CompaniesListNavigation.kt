package dev.fnc314.tools.jobtracker.features.companieslist.ui

import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_companies_list_ui.generated.resources.companies_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
public data object CompaniesListRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.companies_route_title
}

public fun companiesListNavHook(
    onCompanyClick: (companyId: String, companyName: String, website: String?) -> Unit,
): FeatureNavigationHook =
    FeatureNavigationHook { scope, _ ->
        scope.entry<CompaniesListRoute> {
            CompaniesListScreen(
                onCompanyClick = onCompanyClick,
            )
        }
    }
