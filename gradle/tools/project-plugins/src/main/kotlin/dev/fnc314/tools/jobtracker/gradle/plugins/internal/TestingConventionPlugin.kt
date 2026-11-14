package dev.fnc314.tools.jobtracker.gradle.plugins.internal

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.commonTest
import dev.fnc314.tools.jobtracker.gradle.plugins.kotlinMultiplatform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Internal convention plugin configuring testing standards across all multiplatform projects:
 * - Multiplatform test bundle (`kotlin.test`, coroutines-test, serialization, immutable collections, datetime) in `commonTest`
 * - JVM test bundle (`junit`, `kotlin-test-junit`) in `jvmTest`
 * - Android test bundle (`androidx-testExt-junit`, `androidx-testRunner`) in `androidHostTest`
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

        // 2. Configure Kotlin Multiplatform test source set dependencies via bundles
        pluginManager.withPlugin(CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId) {
            kotlinMultiplatform {
                commonTest {
                    dependencies {
                        implementation(CatalogTypes.Bundles.TESTING(project))
                    }
                }
                sourceSets.matching { it.name == "jvmTest" }.configureEach { jvmTest ->
                    jvmTest.dependencies {
                        implementation(CatalogTypes.Bundles.TESTING_JVM(project))
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
            kotlin?.sourceSets?.matching { it.name == "androidHostTest" }?.configureEach { hostTest ->
                hostTest.dependencies {
                    implementation(CatalogTypes.Bundles.TESTING_ANDROID(project))
                }
            }
        }
    }
}
