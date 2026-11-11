package dev.fnc314.tools.jobtracker.features.companydetails.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.navigation.ApplicationId
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_company_details_ui.generated.resources.company_details_title
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
) : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.company_details_title
}

internal class CompanyDetailsEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
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
