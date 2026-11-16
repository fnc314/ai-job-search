package dev.fnc314.aijobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.provider.KoContainingDeclarationProvider
import com.lemonappdev.konsist.api.provider.modifier.KoOverrideModifierProvider

/**
 * Repository-wide architectural tests enforcing explicit visibility modifiers (aligning with `explicitApi`)
 * and KDoc documentation priorities.
 */
class VisibilityAndKDocRulesTest : BaseArchitectureTest() {

    @Test
    fun `all declarations in records have explicit visibility modifiers`() {
        val decls = recordsScope.classes() +
            recordsScope.interfaces() +
            recordsScope.objects() +
            recordsScope.functions() +
            recordsScope.properties()

        decls
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .assertTrue {
                it.hasPublicModifier || it.hasInternalModifier || it.hasPrivateModifier || it.hasProtectedModifier ||
                    (it as? KoOverrideModifierProvider)?.hasOverrideModifier == true ||
                    (it as? KoContainingDeclarationProvider)?.containingDeclaration is KoInterfaceDeclaration
            }
    }

    @Test
    fun `all declarations in backend have explicit visibility modifiers`() {
        val decls = backendScope.classes() +
            backendScope.interfaces() +
            backendScope.objects() +
            backendScope.functions() +
            backendScope.properties()

        decls
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .assertTrue {
                it.hasPublicModifier || it.hasInternalModifier || it.hasPrivateModifier || it.hasProtectedModifier ||
                    (it as? KoOverrideModifierProvider)?.hasOverrideModifier == true ||
                    (it as? KoContainingDeclarationProvider)?.containingDeclaration is KoInterfaceDeclaration
            }
    }

    @Test
    fun `all declarations in gradle tools have explicit visibility modifiers`() {
        val decls = gradleToolsScope.classes() +
            gradleToolsScope.interfaces() +
            gradleToolsScope.objects() +
            gradleToolsScope.functions() +
            gradleToolsScope.properties()

        decls
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .assertTrue {
                it.hasPublicModifier || it.hasInternalModifier || it.hasPrivateModifier || it.hasProtectedModifier ||
                    (it as? KoOverrideModifierProvider)?.hasOverrideModifier == true ||
                    (it as? KoContainingDeclarationProvider)?.containingDeclaration is KoInterfaceDeclaration
            }
    }

    @Test
    fun `audit internal declarations KDoc coverage across repository`() {
        val allScopes = listOf(recordsScope, backendScope, gradleToolsScope, frontendScope)
        var totalInternal = 0
        var internalWithKDoc = 0

        for (scope in allScopes) {
            val decls = scope.classes() +
                scope.interfaces() +
                scope.objects() +
                scope.functions() +
                scope.properties()

            val internalDecls = decls
                .filter { it.hasInternalModifier }
                .filterNot {
                    it.containingFile.path.contains("/test/") ||
                        it.containingFile.path.contains("/jvmTest/") ||
                        it.containingFile.path.contains("/androidTest/") ||
                        it.containingFile.path.contains("/build/")
                }

            totalInternal += internalDecls.size
            internalWithKDoc += internalDecls.count { it.hasKDoc }
        }

        println("Internal declarations KDoc coverage: $internalWithKDoc / $totalInternal (${if (totalInternal > 0) (internalWithKDoc * 100 / totalInternal) else 0}%)")
    }
}
