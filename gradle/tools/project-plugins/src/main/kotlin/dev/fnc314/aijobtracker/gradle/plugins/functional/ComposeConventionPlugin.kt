package dev.fnc314.aijobtracker.gradle.plugins.functional

import dev.fnc314.aijobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.aijobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.aijobtracker.gradle.plugins.config.ComposeDependenciesConventionPlugin
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
        pluginManager.apply(ComposeDependenciesConventionPlugin::class.java)
    }
}
