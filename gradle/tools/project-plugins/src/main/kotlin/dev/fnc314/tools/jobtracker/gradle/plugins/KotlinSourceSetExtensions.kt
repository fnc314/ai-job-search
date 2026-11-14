package dev.fnc314.tools.jobtracker.gradle.plugins

import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal val KotlinMultiplatformExtension.commonMain: KotlinSourceSet
    get() = sourceSets.getByName("commonMain")

internal fun KotlinMultiplatformExtension.commonMain(configure: KotlinSourceSet.() -> Unit): KotlinSourceSet =
    commonMain.apply(configure)

internal val KotlinMultiplatformExtension.commonTest: KotlinSourceSet
    get() = sourceSets.getByName("commonTest")

internal fun KotlinMultiplatformExtension.commonTest(configure: KotlinSourceSet.() -> Unit): KotlinSourceSet =
    commonTest.apply(configure)

internal val KotlinMultiplatformExtension.androidMain: KotlinSourceSet?
    get() = sourceSets.findByName("androidMain")

internal fun KotlinMultiplatformExtension.androidMain(configure: KotlinSourceSet.() -> Unit) {
    androidMain?.apply(configure)
}

internal val KotlinMultiplatformExtension.jvmMain: KotlinSourceSet?
    get() = sourceSets.findByName("jvmMain")

internal fun KotlinMultiplatformExtension.jvmMain(configure: KotlinSourceSet.() -> Unit) {
    jvmMain?.apply(configure)
}

internal val KotlinMultiplatformExtension.iosMain: KotlinSourceSet?
    get() = sourceSets.findByName("iosMain")

internal fun KotlinMultiplatformExtension.iosMain(configure: KotlinSourceSet.() -> Unit) {
    iosMain?.apply(configure)
}

internal val Project.kotlinMultiplatform: KotlinMultiplatformExtension
    get() = extensions.getByType(KotlinMultiplatformExtension::class.java)

internal inline fun Project.kotlinMultiplatform(crossinline configure: KotlinMultiplatformExtension.() -> Unit): Unit {
    kotlinMultiplatform.configure()
}

internal val Project.hasKotlinPlugin: Boolean
    get() = plugins.let {
        it.hasPlugin(
            CatalogTypes.Plugins.KOTLIN_JVM(this).get().pluginId
        ) ||
                it.hasPlugin(
                    CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId
                )
    }

internal val Project.hasAndroidPlugin: Boolean
    get() = plugins.let {
        it.hasPlugin(
            CatalogTypes.Plugins.ANDROID_APPLICATION(this).get().pluginId
        ) ||
                it.hasPlugin(
                    CatalogTypes.Plugins.ANDROID_MULTIPLATFORM_LIBRARY(this).get().pluginId
                )
    }

/**
 * Initializes the [KotlinMultiplatformExtension] on the receiver [Project]
 *
 * @param optIns A [Set] of fully-qualified opt-in annotations to apply to all Kotlin source sets
 * @receiver A [Project] instance
 */
@OptIn(
    ExperimentalAbiValidation::class,
    ExperimentalKotlinGradlePluginApi::class,
)
internal inline fun Project.configureKotlinMultiplatformExtension(
    optIns: Set<String> = emptySet(),
    crossinline furtherConfig: context(Project) KotlinMultiplatformExtension.() -> Unit = {},
) {
    this@configureKotlinMultiplatformExtension.kotlinMultiplatform {
        dependencies {

        }
        explicitApiWarning()
        abiValidation {
            it.referenceDumpDir.set(
                this@configureKotlinMultiplatformExtension.layout.projectDirectory.dir("api")
            )
        }
        jvmToolchain(
            CatalogTypes.Versions.JVM_TOOLCHAIN.asInt(this@configureKotlinMultiplatformExtension)
        )
        compilerOptions {
            verbose.set(true)
            apiVersion.set(
                this@configureKotlinMultiplatformExtension.kotlinApiAndLanguageVersion
            )
            languageVersion.set(
                this@configureKotlinMultiplatformExtension.kotlinApiAndLanguageVersion
            )
            optIn.set(optIns)
        }
        context(this@configureKotlinMultiplatformExtension) {
            furtherConfig()
        }
    }
}

internal val Project.kotlinApiAndLanguageVersion: KotlinVersion
    get() = CatalogTypes.Versions.KOTLIN.asString(this).let {
        KotlinVersion.fromVersion(it.substringBeforeLast("."))
    }