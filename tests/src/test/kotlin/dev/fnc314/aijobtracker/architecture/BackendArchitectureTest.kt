package dev.fnc314.aijobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Architecture test suite for `backend` subsystem.
 *
 * Enforces architectural boundaries and comprehensive KDoc documentation on every public declaration.
 */
class BackendArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `all public declarations in backend have KDoc`() {
        val decls = backendScope.classes() +
            backendScope.interfaces() +
            backendScope.objects() +
            backendScope.functions() +
            backendScope.properties()

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

    @Test
    fun `all public functions in backend have explicit return types`() {
        backendScope
            .functions()
            .filter { it.hasPublicModifier }
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .assertTrue { it.hasReturnType() }
    }
}
