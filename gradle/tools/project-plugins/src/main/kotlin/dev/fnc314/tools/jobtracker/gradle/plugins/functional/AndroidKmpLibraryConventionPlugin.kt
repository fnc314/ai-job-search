package dev.fnc314.tools.jobtracker.gradle.plugins.functional

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.configure
import dev.fnc314.tools.jobtracker.gradle.plugins.configureKotlinMultiplatformExtension
import dev.fnc314.tools.jobtracker.gradle.plugins.internal.TestingConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Functional convention plugin configuring Android target support within a Kotlin Multiplatform library module.
 *
 * Configures `compileSdk` and `minSdk` versions from the version catalog, computes package namespaces
 * from directory structure if omitted, and applies Android unit testing harnesses.
 */
public class AndroidKmpLibraryConventionPlugin : Plugin<Project>, BaseConventionPlugin {

    override fun apply(target: Project) {
        target.configurePlugin()
    }

    override fun Project.configurePlugin() {
        plugins.apply(CatalogTypes.Plugins.ANDROID_MULTIPLATFORM_LIBRARY(this).get().pluginId)

        configureKotlinMultiplatformExtension()

        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

        val extAware = kotlin as ExtensionAware
        extAware.extensions.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) { android ->
            android.compileSdk = CatalogTypes.Versions.ANDROID_COMPILE_SDK.asInt(project)
            android.minSdk = CatalogTypes.Versions.ANDROID_MIN_SDK.asInt(project)
            if (android.namespace == null) {
                val relative = projectDir.relativeTo(rootDir).invariantSeparatorsPath
                val sanitized = relative.split('/').joinToString(".") { segment ->
                    segment.replace("-", "").lowercase()
                }
                android.namespace = "dev.fnc314.tools.jobtracker.$sanitized"
            }
            android.androidResources {
                enable = true
                failOnMissingConfigEntry = true
                resourcePrefix = "${project.name}_"
            }
            context(this) {
                android.lint.configure()
            }
        }

        plugins.apply(TestingConventionPlugin::class.java)
    }
}
