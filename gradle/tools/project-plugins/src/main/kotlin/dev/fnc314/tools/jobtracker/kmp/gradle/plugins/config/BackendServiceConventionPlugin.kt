package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.config

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Convention plugin for backend service modules.
 * Applies Ktor, Firebase, serialization, and JVM 21 toolchain configuration.
 */
public class BackendServiceConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        plugins.apply(JavaPlugin::class.java)
        plugins.apply(CatalogTypes.Plugins.KOTLIN_JVM(this).get().pluginId)

        val kotlin = extensions.getByType(KotlinJvmProjectExtension::class.java)
        kotlin.jvmToolchain(21)


        // Add common dependencies
        dependencies.add("implementation", CatalogTypes.Libraries.KTOR_SERVER_CORE(project))
        dependencies.add("implementation", CatalogTypes.Libraries.KTOR_SERVER_NETTY(project))
        dependencies.add("implementation", CatalogTypes.Libraries.FIREBASE_ADMIN(project))
        dependencies.add("implementation", CatalogTypes.Libraries.KOTLINX_COROUTINES_CORE(project))
        dependencies.add("implementation", CatalogTypes.Libraries.KOTLINX_DATETIME(project))
        dependencies.add("implementation", CatalogTypes.Libraries.KOTLINX_SERIALIZATION_JSON(project))
    }
}
