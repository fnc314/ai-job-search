package dev.fnc314.tools.jobtracker.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

public fun interface FeatureNavigationHook {
    public operator fun invoke(
        navGraphBuilder: NavGraphBuilder,
        navController: NavHostController,
    )
}
