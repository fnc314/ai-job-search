package dev.fnc314.tools.jobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Architecture test suite for `records` subsystem (`records-proto` and `records-parser`).
 *
 * Enforces architectural boundaries, naming standards, zero Android framework dependency leakage,
 * and comprehensive KDoc documentation on every public declaration.
 */
class RecordsArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `no android imports exist within records project`() {
        recordsScope
            .files
            .assertTrue { file ->
                file.imports.none { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            }
    }

    @Test
    fun `all records source files reside within dev fnc314 aijobtracker records package`() {
        recordsScope
            .files
            .filterNot { it.path.contains("/test/") || it.path.contains("/jvmTest/") || it.path.contains("/build/") || it.path.contains("/bin/") }
            .assertTrue { file ->
                file.packagee?.name?.startsWith("dev.fnc314.aijobtracker.records") == true
            }
    }

    @Test
    fun `all public declarations in parser have explicit return types`() {
        recordsScope
            .functions()
            .filter { it.hasPublicModifier }
            .filter { it.containingFile.path.contains("/parser/") }
            .filterNot { it.containingFile.path.contains("/test/") || it.containingFile.path.contains("/jvmTest/") || it.containingFile.path.contains("/build/") || it.containingFile.path.contains("/bin/") }
            .assertTrue { it.hasReturnType() }
    }

    @Test
    fun `no standalone ByString enum lookup maps or EnumMapping monolith exist in records`() {
        recordsScope
            .properties()
            .assertTrue { prop -> !prop.name.endsWith("ByString") }

        recordsScope
            .objects()
            .assertTrue { obj -> obj.name != "EnumMapping" }
    }

    @Test
    fun `all public declarations in records have KDoc`() {
        val decls = recordsScope.classes() +
            recordsScope.interfaces() +
            recordsScope.objects() +
            recordsScope.functions() +
            recordsScope.properties()

        decls
            .filter { it.hasPublicModifier }
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .assertTrue { it.hasKDoc }
    }
}
