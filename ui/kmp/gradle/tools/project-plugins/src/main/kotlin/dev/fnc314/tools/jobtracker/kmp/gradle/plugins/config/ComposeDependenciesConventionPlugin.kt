package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Concern convention plugin providing shared Compose UI, foundation, and tooling dependency bundles.
 */
public class ComposeDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation(CatalogTypes.Bundles.COMPOSE_UI(project))
        }

        kotlin.sourceSets.findByName("androidMain")?.dependencies {
            implementation(CatalogTypes.Bundles.COMPOSE_TOOLING(project))
            implementation(CatalogTypes.Libraries.COMPOSE_CONSTRAINT_LAYOUT(this@configurePlugin))
        }
    }
}
