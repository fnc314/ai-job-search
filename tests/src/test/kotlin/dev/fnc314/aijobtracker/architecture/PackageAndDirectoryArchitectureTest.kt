package dev.fnc314.aijobtracker.architecture

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

    @Test
    fun `records parser packages start with dev fnc314 aijobtracker`() {
        recordsScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue {
                it.packagee?.name?.startsWith("dev.fnc314.aijobtracker") == true
            }
    }

    @Test
    fun `records parser directory paths match package declarations`() {
        recordsScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue { file ->
                val pkg = file.packagee?.name ?: return@assertTrue false
                val expectedSubpath = pkg.replace('.', '/')
                file.path.replace('\\', '/').substringBeforeLast('/').endsWith(expectedSubpath)
            }
    }

    @Test
    fun `backend packages start with dev fnc314 aijobtracker`() {
        backendScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue {
                it.packagee?.name?.startsWith("dev.fnc314.aijobtracker.backend") == true
            }
    }

    @Test
    fun `backend directory paths match package declarations`() {
        backendScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue { file ->
                val pkg = file.packagee?.name ?: return@assertTrue false
                val expectedSubpath = pkg.replace('.', '/')
                file.path.replace('\\', '/').substringBeforeLast('/').endsWith(expectedSubpath)
            }
    }

    @Test
    fun `frontend packages start with dev fnc314 aijobtracker`() {
        frontendScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue {
                it.packagee?.name?.startsWith("dev.fnc314.aijobtracker.frontend") == true
            }
    }

    @Test
    fun `frontend directory paths match package declarations`() {
        frontendScope
            .files
            .filterNot { it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue { file ->
                val pkg = file.packagee?.name ?: return@assertTrue false
                val expectedSubpath = pkg.replace('.', '/')
                file.path.replace('\\', '/').substringBeforeLast('/').endsWith(expectedSubpath)
            }
    }
}
