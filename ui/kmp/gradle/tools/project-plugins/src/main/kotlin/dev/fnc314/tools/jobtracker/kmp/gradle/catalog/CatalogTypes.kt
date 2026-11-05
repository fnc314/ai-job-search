package dev.fnc314.tools.jobtracker.kmp.gradle.catalog

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

/** Base typed contract for catalog entry lookups. Kept internal to prevent leaking to consuming buildscripts. */
internal sealed interface CatalogTypes<out T> {
    public val catalogKey: String
    public fun fromCatalog(project: Project): T

    public operator fun invoke(project: Project): T = fromCatalog(project)

    /** Strongly-typed version constraints from libs.versions.toml */
    public enum class Versions(
        override val catalogKey: String,
    ) : CatalogTypes<VersionConstraint> {
        JVM("jvm"),
        ANDROID_COMPILE_SDK("android-compileSdk"),
        ANDROID_MIN_SDK("android-minSdk"),
        ANDROID_TARGET_SDK("android-targetSdk"),
        KOTLIN("kotlin"),
        AGP("agp"),
        NODE("node");

        override fun fromCatalog(project: Project): VersionConstraint =
            project.versionCatalog.findVersion(catalogKey).orElseThrow {
                IllegalArgumentException("Missing version '$catalogKey' in catalog")
            }

        public fun asString(project: Project): String = fromCatalog(project).requiredVersion
        public fun asInt(project: Project): Int = asString(project).toInt()
    }

    /** Strongly-typed library coordinates from libs.versions.toml */
    public enum class Libraries(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<MinimalExternalModuleDependency>> {
        KOTLINX_COROUTINES_CORE("kotlinx-coroutines-core"),
        KOTLINX_DATETIME("kotlinx-datetime"),
        KOTLINX_SERIALIZATION_JSON("kotlinx-serialization-json"),
        KOTLIN_TEST("kotlin-test"),
        KOTLIN_TEST_JUNIT("kotlin-testJunit"),
        KOTLINX_COROUTINES_TEST("kotlinx-coroutines-test"),
        JUNIT("junit"),
        MOCKK("mockk"),
        KTOR_CLIENT_OKHTTP("ktor-client-okhttp"),
        KTOR_CLIENT_CIO("ktor-client-cio"),
        KTOR_CLIENT_DARWIN("ktor-client-darwin"),
        KTOR_CLIENT_JS("ktor-client-js"),
        COMPOSE_UI_TOOLING("compose-uiTooling"),
        NAVIGATION_COMPOSE("navigation-compose"),
        ANDROIDX_ROOM3_COMPILER("androidx-room3-compiler"),
        KSP_GRADLE_PLUGIN("ksp-gradle-plugin"),
        DOKKA_GRADLE_PLUGIN("dokka-gradle-plugin");

        override fun fromCatalog(project: Project): Provider<MinimalExternalModuleDependency> =
            project.versionCatalog.findLibrary(catalogKey).orElseThrow {
                IllegalArgumentException("Missing library '$catalogKey' in catalog")
            }
    }

    /** Strongly-typed dependency bundles from libs.versions.toml */
    public enum class Bundles(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<ExternalModuleDependencyBundle>> {
        COMPOSE_UI("compose-ui"),
        COMPOSE_TOOLING("compose-tooling"),
        KTOR_CLIENT("ktor-client"),
        KOIN("koin"),
        ANDROIDX_LIFECYCLE_COMPOSE("androidx-lifecycle-compose"),
        FIREBASE_ANDROID("firebase-android"),
        FIREBASE_JVM("firebase-jvm"),
        NAVIGATION3("navigation3"),
        TESTING("testing"),
        TESTING_JVM("testing-jvm"),
        TESTING_ANDROID("testing-android"),
        ROOM("room"),
        SQLITE_BUNDLED("sqlite-bundled"),
        SQLITE_WEB("sqlite-web");

        override fun fromCatalog(project: Project): Provider<ExternalModuleDependencyBundle> =
            project.versionCatalog.findBundle(catalogKey).orElseThrow {
                IllegalArgumentException("Missing bundle '$catalogKey' in catalog")
            }
    }

    /** Strongly-typed plugin dependencies from libs.versions.toml */
    public enum class Plugins(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<PluginDependency>> {
        KOTLIN_MULTIPLATFORM("kotlinMultiplatform"),
        KOTLIN_JVM("kotlinJvm"),
        KOTLIN_SERIALIZATION("kotlinSerialization"),
        ANDROID_APPLICATION("androidApplication"),
        ANDROID_MULTIPLATFORM_LIBRARY("androidMultiplatformLibrary"),
        COMPOSE_MULTIPLATFORM("composeMultiplatform"),
        COMPOSE_COMPILER("composeCompiler"),
        KSP("ksp"),
        DOKKA("dokka"),
        LOCAL_DOKKA("local-dokka");

        override fun fromCatalog(project: Project): Provider<PluginDependency> =
            project.versionCatalog.findPlugin(catalogKey).orElseThrow {
                IllegalArgumentException("Missing plugin '$catalogKey' in catalog")
            }
    }
}

/** Extension to retrieve the default 'libs' VersionCatalog (internal) */
internal val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

/** Convenience lookup extension on Project (internal) */
internal fun <T> Project.fromCatalog(catalogType: CatalogTypes<T>): T = catalogType.fromCatalog(this)
