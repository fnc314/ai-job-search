# Module: `:core-network`

> [!NOTE]
> **Quick Facts for AI Agents & Developers**
> - **Subproject**: `:core-network`
> - **Package Root**: `dev.fnc314.tools.jobtracker.core.network`
> - **Primary Contributions**: Multiplatform Ktor engine abstraction (`createPlatformHttpClient`), preconfigured JSON serialization client (`createJsonHttpClient`)
> - **Key Build/Verification Command**: `./gradlew :core-network:compileCommonMainKotlinMetadata`

---

## 1. Module Overview
`:core-network` encapsulates low-level multiplatform HTTP networking for the application. It configures platform-specific Ktor client engines, JSON content negotiation, timeout policies, and logging so upstream repositories can interact with remote REST/Cloud endpoints uniformly.

---

## 2. Contribution to the Larger Application
This module abstracts HTTP transport differences across operating systems:
- **Upstream Consumers**: `core:data` (for signed-URL Cloud Storage artifact retrieval and cloud APIs), `apps:core`.
- **Downstream Dependencies**: Ktor Client Core, Content Negotiation, Serialization, Logging.
- **Platform Engine Bindings**:
  - Android: `io.ktor:ktor-client-okhttp`
  - JVM (Desktop): `io.ktor:ktor-client-cio`
  - iOS (Darwin): `io.ktor:ktor-client-darwin`
  - Web (JS / Wasm): `io.ktor:ktor-client-js`

---

## 3. Package & Subproject Structure

```
core/network/src/
├── commonMain/kotlin/dev/fnc314/tools/jobtracker/core/network/
│   └── NetworkClient.kt            # Expect declaration for createPlatformHttpClient and factory createJsonHttpClient
├── androidMain/kotlin/             # OkHttp engine actual implementation
├── jvmMain/kotlin/                 # CIO engine actual implementation
├── iosMain/kotlin/                 # Darwin engine actual implementation
├── jsMain/kotlin/                  # JS Fetch engine actual implementation
└── wasmJsMain/kotlin/              # Wasm JS Fetch engine actual implementation
```

---

## 4. Public API Surface & Significant Contributions

- `public fun createJsonHttpClient(): HttpClient`: Returns a preconfigured Ktor `HttpClient` with:
  - JSON Content Negotiation using `kotlinx.serialization.json.Json` configured with `ignoreUnknownKeys = true` and `isLenient = true`.
  - Configured request timeouts (15s connect, 30s request).
  - Built-in logging enabled for non-release builds.
- `public expect fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient`: Factory function implemented natively by each platform engine.

---

## 5. Key Gradle & Mise Tasks

| Task | Scope | Purpose |
| :--- | :--- | :--- |
| `./gradlew :core-network:compileCommonMainKotlinMetadata` | Module | Validates expect/actual metadata across all platform targets |
| `./gradlew :core-network:assemble` | Module | Builds platform library artifacts |
| `mise run ui:kmp:build` | Workspace | Builds full application hierarchy |

---

## 6. Deep-Link & Navigation Integration
`:core-network` is a low-level network transport library with no navigation or deep-link involvement.

---

## 7. Convention Plugins & Dependencies

### Applied Convention Plugins
- `local.kotlin.multiplatform`: Multi-target toolchain.
- `local.android.kmp.library`: AGP Multiplatform Library setup.
- `local.dependencies.ktor`: Ktor client core and content-negotiation bundles.
- `local.dependencies.serialization`: JSON serialization support.
