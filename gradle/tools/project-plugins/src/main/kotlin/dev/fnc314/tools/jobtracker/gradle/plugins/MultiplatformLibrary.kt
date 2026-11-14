package dev.fnc314.tools.jobtracker.gradle.plugins

import dev.fnc314.tools.jobtracker.gradle.plugins.internal.TestingConventionPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import javax.inject.Inject

/**
 * Abstract Gradle plugin applying multiplatform testing configurations.
 */
public abstract class MultiplatformLibrary @Inject constructor() : Plugin<Project> {
    public override fun apply(target: Project): Unit {
        target.plugins.apply(TestingConventionPlugin::class.java)
    }
}
