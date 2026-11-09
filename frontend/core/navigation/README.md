# Module: `:frontend:core-navigation`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:frontend:core-navigation`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.navigation`
> - **Primary Contributions**: Multiplatform navigation abstractions (`NavKeyRoute`, `FeatureNavigationHook`, `TopLevelDestination`, `BackHandler`)
> - **Architectural Invariant**: Completely agnostic of individual feature destinations; feature UI modules register their routes via `NavKeyRoute` and `FeatureNavigationHook`.
> - **Key Build/Verification Command**: `./gradlew :frontend:core-navigation:compileCommonMainKotlinMetadata`

---

## 1. Module Overview
`:frontend:core-navigation` defines the core multiplatform navigation contracts, abstractions, and platform-specific back-handling logic. It enables a completely decoupled navigation architecture where features define their own type-safe routes and navigation contribution hooks without having `core` depend on any feature.

---

## 2. Contribution to the Larger Application
- **Upstream Consumers**: All feature UI modules (`features/*/ui`) and `apps-core`.
- **Downstream Dependencies**: `androidx.navigation:navigation-compose`, Kotlinx Serialization, Compose Runtime.
- **Architectural Role**: Features contribute destinations via `FeatureNavigationHook`, which `apps-core` consumes to assemble the root `NavHost`.

---

## 3. Package & Subproject Structure

```
core/navigation/src/
├── commonMain/kotlin/dev/fnc314/tools/jobtracker/core/navigation/
│   ├── NavKey.kt                   # NavKey & NavKeyRoute base interfaces
│   ├── FeatureNavigationHook.kt    # Functional interface for feature graph contributions
│   ├── TopLevelDestination.kt      # ABI-stable navigation rail / bar destination contract
│   └── BackHandler.kt              # expect fun BackHandler
├── androidMain/kotlin/             # Android BackHandler actual (delegating to activity-compose)
├── jvmMain/kotlin/                 # Desktop JVM BackHandler actual (no-op)
├── iosMain/kotlin/                 # iOS BackHandler actual (no-op)
├── jsMain/kotlin/                  # JS browser BackHandler actual (no-op)
└── wasmJsMain/kotlin/              # Wasm browser BackHandler actual (no-op)
```

---

## 4. Public API Surface & Significant Contributions
- `public interface NavKeyRoute : NavKey`: Base marker contract for type-safe destination routes.
- `public fun interface FeatureNavigationHook`: Functional interface with `operator fun invoke(navGraphBuilder: NavGraphBuilder, navController: NavHostController)`.
- `public interface TopLevelDestination`: ABI-stable interface with companion factory `from(...)` and internal data class implementation.
- `public expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)`: Multiplatform back handler.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :frontend:core-navigation:compileCommonMainKotlinMetadata` | Module | Validates expect/actual metadata across all platform targets |
| `./gradlew :frontend:core-navigation:assemble` | Module | Builds platform library artifacts |
| `mise run frontend:build` | Workspace | Builds full application hierarchy |
