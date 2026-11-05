package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.functional

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.internal.TestingConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

public class KotlinMultiplatformConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        plugins.apply(CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId)

        val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

        // Strict explicit API checking with warning severity
        kotlin.explicitApiWarning()

        // 1. Toolchain parity across all projects
        kotlin.jvmToolchain(CatalogTypes.Versions.JVM.asInt(this))

        // 2. Standard multiplatform targets
        with(kotlin) {
            jvm {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
            js {
                browser()
            }
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }
            iosArm64()
            iosSimulatorArm64()
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }

        // 3. Testing conventions (dependencies, logging, test tasks)
        plugins.apply(TestingConventionPlugin::class.java)
    }
}
