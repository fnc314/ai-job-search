package dev.fnc314.tools.jobtracker.gradle.plugins.functional

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.BaseConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.configure
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

public class AndroidApplicationConventionPlugin : Plugin<Project>, BaseConventionPlugin {

    private fun Project.configureDefaultConfig(androidExt: ApplicationExtension) {
        androidExt.apply {
            defaultConfig {
                namespace = "dev.fnc314.tools.jobtracker.app.android"
                applicationId = "dev.fnc314.tools.jobtracker"
                versionCode = 1
                versionName = "1.0.0"
                buildToolsVersion =
                    CatalogTypes.Versions.ANDROID_BUILD_TOOLS.asString(project = this@configureDefaultConfig)
                minSdk {
                    release(
                        version = CatalogTypes.Versions.ANDROID_MIN_SDK.asInt(project = this@configureDefaultConfig),
                    )
                }
                compileSdk {
                    release(
                        version = CatalogTypes.Versions.ANDROID_COMPILE_SDK.asInt(project = this@configureDefaultConfig),
                    ) {
                        minorApiLevel =
                            CatalogTypes.Versions.ANDROID_COMPILE_SDK_MINOR.asInt(project = this@configureDefaultConfig)
                    }
                }
                targetSdk {
                    release(
                        version = CatalogTypes.Versions.ANDROID_TARGET_SDK.asInt(project = this@configureDefaultConfig),
                    )
                }
            }
        }
    }

    override fun apply(target: Project) {
        target.configurePlugin()
    }

    override fun Project.configurePlugin() {
        plugins.apply(CatalogTypes.Plugins.ANDROID_APPLICATION(this).get().pluginId)
        extensions.configure(
            ApplicationAndroidComponentsExtension::class.java,
        ) { android ->
            android.finalizeDsl { appExt ->
                appExt.apply {
                    enableKotlin = true
                    this@configurePlugin.configureDefaultConfig(androidExt = appExt)
                    dependenciesInfo {
                        includeInApk = false
                        includeInBundle = false
                    }
                    androidResources {
                        failOnMissingConfigEntry = true
                        generateLocaleConfig = true
                    }
                    testOptions {
                        unitTests {
                            isIncludeAndroidResources = true
                            isReturnDefaultValues = true
                        }
                        animationsDisabled = false
                    }
                    context(this@configurePlugin) {
                        compileOptions.configure()
                        lint.configure()
                    }
                }
            }
        }

        dependencies.addProvider(
            "debugImplementation",
            CatalogTypes.Bundles.KMP_INSPECTOR(this),
        )
        dependencies.addProvider(
            "releaseImplementation",
            CatalogTypes.Libraries.KMP_INSPECTOR_NOOP(this),
        )
        dependencies.addProvider(
            "coreLibraryDesugaring",
            CatalogTypes.Libraries.DESUGARING(this),
        )
    }
}