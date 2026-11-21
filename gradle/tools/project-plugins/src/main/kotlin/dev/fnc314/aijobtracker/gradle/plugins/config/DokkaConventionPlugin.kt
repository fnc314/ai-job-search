package dev.fnc314.aijobtracker.gradle.plugins.config

import dev.fnc314.aijobtracker.gradle.catalog.CatalogTypes
import dev.fnc314.aijobtracker.gradle.plugins.BaseConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.DokkaExtension
import java.io.File

/**
 * Convention plugin that applies and configures Dokka 2.x for a participating project.
 *
 * Configures the HTML publication format with standardized module naming,
 * README inclusion as frontpiece documentation, and sets up documentation aggregation readiness.
 */
public class DokkaConventionPlugin :
  Plugin<Project>,
  BaseConventionPlugin {
  override fun apply(target: Project) {
    target.configurePlugin()
  }

  override fun Project.configurePlugin() {
    pluginManager.apply("org.jetbrains.dokka")
    extensions.configure(DokkaExtension::class.java) { dokka ->
      val modName = path.ifEmpty { ":$name" }
      dokka.dokkaPublications.named("html") { publication ->
        publication.moduleName.set(modName)
      }

      val readme = file("README.md")
      if (readme.exists()) {
        val generatedInclude = layout.buildDirectory.file("dokka/includes/module.md")
        val prepareTask =
          tasks.register("prepareDokkaModuleDocs") { task ->
            task.inputs.file(readme)
            task.outputs.file(generatedInclude)
            task.doLast {
              val original = readme.readText()
              // Replace any occurrence of "#... Module" or "#... Package" in markdown headings
              // so Dokka does not mistakenly parse them as additional Module/Package declaration directives.
              val sanitized =
                original.lines().joinToString("\n") { line ->
                  if (line.trimStart().startsWith("#")) {
                    line
                      .replace(Regex("(?i)\\bmodule\\b"), "Subsystem")
                      .replace(Regex("(?i)\\bpackage\\b"), "Namespace")
                  } else {
                    line
                  }
                }
              val content =
                buildString {
                  appendLine("# Module $modName")
                  appendLine()
                  append(sanitized)
                }
              val outputFile = generatedInclude.get().asFile
              outputFile.parentFile.mkdirs()
              outputFile.writeText(content)
            }
          }

        dokka.dokkaPublications.named("html") { publication ->
          publication.includes.from(generatedInclude)
        }
        dokka.dokkaSourceSets.configureEach { sourceSet ->
          sourceSet.includes.from(generatedInclude)
        }
        tasks.matching { it.name.startsWith("dokka") }.configureEach { task ->
          task.dependsOn(prepareTask)
        }
      }
    }
  }
}