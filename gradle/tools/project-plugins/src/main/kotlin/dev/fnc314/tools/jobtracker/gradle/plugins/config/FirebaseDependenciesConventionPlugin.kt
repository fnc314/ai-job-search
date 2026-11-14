package dev.fnc314.tools.jobtracker.gradle.plugins.config

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.catalog.versionCatalog
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.androidMain
import dev.fnc314.tools.jobtracker.gradle.plugins.hasAndroidPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.hasKotlinPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project

public class FirebaseDependenciesConventionPlugin : BaseConventionPlugin, Plugin<Project> {
    override fun apply(target: Project) {
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
            kotlinMultiplatform {
                sourceSets.androidMain.dependencies {
                    CatalogTypes.Bundles.FIREBASE_ANDROID(this@configurePlugin).get().forEach {
                        implementation(it)
                    }
                }
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