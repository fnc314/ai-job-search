package dev.fnc314.aijobtracker.architecture

import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Konsist architecture test suite enforcing Single Abstract Method (SAM) conventions
 * across all Kotlin code in the repository:
 * 1. Any interface that contains exactly one abstract method to implement must be declared as a `fun interface`.
 * 2. Directly declared single abstract methods must use `operator fun invoke(...)`.
 */
class SingleAbstractMethodArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `single abstract method interfaces must be declared as fun interfaces`() {
        val allScopes = listOf(recordsScope, backendScope, gradleToolsScope, frontendScope)

        for (scope in allScopes) {
            scope
                .interfaces()
                .filterNot { it.hasSealedModifier }
                .filterNot {
                    it.containingFile.path.contains("/test/") ||
                        it.containingFile.path.contains("/jvmTest/") ||
                        it.containingFile.path.contains("/build/") ||
                        it.containingFile.path.contains("/bin/") ||
                        it.name == "BaseConventionPlugin"
                }
                .filter { iface ->
                    val declaredFunctions = iface.functions(includeNested = false)
                    val abstractFunctions = declaredFunctions.filterNot { it.hasBlockBody || it.hasExpressionBody }
                    val abstractProperties = iface.properties(includeNested = false)
                        .filterNot { it.hasGetter }

                    abstractFunctions.size == 1 && abstractProperties.isEmpty()
                }
                .assertTrue { iface ->
                    iface.hasFunModifier
                }
        }
    }

    @Test
    fun `single abstract method interface functions must be named invoke and declare operator modifier`() {
        val allScopes = listOf(recordsScope, backendScope, gradleToolsScope, frontendScope)

        for (scope in allScopes) {
            scope
                .interfaces()
                .filterNot { it.hasSealedModifier }
                .filterNot {
                    it.containingFile.path.contains("/test/") ||
                        it.containingFile.path.contains("/jvmTest/") ||
                        it.containingFile.path.contains("/build/") ||
                        it.containingFile.path.contains("/bin/") ||
                        it.name == "BaseConventionPlugin"
                }
                .filter { iface ->
                    val declaredFunctions = iface.functions(includeNested = false)
                    val abstractFunctions = declaredFunctions.filterNot { it.hasBlockBody || it.hasExpressionBody }
                    val abstractProperties = iface.properties(includeNested = false)
                        .filterNot { it.hasGetter }

                    abstractFunctions.size == 1 && abstractProperties.isEmpty()
                }
                .assertTrue { iface ->
                    val singleMethod = iface.functions(includeNested = false)
                        .first { !it.hasBlockBody && !it.hasExpressionBody }
                    singleMethod.name == "invoke" && singleMethod.hasOperatorModifier
                }
        }
    }
}
