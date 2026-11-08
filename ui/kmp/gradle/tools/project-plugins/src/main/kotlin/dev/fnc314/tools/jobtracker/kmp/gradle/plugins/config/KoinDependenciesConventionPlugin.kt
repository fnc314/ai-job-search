package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.commonMain
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.koin.compiler.plugin.KoinGradleExtension

/**
 * Concern convention plugin providing Koin core and Compose multiplatform dependency injection bundles.
 */
public class KoinDependenciesConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        CatalogTypes.Plugins.KOIN_COMPILER(this).get().also {
            plugins.apply(it.pluginId)
        }
        extensions.configure(KoinGradleExtension::class.java) { ext ->
            ext.apply {
                userLogs.set(true)
                debugLogs.set(true)
                skipDefaultValues.set(true)
                unsafeDslChecks.set(true)
                compileSafety.set(true)
                strictSafety.set(true)
                aiAssist.set(true)
                logSeverity.set("info")
                versionCheckSeverity.set("info")
            }

        }
        kotlinMultiplatform {
            commonMain {
                dependencies {
                    implementation(CatalogTypes.Bundles.KOIN(project))
                    implementation(CatalogTypes.Bundles.KOIN_COMPILER(project))
                }
            }
        }
    }
}
