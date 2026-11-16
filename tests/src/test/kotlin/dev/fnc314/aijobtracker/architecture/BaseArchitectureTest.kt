package dev.fnc314.aijobtracker.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import java.io.File

/**
 * Base fixture for Konsist architecture tests, resolving the workspace root
 * and providing scoped access across all monorepo subsystems.
 */
abstract class BaseArchitectureTest {

    protected val rootDir: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "mise.toml").exists() }

    protected val recordsScope: KoScope by lazy {
        Konsist.scopeFromDirectory("records")
    }

    protected val backendScope: KoScope by lazy {
        Konsist.scopeFromDirectory("backend")
    }

    protected val frontendScope: KoScope by lazy {
        Konsist.scopeFromDirectory("frontend")
    }

    protected val gradleToolsScope: KoScope by lazy {
        Konsist.scopeFromDirectory("gradle/tools")
    }
}
