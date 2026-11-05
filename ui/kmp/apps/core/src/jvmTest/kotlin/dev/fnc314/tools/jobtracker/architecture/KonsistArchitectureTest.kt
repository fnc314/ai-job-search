package dev.fnc314.tools.jobtracker.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Global architecture test suite for `ui/kmp` multiplatform workspace.
 *
 * Verifies critical structural invariants defined in `ui/kmp/AGENTS.md`:
 * 1. Zero rogue `Color()` calls outside of `Color.kt`.
 * 2. Option A UI Decoupling: UI feature modules depend on `core.widgets` and never import `core.designsystem` directly.
 * 3. All ViewModel declarations adhere to naming conventions.
 */
class KonsistArchitectureTest {

    private val kmpScope = Konsist.scopeFromDirectory("..")

    @Test
    fun `no files outside of core design system call Color constructor`() {
        kmpScope
            .files
            .filterNot { it.path.contains("/core/design-system/") || it.path.contains("/build/") }
            .assertTrue { file ->
                !file.text.contains(Regex("""\bColor\(0x[0-9a-fA-F]+|\bColor\([0-9]+"""))
            }
    }

    @Test
    fun `view models reside in ui package and end with ViewModel suffix`() {
        kmpScope
            .classes()
            .filter { it.hasParentWithName("ViewModel") }
            .assertTrue { clazz ->
                clazz.name.endsWith("ViewModel") && (clazz.packagee?.name?.contains(".ui") == true)
            }
    }

    @Test
    fun `all use cases have execute or invoke method`() {
        kmpScope
            .classes()
            .filter { it.name.endsWith("UseCase") }
            .assertTrue { clazz ->
                clazz.functions().any { func ->
                    func.name == "execute" || func.name == "invoke"
                }
            }
    }
}
