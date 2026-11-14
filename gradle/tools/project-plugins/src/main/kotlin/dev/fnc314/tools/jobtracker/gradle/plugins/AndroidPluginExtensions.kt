package dev.fnc314.tools.jobtracker.gradle.plugins

import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.Lint
import dev.fnc314.tools.jobtracker.gradle.catalog.CatalogTypes
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * Initializes the receiver [Lint] within the context of a [Project]
 * @param project A [Project] instance
 * @receiver A [Lint] instance
 */
context(project: Project)
internal fun Lint.configure() {
    // set to true to turn off analysis progress reporting by lint
    quiet = false
    // if true, stop the gradle build if errors are found
    abortOnError = false
    // set to true to have all release builds run lint on issues with severity=fatal
    // and abort the build (controlled by abortOnError above) if fatal issues are found
    checkReleaseBuilds = true
    // if true, only report errors
    ignoreWarnings = false
    // if true, emit full/absolute paths to files with errors (true by default)
    absolutePaths = true
    // if true, check all issues, including those that are off by default
    checkAllWarnings = true
    // if true, treat all warnings as errors
    warningsAsErrors = true
    // turn off checking the given issue id's
    // disable.plus(setOf("TypographyFractions", "TypographyQuotes"))
    // turn on the given issue id's
    // enable.plus(setOf("RtlHardcoded", "RtlCompat", "RtlEnabled"))
    // check *only* the given issue id's
    // checkOnly.plus(setOf("NewApi", "InlinedApi"))
    // if true, don't include source code lines in the error output
    noLines = false
    // if true, show all locations for an error, do not truncate lists, etc.
    showAll = true
    // whether lint should include full issue explanations in the text error output
    explainIssues = true
    // Fallback lint configuration (default severities, etc.)
    lintConfig = project.layout.settingsDirectory
        .file("config/lint-android/lint.xml")
        .asFile
    // if true, write text report to standard output (false by default)
    printTextReport = true
    // Set the severity of the given issues to fatal (which means they will be
    // checked during release builds (even if the lint target is not included)
    // fatal
    // Set the severity of the given issues to error
    // error
    // Set the severity of the given issues to warning
    // warning
    // Set the severity of the given issues to informational
    // informational
    // Use (or create) a baseline file for issues that should not be reported
    // baseline = layout.settingsDirectory.file("config/android-lint/baseline.xml")
    // Normally most lint checks are not run on test sources (except the checks
    // dedicated to looking for mistakes in unit or instrumentation tests, unless
    // ignoreTestSources is true). You can turn on normal lint checking in all
    // sources with the following flag, false by default:
    checkTestSources = true
    // Like checkTestSources, but always skips analyzing tests -- meaning that it
    // also ignores checks that have explicitly asked to look at test sources, such
    // as the unused resource check.
    ignoreTestSources = false
    // Normally lint will skip generated sources, but you can turn it on with this flag
    checkGeneratedSources = true
    // Whether lint should check all dependencies too as part of its analysis.
    // Default is false.
    checkDependencies = true
    // targetSdk version used when generating a lint report for a library.
    // Must be equal or higher than main target SDK. Must be set for libraries only.
    targetSdk = CatalogTypes.Versions.ANDROID_TARGET_SDK.asInt(project = project)
}

context(project: Project)
internal fun CompileOptions.configure() {
    sourceCompatibility = CatalogTypes
        .Versions
        .JVM_TOOLCHAIN
        .asString(project = project)
        .let { JavaVersion.toVersion(it) }

    targetCompatibility = CatalogTypes
        .Versions
        .JVM_TOOLCHAIN
        .asString(project = project)
        .let { JavaVersion.toVersion(it) }

    isCoreLibraryDesugaringEnabled = true
}