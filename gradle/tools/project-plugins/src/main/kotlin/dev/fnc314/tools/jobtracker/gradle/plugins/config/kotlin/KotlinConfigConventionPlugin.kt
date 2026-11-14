package dev.fnc314.tools.jobtracker.gradle.plugins.config.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.tools.jobtracker.gradle.plugins.config.ComposeDependenciesConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.functional.ComposeConventionPlugin
import dev.fnc314.tools.jobtracker.gradle.plugins.configureKotlinMultiplatformExtension
import dev.fnc314.tools.jobtracker.gradle.plugins.hasKotlinPlugin

private val KOTLIN_OPT_INS = setOf(
    "kotlin.time.ExperimentalTime",
    "kotlin.uuid.ExperimentalUuidApi",
    "kotlin.io.path.ExperimentalPathApi",
    "kotlin.contracts.ExperimentalContracts",

    "kotlinx.coroutines.ExperimentalCoroutinesApi",
    "kotlinx.serialization.ExperimentalSerializationApi",
)

internal sealed interface KotlinConfigConventionPlugin : Plugin<Project> {
    val optIns: Set<String> get() = KOTLIN_OPT_INS

    val furtherConfig: context(Project) KotlinMultiplatformExtension.() -> Unit
        get() = {}

    abstract class Base constructor() : KotlinConfigConventionPlugin {
        final override fun apply(target: Project) {
            if (target.hasKotlinPlugin) {
                context(target) {
                    target.configureKotlinMultiplatformExtension(
                        optIns = optIns,
                        furtherConfig = furtherConfig
                    )
                }
            }
        }
    }

    abstract class ComposeUI constructor() : Base() {

        override val furtherConfig: context(Project) KotlinMultiplatformExtension.() -> Unit
            get() = {
                val project = contextOf<Project>()
                project.logger.lifecycle("Configuring Compose dependencies for project ${project.name}...")
                project.pluginManager.apply(CatalogTypes.Plugins.COMPOSE_MULTIPLATFORM(project).get().pluginId)
                project.pluginManager.apply(CatalogTypes.Plugins.COMPOSE_COMPILER(project).get().pluginId)

                sourceSets.getByName("commonMain").dependencies {
                    implementation(CatalogTypes.Bundles.COMPOSE_UI(project))
                    implementation(CatalogTypes.Bundles.COIL(project))
                    implementation(CatalogTypes.Bundles.MATERIAL3_ADAPTIVE(project))
                }

                sourceSets.findByName("androidMain")?.dependencies {
                    implementation(CatalogTypes.Bundles.COMPOSE_TOOLING(project))
                    implementation(CatalogTypes.Libraries.COMPOSE_CONSTRAINT_LAYOUT(project))
                }
            }
        override val optIns: Set<String> = KOTLIN_OPT_INS + setOf(
            "androidx.compose.ui.ExperimentalComposeUiApi",
            "androidx.compose.animation.ExperimentalAnimationApi",

            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",

            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi",
        )
    }
}