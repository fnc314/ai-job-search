package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.commonMain
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Concern convention plugin providing Jetpack Navigation Compose and serialization dependencies.
 */
public class NavigationDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        kotlinMultiplatform {
            commonMain {
                dependencies {
                    implementation(CatalogTypes.Libraries.KOTLINX_SERIALIZATION_JSON(project))
                    implementation(CatalogTypes.Bundles.NAVIGATION3(project))
                }
            }
        }
    }
}
