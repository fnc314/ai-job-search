package dev.fnc314.tools.jobtracker.gradle.plugins.config

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.commonMain
import dev.fnc314.tools.jobtracker.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Concern convention plugin providing Room 3 runtime and SQLite platform-specific driver bundles across all targets.
 */
public class DataSourceDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        pluginManager.withPlugin(CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId) {
            kotlinMultiplatform {
                commonMain {
                    dependencies {
                        implementation(CatalogTypes.Bundles.ROOM(project))
                    }
                }
                sourceSets.matching { it.name == "iosMain" }.configureEach { sourceSet ->
                    sourceSet.dependencies {
                        implementation(CatalogTypes.Bundles.SQLITE_BUNDLED(project))
                    }
                }
                sourceSets.matching { it.name == "jvmMain" }.configureEach { sourceSet ->
                    sourceSet.dependencies {
                        implementation(CatalogTypes.Bundles.SQLITE_BUNDLED(project))
                    }
                }
                sourceSets.matching { it.name == "androidMain" }.configureEach { sourceSet ->
                    sourceSet.dependencies {
                        implementation(CatalogTypes.Bundles.SQLITE_BUNDLED(project))
                    }
                }
                sourceSets.matching { it.name == "wasmJsMain" || it.name == "jsMain" }.configureEach { sourceSet ->
                    sourceSet.dependencies {
                        implementation(CatalogTypes.Bundles.SQLITE_WEB(project))
                    }
                }
            }
        }
    }
}
