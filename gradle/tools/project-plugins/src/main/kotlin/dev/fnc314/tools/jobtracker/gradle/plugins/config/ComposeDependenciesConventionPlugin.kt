package dev.fnc314.tools.jobtracker.gradle.plugins.config

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.config.kotlin.KotlinConfigConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Concern convention plugin providing shared Compose UI, foundation, and tooling dependency bundles.
 */
public class ComposeDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {

    override fun apply(target: Project) {
        target.configurePlugin()
    }

    override fun Project.configurePlugin() {
        pluginManager.apply(KotlinConfigConventionPlugin.ComposeUI::class.java)
    }
}
