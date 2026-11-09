package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Functional convention plugin applying Compose Multiplatform and Jetpack Compose compiler plugins.
 */
public class ComposeConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        plugins.apply(CatalogTypes.Plugins.COMPOSE_MULTIPLATFORM(this).get().pluginId)
        plugins.apply(CatalogTypes.Plugins.COMPOSE_COMPILER(this).get().pluginId)
    }
}
