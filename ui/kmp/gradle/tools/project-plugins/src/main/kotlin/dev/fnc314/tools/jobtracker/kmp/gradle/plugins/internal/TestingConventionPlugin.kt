package dev.fnc314.tools.jobtracker.kmp.gradle.plugins.internal

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.commonTest
import dev.fnc314.tools.jobtracker.kmp.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Internal convention plugin configuring testing standards across all multiplatform projects:
 * - Test dependencies (`kotlin.test`, `kotlinx-coroutines-test`) in `commonTest`
 * - Consistent test logging and exception formatting on all Gradle [Test] tasks
 * - Host test integration for Android Kotlin Multiplatform library targets
 */
internal class TestingConventionPlugin : Plugin<Project>, BaseConventionPlugin {
    override fun apply(target: Project): Unit {
        target.configurePlugin()
    }

    override fun Project.configurePlugin(): Unit {
        // 1. Configure all Gradle Test tasks with standard logging and exception reporting
        tasks.withType(Test::class.java).configureEach { testTask ->
            testTask.testLogging.apply {
                setEvents(
                    listOf(
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.FAILED
                    )
                )
                exceptionFormat = TestExceptionFormat.FULL
                showExceptions = true
                showCauses = true
                showStackTraces = true
                showStandardStreams = false
            }
        }

        // 2. Configure Kotlin Multiplatform test source set dependencies
        pluginManager.withPlugin(CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId) {
            kotlinMultiplatform {
                commonTest {
                    dependencies {
                        implementation(CatalogTypes.Libraries.KOTLIN_TEST(project))
                        implementation(CatalogTypes.Libraries.KOTLINX_COROUTINES_TEST(project))
                    }
                }
            }
        }

        // 3. Configure Android Multiplatform host tests if android KMP library is applied
        pluginManager.withPlugin(CatalogTypes.Plugins.ANDROID_MULTIPLATFORM_LIBRARY(this).get().pluginId) {
            val kotlin = extensions.findByType(KotlinMultiplatformExtension::class.java)
            val extAware = kotlin as? ExtensionAware
            extAware?.extensions?.configure(KotlinMultiplatformAndroidLibraryTarget::class.java) { android ->
                android.withHostTest {}
            }
        }
    }
}
