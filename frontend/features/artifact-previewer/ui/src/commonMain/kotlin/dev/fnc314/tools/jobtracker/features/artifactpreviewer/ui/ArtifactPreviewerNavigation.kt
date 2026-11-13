package dev.fnc314.tools.jobtracker.features.artifactpreviewer.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.tools.jobtracker.core.navigation.TopBarActions
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.Res
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for the full-page artifact preview screen.
 */
@Serializable
public data class ArtifactPreviewRoute(
    val applicationId: String,
    val artifactId: String,
) : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.artifact_previewer_route_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.DetailWithCustomActions
}

public class ArtifactPreviewerEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<ArtifactPreviewRoute> { route ->
            ArtifactPreviewScreen(
                applicationId = route.applicationId,
                artifactId = route.artifactId,
            )
        }
    }
}
