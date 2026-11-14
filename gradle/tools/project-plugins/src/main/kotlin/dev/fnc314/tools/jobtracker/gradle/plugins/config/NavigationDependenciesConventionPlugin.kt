package dev.fnc314.tools.jobtracker.gradle.plugins.config

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.commonMain
import dev.fnc314.tools.jobtracker.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Concern convention plugin providing Jetpack Navigation 3 and serialization dependencies.
 */
public class NavigationDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        configurations.configureEach { config: Configuration ->
            config.resolutionStrategy.eachDependency { details ->
                if (details.requested.group == "androidx.navigation3" && details.requested.name == "navigation3-ui") {
                    if (config.name.contains("ios", ignoreCase = true) ||
                        config.name.contains("js", ignoreCase = true) ||
                        config.name.contains("wasm", ignoreCase = true) ||
                        config.name.contains("apple", ignoreCase = true) ||
                        config.name.contains("native", ignoreCase = true) ||
                        config.name.contains("web", ignoreCase = true)
                    ) {
                        details.useTarget("androidx.navigation3:navigation3-runtime:${details.requested.version}")
                    }
                }
            }
        }

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
