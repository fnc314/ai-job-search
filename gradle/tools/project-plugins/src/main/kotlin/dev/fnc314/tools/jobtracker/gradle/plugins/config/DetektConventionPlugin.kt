package dev.fnc314.tools.jobtracker.gradle.plugins.config

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin that applies and configures Detekt static analysis and formatting.
 *
 * Configures the master rule configuration at `config/detekt/detekt.yml`, attaches
 * `detekt-formatting` and `compose-rules` plugins, and standardizes multiplatform analysis.
 */
public class DetektConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        pluginManager.apply(CatalogTypes.Plugins.DETEKT(this).get().pluginId)

        extensions.configure(DetektExtension::class.java) { detekt ->
            val configDir = rootProject.file("../../config/detekt").takeIf { it.exists() }
                ?: rootProject.file("../config/detekt").takeIf { it.exists() }
                ?: rootProject.file("config/detekt")

            detekt.toolVersion = CatalogTypes.Versions.DETEKT.asString(this)
            detekt.source.setFrom(
                files(
                    "src/commonMain/kotlin",
                    "src/jvmMain/kotlin",
                    "src/androidMain/kotlin",
                    "src/main/kotlin"
                )
            )
            detekt.config.setFrom(
                fileTree(configDir).include("detekt-*.yml")
            )
            detekt.buildUponDefaultConfig = false
            detekt.allRules = false
            detekt.parallel = true
        }

        dependencies.add(
            "detektPlugins",
            CatalogTypes.Libraries.DETEKT_FORMATTING(this).get()
        )
        dependencies.add(
            "detektPlugins",
            CatalogTypes.Libraries.COMPOSE_RULES_DETEKT(this).get()
        )
    }
}
