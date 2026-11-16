package dev.fnc314.aijobtracker.architecture

import com.lemonappdev.konsist.api.verify.assertTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue as kotlinAssertTrue
import com.lemonappdev.konsist.api.provider.modifier.KoActualModifierProvider
import com.lemonappdev.konsist.api.provider.modifier.KoOverrideModifierProvider

/**
 * Global architecture test suite for `frontend` multiplatform workspace.
 *
 * Verifies critical structural invariants defined in `frontend/AGENTS.md`:
 * 1. Zero rogue `Color()` calls outside of `Color.kt`.
 * 2. Option A UI Decoupling: UI feature modules depend on `core.widgets` and never import `core.designsystem` directly.
 * 3. All ViewModel declarations adhere to naming conventions.
 * 4. All public declarations contain comprehensive KDoc documentation.
 */
class FrontendArchitectureTest : BaseArchitectureTest() {

    @Test
    fun `no files outside of core design system call Color constructor`() {
        frontendScope
            .files
            .filterNot { it.path.contains("/core/design-system/") || it.path.contains("/build/") }
            .assertTrue { file ->
                !file.text.contains(Regex("""\bColor\(0x[0-9a-fA-F]+|\bColor\([0-9]+"""))
            }
    }

    @Test
    fun `view models reside in ui package and end with ViewModel suffix`() {
        frontendScope
            .classes()
            .filter { it.hasParentWithName("ViewModel") }
            .assertTrue { clazz ->
                clazz.name.endsWith("ViewModel") && (clazz.packagee?.name?.contains(".ui") == true)
            }
    }

    @Test
    fun `all use cases have execute or invoke method`() {
        frontendScope
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
        frontendScope
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
        frontendScope
            .properties()
            .assertTrue { prop ->
                !prop.name.endsWith("ByString")
            }
    }

    @Test
    fun `internal implementation classes ending with Impl reside in impl package`() {
        frontendScope
            .classes()
            .filter { it.name.endsWith("Impl") && it.hasInternalModifier }
            .assertTrue { clazz ->
                clazz.packagee?.name?.contains(".impl") == true
            }
    }

    @Test
    fun `feature domain code does not import data or ui packages`() {
        frontendScope
            .files
            .filter { it.path.contains("/features/") && it.path.contains("/domain/") }
            .assertTrue { file ->
                file.imports.none { import ->
                    (!import.name.startsWith("dev.fnc314.aijobtracker.frontend.core.data.") &&
                        !import.name.endsWith("Repository") &&
                        import.name.contains(".data.")) ||
                        import.name.contains(".ui.")
                }
            }
    }

    @Test
    fun `all datasource interfaces and implementations are internal`() {
        val classAndIface = frontendScope.classes() + frontendScope.interfaces()
        classAndIface
            .filter { it.name.contains("DataSource") && it.containingFile.path.contains("/features/") }
            .assertTrue { decl ->
                decl.hasInternalModifier
            }
    }

    @Test
    fun `public declarations do not name a tool`() {
        val baseline = File(rootDir, "docs/interface-naming-baseline.txt")
            .readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .toSet()
        val substrings = listOf("firestore", "firebase", "gmail", "composio", "kermit", "jpype")
        val tokens = setOf("gcs", "room", "ktor", "notion")
        fun namesTool(name: String): Boolean {
            if (substrings.any { name.lowercase().contains(it) }) return true
            return Regex("[A-Z]+(?=[A-Z][a-z])|[A-Z]?[a-z]+|[A-Z]+|\\d+").findAll(name).any { it.value.lowercase() in tokens }
        }
        fun relPath(path: String): String = File(path).normalize().relativeTo(rootDir).path
        val declarations = frontendScope.classes() + frontendScope.interfaces() + frontendScope.objects() + frontendScope.functions() + frontendScope.properties()
        val violations = declarations
            .filter { it.hasPublicModifier }
            .filterNot { it.containingFile.path.let { p -> p.contains("KmpInspector") || p.contains("/impl/") || p.contains("/build/") || p.contains("/bin/") || p.contains("Test/") } }
            .filter { namesTool(it.name) }
            .map { "${relPath(it.containingFile.path)}:${it.name}" }
            .filterNot { it in baseline }
        kotlinAssertTrue(
            violations.isEmpty(),
            "Public declarations must be named for the interface, not the tool (Remote/Local/Network): $violations",
        )
    }

    @Test
    fun `all composeResources strings xml files use indexed format specifiers`() {
        val frontendDir = File(rootDir, "frontend")
        val stringsFiles = frontendDir.walkTopDown()
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

    @Test
    fun `all public declarations in frontend have KDoc`() {
        val decls = frontendScope.classes() +
            frontendScope.interfaces() +
            frontendScope.objects() +
            frontendScope.functions() +
            frontendScope.properties()

        val missing = decls
            .filter { it.hasPublicModifier }
            .filterNot {
                it.containingFile.path.contains("/test/") ||
                    it.containingFile.path.contains("/jvmTest/") ||
                    it.containingFile.path.contains("/androidTest/") ||
                    it.containingFile.path.contains("/commonTest/") ||
                    it.containingFile.path.contains("/build/") ||
                    it.containingFile.path.contains("/bin/")
            }
            .filterNot { (it as? KoActualModifierProvider)?.hasActualModifier == true }
            .filterNot { (it as? KoOverrideModifierProvider)?.hasOverrideModifier == true }
            .assertTrue { it.hasKDoc }
    }
}
