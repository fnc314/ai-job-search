package dev.fnc314.tools.jobsearch.records.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Global architecture test suite for the `records` project (`records-proto` and `records-parser`).
 *
 * Enforces architectural boundaries, naming standards, and zero Android framework dependency leakage.
 */
class RecordsArchitectureTest {

    private val recordsScope = Konsist.scopeFromProject()

    @Test
    fun `no android imports exist within records project`() {
        recordsScope
            .files
            .assertTrue { file ->
                file.imports.none { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            }
    }

    @Test
    fun `all records source files reside within dev fnc314 tools jobsearch package`() {
        recordsScope
            .files
            .assertTrue { file ->
                file.packagee?.name?.startsWith("dev.fnc314.tools.jobsearch") == true
            }
    }

    @Test
    fun `all public declarations in parser have explicit return types`() {
        Konsist.scopeFromPackage("dev.fnc314.tools.jobsearch.records.parser..")
            .functions()
            .filter { it.hasPublicModifier }
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
}
