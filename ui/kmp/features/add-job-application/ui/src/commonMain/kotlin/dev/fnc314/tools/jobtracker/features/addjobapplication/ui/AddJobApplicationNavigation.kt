package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import kotlinx.serialization.Serializable

@Serializable
public data object AddApplicationRoute : NavKeyRoute

@Serializable
public data object AddLeadRoute : NavKeyRoute

public fun addJobApplicationNavHook(
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<AddApplicationRoute> {
        AddJobApplicationScreen(
            onNavigateBack = onNavigateBack,
        )
    }
    navGraphBuilder.composable<AddLeadRoute> {
        AddLeadScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
