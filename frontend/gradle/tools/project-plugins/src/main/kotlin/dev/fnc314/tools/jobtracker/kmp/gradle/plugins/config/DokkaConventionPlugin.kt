package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension

/**
 * Convention plugin that applies and configures Dokka 2.x for a participating project.
 *
 * Configures the HTML publication format with standardized module naming,
 * README inclusion, and sets up documentation aggregation readiness.
 */
public class DokkaConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        pluginManager.apply(CatalogTypes.Plugins.DOKKA(this).get().pluginId)
        extensions.configure(DokkaExtension::class.java) { dokka ->
            dokka.dokkaPublications.named("html") { publication ->
                publication.moduleName.set(path)
            }
        }
    }
}
