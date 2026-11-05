package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Concern convention plugin applying `kotlinx.serialization` compiler plugin and runtime JSON dependencies.
 */
public class SerializationDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        plugins.apply(CatalogTypes.Plugins.KOTLIN_SERIALIZATION(this).get().pluginId)

        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation(CatalogTypes.Libraries.KOTLINX_SERIALIZATION_JSON(project))
        }
    }
}
