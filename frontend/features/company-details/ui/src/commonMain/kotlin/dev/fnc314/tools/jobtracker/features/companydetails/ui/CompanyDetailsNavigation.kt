package dev.fnc314.tools.jobtracker.features.companydetails.ui

import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for viewing details of an employer and its openings.
 *
 * @property companyId Canonical company slug.
 * @property companyName Display name used in dynamic top app bar headers.
 * @property website Company website URL used for external navigation.
 */
@Serializable
public data class CompanyDetailsRoute(
    public val companyId: String,
    public val companyName: String = "",
    public val website: String? = null,
) : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.company_details_title
}

public fun companyDetailsNavHook(
    onNavigateBack: () -> Unit,
    onNavigateToApplication: (applicationId: String) -> Unit,
    onOpenUrl: ((String) -> Unit)? = null,
): FeatureNavigationHook =
    FeatureNavigationHook { scope, _ ->
        scope.entry<CompanyDetailsRoute> { key ->
            CompanyDetailsScreen(
                companyId = key.companyId,
                onNavigateBack = onNavigateBack,
                onNavigateToApplication = onNavigateToApplication,
                onOpenUrl = onOpenUrl,
            )
        }
    }
