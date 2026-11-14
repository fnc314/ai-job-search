package dev.fnc314.tools.jobtracker.gradle.catalog

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
        JVM_TOOLCHAIN(catalogKey = "jvm-toolchain"),
        ANDROID_BUILD_TOOLS(catalogKey = "android-build-tools"),
        ANDROID_COMPILE_SDK(catalogKey = "android-compile-sdk"),
        ANDROID_COMPILE_SDK_MINOR(catalogKey = "android-compile-sdk-minor"),
        ANDROID_MIN_SDK(catalogKey = "android-min-sdk"),
        ANDROID_TARGET_SDK(catalogKey = "android-target-sdk"),
        KOTLIN(catalogKey = "kotlin"),
        AGP(catalogKey = "agp"),
        NODE(catalogKey = "node"),
        DETEKT(catalogKey = "detekt"),
        ;

        override fun fromCatalog(project: Project): VersionConstraint =
            project.versionCatalog.findVersion(catalogKey).orElseThrow {
                IllegalArgumentException("Missing version '$catalogKey' in catalog")
            }

        /** Returns the version string defined in the catalog. */
        public fun asString(project: Project): String = fromCatalog(project).requiredVersion

        /** Returns the version parsed as an integer (e.g. for compileSdk, JVM target). */
        public fun asInt(project: Project): Int = asString(project).toInt()
    }

    /** Strongly-typed library coordinates from libs.versions.toml */
    public enum class Libraries(
        override val catalogKey: String,
    ) : CatalogTypes<Provider<MinimalExternalModuleDependency>> {
        KOTLINX_COROUTINES_CORE(catalogKey = "kotlinx-coroutines-core"),
        KOTLINX_DATETIME(catalogKey = "kotlinx-datetime"),
        KOTLINX_SERIALIZATION_JSON(catalogKey = "kotlinx-serialization-json"),
        KOTLIN_TEST(catalogKey = "kotlin-test"),
        KOTLIN_TEST_JUNIT(catalogKey = "kotlin-testJunit"),
        KOTLINX_COROUTINES_TEST(catalogKey = "kotlinx-coroutines-test"),
        JUNIT(catalogKey = "junit"),
        MOCKK(catalogKey = "mockk"),
        KTOR_CLIENT_OKHTTP(catalogKey = "ktor-client-okhttp"),
        KTOR_CLIENT_CIO(catalogKey = "ktor-client-cio"),
        KTOR_CLIENT_DARWIN(catalogKey = "ktor-client-darwin"),
        KTOR_CLIENT_JS(catalogKey = "ktor-client-js"),
        KTOR_SERVER_CORE(catalogKey = "ktor-server-core"),
        KTOR_SERVER_NETTY(catalogKey = "ktor-server-netty"),
        FIREBASE_ADMIN(catalogKey = "firebase-admin"),
        COMPOSE_UI_TOOLING(catalogKey = "compose-uiTooling"),
        NAVIGATION_COMPOSE(catalogKey = "navigation-compose"),
        ANDROIDX_ROOM3_COMPILER(catalogKey = "androidx-room3-compiler"),
        KSP_GRADLE_PLUGIN(catalogKey = "ksp-gradle-plugin"),
        DOKKA_GRADLE_PLUGIN(catalogKey = "dokka-gradle-plugin"),
        DETEKT_FORMATTING(catalogKey = "detekt-formatting"),
        COMPOSE_RULES_DETEKT(catalogKey = "compose-rules-detekt"),
        COMPOSE_CONSTRAINT_LAYOUT(catalogKey = "compose-constraint"),
        KONSIST(catalogKey = "konsist"),
        FIREBASE_BOM(catalogKey = "firebase-bom"),
        KMP_INSPECTOR_NOOP(catalogKey = "kmp-inspector-noop"),
        DESUGARING(catalogKey = "desugaring"),
        ;

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
        COIL("coil"),
        KOIN("koin"),
        KOIN_COMPILER("koin-compiler"),
        ANDROIDX_LIFECYCLE_COMPOSE("androidx-lifecycle-compose"),
        FIREBASE_ANDROID("firebase-android"),
        FIREBASE_JVM("firebase-jvm"),
        NAVIGATION3("navigation3"),
        TESTING("testing"),
        TESTING_JVM("testing-jvm"),
        TESTING_ANDROID("testing-android"),
        ROOM("room"),
        SQLITE_BUNDLED("sqlite-bundled"),
        SQLITE_WEB("sqlite-web"),
        KMP_INSPECTOR("kmp-inspector"),
        MATERIAL3_ADAPTIVE("material3-adaptive"),
        ;

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
        KOIN_COMPILER("koin-compiler"),
        KSP("ksp"),
        DOKKA("dokka"),
        LOCAL_DOKKA("local-dokka"),
        DETEKT("detekt"),
        LOCAL_DETEKT("local-detekt"),
        GOOGLE_PLAY_SERVICES("google-play-services"),
        ;

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
