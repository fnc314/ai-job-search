package dev.fnc314.tools.jobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Architecture test suite for `gradle/tools` project plugins.
 *
 * Enforces conventions and comprehensive KDoc documentation on every public declaration.
 */
class GradleToolsArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `all public declarations in gradle tools have KDoc`() {
        val decls = gradleToolsScope.classes() +
            gradleToolsScope.interfaces() +
            gradleToolsScope.objects() +
            gradleToolsScope.functions() +
            gradleToolsScope.properties()

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
    fun `all public functions in gradle tools have explicit return types`() {
        gradleToolsScope
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
