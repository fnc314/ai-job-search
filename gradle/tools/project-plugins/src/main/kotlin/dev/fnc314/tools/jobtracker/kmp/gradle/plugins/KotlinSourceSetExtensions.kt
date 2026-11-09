package dev.fnc314.tools.jobtracker.kmp.gradle.plugins

import dev.fnc314.tools.jobtracker.kmp.gradle.catalog.CatalogTypes
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import javax.xml.catalog.Catalog

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

internal val Project.hasKotlinPlugin: Boolean get() = plugins.let {
    it.hasPlugin(
        CatalogTypes.Plugins.KOTLIN_JVM(this).get().pluginId
    ) ||
    it.hasPlugin(
        CatalogTypes.Plugins.KOTLIN_MULTIPLATFORM(this).get().pluginId
    )
}

internal val Project.hasAndroidPlugin: Boolean get() = plugins.let {
    it.hasPlugin(
        CatalogTypes.Plugins.ANDROID_APPLICATION(this).get().pluginId
    ) ||
    it.hasPlugin(
        CatalogTypes.Plugins.ANDROID_MULTIPLATFORM_LIBRARY(this).get().pluginId
    )
}