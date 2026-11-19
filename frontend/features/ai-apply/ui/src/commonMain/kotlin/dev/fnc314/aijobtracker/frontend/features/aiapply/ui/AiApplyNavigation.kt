package dev.fnc314.aijobtracker.frontend.features.aiapply.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.ApplicationId
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.features.artifactpreviewer.ui.ArtifactPreviewRoute
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_assistant_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for the AI Apply assistant feature.
 */
@Serializable
public data object AiApplyRoute : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.apply_assistant_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.TopLevel
}

/**
 * Navigation entry registry providing routes for the AI Apply assistant feature.
 */
public class AiApplyEntryRegistry : FeatureEntryRegistry {
    override operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<AiApplyRoute> {
            val navigator = LocalAppNavigator.current
            AiApplyScreen(
                onOpenApplication = { id ->
                    navigator.navigateToApplicationDetails(ApplicationId(id))
                },
                onPreviewArtifact = { appId, artifactId ->
                    navigator.navigate(ArtifactPreviewRoute(applicationId = appId, artifactId = artifactId))
                },
            )
        }
    }
}
