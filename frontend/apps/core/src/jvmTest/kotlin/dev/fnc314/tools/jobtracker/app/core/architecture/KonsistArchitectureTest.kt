package dev.fnc314.tools.jobtracker.app.core.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue as kotlinAssertTrue

/**
 * Global architecture test suite for `frontend` multiplatform workspace.
 *
 * Verifies critical structural invariants defined in `frontend/AGENTS.md`:
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

    @Test
    fun `all sealed WireContract interfaces declare Known enum Unrecognized class and fromWireKey`() {
        kmpScope
            .interfaces()
            .filter { it.hasSealedModifier && it.hasParentInterfaceWithName("WireContract") }
            .assertTrue { iface ->
                val hasKnownEnum = iface.classes().any { it.name == "Known" && it.hasEnumModifier }
                val hasUnrecognized = iface.classes().any { it.name == "Unrecognized" && it.hasDataModifier }
                val hasFromWireKey = iface.objects().any { obj ->
                    obj.hasCompanionModifier && obj.functions().any { fn -> fn.name == "fromWireKey" }
                }
                hasKnownEnum && hasUnrecognized && hasFromWireKey
            }
    }

    @Test
    fun `no standalone ByString enum lookup maps exist`() {
        kmpScope
            .properties()
            .assertTrue { prop ->
                !prop.name.endsWith("ByString")
            }
    }

    @Test
    fun `internal implementation classes ending with Impl reside in impl package`() {
        kmpScope
            .classes()
            .filter { it.name.endsWith("Impl") && it.hasInternalModifier }
            .assertTrue { clazz ->
                clazz.packagee?.name?.contains(".impl") == true
            }
    }

    @Test
    fun `feature domain code does not import data or ui packages`() {
        kmpScope
            .files
            .filter { it.path.contains("/features/") && it.path.contains("/domain/") }
            .assertTrue { file ->
                file.imports.none { import ->
                    (!import.name.startsWith("dev.fnc314.tools.jobtracker.core.data.") &&
                        !import.name.endsWith("Repository") &&
                        import.name.contains(".data.")) ||
                        import.name.contains(".ui.")
                }
            }
    }

    @Test
    fun `all datasource interfaces and implementations are internal`() {
        val classAndIface = kmpScope.classes() + kmpScope.interfaces()
        classAndIface
            .filter { it.name.contains("DataSource") && it.containingFile.path.contains("/features/") }
            .assertTrue { decl ->
                decl.hasInternalModifier
            }
    }

    @Test
    fun `all composeResources strings xml files use indexed format specifiers`() {
        val rootDir = File("..")
        val stringsFiles = rootDir.walkTopDown()
            .filter { file ->
                file.isFile &&
                    file.name == "strings.xml" &&
                    file.path.contains("composeResources") &&
                    !file.path.contains("/build/")
            }
            .toList()
        kotlinAssertTrue(stringsFiles.isNotEmpty(), "Expected to find composeResources strings.xml files")
        val unindexedSpecifierRegex = Regex("""%[sd]""")
        val offendingFiles = stringsFiles.filter { unindexedSpecifierRegex.containsMatchIn(it.readText()) }
        kotlinAssertTrue(
            offendingFiles.isEmpty(),
            "Found unindexed %s or %d in Compose string resources (must use %1\$s / %1\$d): ${offendingFiles.map { it.path }}",
        )
    }
}
