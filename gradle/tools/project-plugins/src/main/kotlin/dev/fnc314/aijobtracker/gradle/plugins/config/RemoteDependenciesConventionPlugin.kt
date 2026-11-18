package dev.fnc314.aijobtracker.gradle.plugins.config

import dev.fnc314.aijobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.aijobtracker.gradle.catalog.versionCatalog
import dev.fnc314.aijobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.aijobtracker.gradle.plugins.hasAndroidPlugin
import dev.fnc314.aijobtracker.gradle.plugins.hasKotlinPlugin
import dev.fnc314.aijobtracker.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin configuring Firebase dependencies, Google Play Services, and platform-specific BOMs.
 */
public class RemoteDependenciesConventionPlugin : BaseConventionPlugin, Plugin<Project> {
    /**
     * Applies Firebase dependencies to the target Gradle [Project].
     *
     * @param target The Gradle project being configured.
     */
    public override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin() {
        if (
            plugins.hasPlugin(
                CatalogTypes.Plugins.ANDROID_APPLICATION(this).get().pluginId
            )
        ) {
            plugins.apply(
                CatalogTypes.Plugins.GOOGLE_PLAY_SERVICES(this).get().pluginId
            )
        }

        if (hasKotlinPlugin) {
            CatalogTypes.Bundles.FIREBASE_ANDROID(this).get().forEach {
                dependencies.add("androidMainImplementation", it)
            }
            kotlinMultiplatform {
                sourceSets.jvmMain.dependencies {
                    CatalogTypes.Bundles.FIREBASE_JVM(this@configurePlugin).get().forEach {
                        implementation(it)
                    }
                }
            }
        } else if (hasAndroidPlugin) {
            dependencies.enforcedPlatform(
                CatalogTypes.Libraries.FIREBASE_BOM(this)
            )
            CatalogTypes.Bundles.FIREBASE_ANDROID(this).get().forEach {
                dependencies.add("implementation", it)
            }
        } else {
            dependencies.enforcedPlatform(
                CatalogTypes.Libraries.FIREBASE_BOM(this)
            )
            CatalogTypes.Bundles.FIREBASE_JVM(this).get().forEach {
                dependencies.add("implementation", it)
            }
        }
    }
}
