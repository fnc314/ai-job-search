package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

public class AndroidApplicationConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        plugins.apply(CatalogTypes.Plugins.ANDROID_APPLICATION(this).get().pluginId)

        dependencies.addProvider(
            "debugImplementation",
            CatalogTypes.Bundles.KMP_INSPECTOR(this)
        )
        dependencies.addProvider(
            "releaseImplementation",
            CatalogTypes.Libraries.KMP_INSPECTOR_NOOP(this)
        )
    }
}