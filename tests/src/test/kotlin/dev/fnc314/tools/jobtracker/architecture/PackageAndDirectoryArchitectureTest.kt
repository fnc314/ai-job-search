package dev.fnc314.tools.jobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Architecture test suite enforcing package and directory alignment.
 *
 * Checks that all packages start with the canonical root and match physical filesystem directory paths.
 */
class PackageAndDirectoryArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `gradle tools packages start with dev fnc314 aijobtracker`() {
        gradleToolsScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue {
                it.packagee?.name?.startsWith("dev.fnc314.aijobtracker") == true
            }
    }

    @Test
    fun `gradle tools directory paths match package declarations`() {
        gradleToolsScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue { file ->
                val pkg = file.packagee?.name ?: return@assertTrue false
                val expectedSubpath = pkg.replace('.', '/')
                file.path.replace('\\', '/').substringBeforeLast('/').endsWith(expectedSubpath)
            }
    }
}
