# Architecture Plan: Canonical Enum Mapping & Wire Contracts

**Status:** Completed  
**Date:** 2026-11-05  
**Target:** `records` and `ui/kmp` (cross-subsystem)  
**Evaluates:** [`172809_DRAFT-code-refactor.md`](../../ui/kmp/.agents/plans/2026-11-05/172809_DRAFT-code-refactor.md)  
**Schema Authority:** [`tracker.schema.json`](../../tracker.schema.json)  

---

## 1. Executive Summary & Evaluation of Draft Refactor

### Evaluation of `172809_DRAFT-code-refactor.md`
The draft proposal correctly identifies a foundational architectural smell:
1. **Disconnected Domain Knowledge:** String wire keys (e.g., `"status:applied"`, `"channel:workday"`) and integer wire tags (`enum.number: Int`) are detached from the types, scattered in external maps (`EnumMapping.kt` in `records/parser`) or linearly scanned via repetitive `entries.firstOrNull { ... }` loops in `ui/kmp:core:data`.
2. **Runtime Performance Degenerations:** In `ui/kmp:core:data`, functions like `TrackerStatus.fromFirestore(raw)` and `ApplicationChannel.fromFirestore(raw)` perform unindexed O(N) linear scans with case-insensitive comparisons and string manipulation (`removePrefix(...)`) on every lookup. In high-frequency UI rendering (e.g. `LazyColumn` of 80+ applications), this causes unnecessary object allocations and CPU cycles.
3. **Room Database Conversion Hazards:** Room's `@ColumnTypeConverter` in `DatabaseConverters.kt` uses `Enum.valueOf(it)` which succeeds only on exact enum `.name` strings (`"APPLIED"`), but throws `IllegalArgumentException` on namespaced Firestore strings (`"status:applied"`), defaulting to fallback values (`DRAFTING`) without surfacing deserialization failures.
4. **Subsystem Drift & Naming Inconsistencies:** The audit revealed subtle schema-to-proto discrepancies between `tracker.schema.json` and `records/proto/src/main/proto/tracker/v1/enums.proto`:
   - Schema defines `actor:gmail-sync`, `proposal:gmail-sync`, `sync:gmail-collect`, `derivedFrom: "gmail"`.
   - Protobuf and `EnumMapping.kt` define `actor:email-sync`, `proposal:email-sync`, `sync:email-collect`, `derivedFrom: "email"`.
   - Schema defines `status:lead` as rank 0, while Protobuf starts at `DRAFTING = 1` without `LEAD`.

---

## 2. Sealed Interface Open Enums & Associated Values (Swift-Style)

### Swift-Style Associated Values in Kotlin
In Swift, enums support associated values (e.g. `case known(Status)`, `case custom(String)`). In Kotlin, traditional `enum class` entries are fixed singletons compiled into bytecode and cannot carry dynamic payloads for unrecognized or custom wire values.

To achieve **zero data loss** and **open extensibility** when deserializing from Firestore or Room, each domain enum hierarchy is modeled as a **Sealed Interface**:
1. **`Known` (`enum class`)**: Implements the sealed interface for all canonical/known schema values, embedding the `wireKey: String`, `displayName: String`, and integer `wireTag: Int` directly within each entry.
2. **`Unrecognized` (`data class`)**: Implements the sealed interface for unexpected, future, or custom strings encountered in the wild, preserving the exact raw `wireKey` payload without failing or forcing an incorrect fallback.
3. **Explicit Fallback / Default**: Every hierarchy defines an explicit default (`Known.UNSPECIFIED` or `Known.DRAFTING`), ensuring that null/blank values resolve cleanly while non-null unknown strings produce `Unrecognized(raw)`.

```kotlin
public sealed interface TrackerStatus : WireContract {
    public val wireKey: String
    public val displayName: String

    public enum class Known(
        override val wireKey: String,
        override val displayName: String,
        public val wireTag: Int = -1
    ) : TrackerStatus {
        LEAD("status:lead", "Lead", 0),
        DRAFTING("status:drafting", "Drafting", 1),
        APPLIED("status:applied", "Applied", 2),
        INTERVIEW("status:interview", "Interview", 3),
        OFFER_EXPECTED("status:offer", "Offer Expected", 4),
        HIRED("status:hired", "Hired", 5),
        REJECTED("status:rejected", "Rejected", 6),
        NO_RESPONSE("status:no-response", "No Response", 7),
        OFFER_DECLINED("status:offer-declined", "Offer Declined", 8),
        APPLICATION_WITHDRAWN("status:withdrawn", "Application Withdrawn", 9),
        UNSPECIFIED("status:unspecified", "Unspecified", -1);
    }

    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerStatus {
        override val displayName: String get() = "Unknown ($wireKey)"
    }

    public companion object {
        public val DEFAULT: TrackerStatus = Known.UNSPECIFIED
        
        // Fast O(1) direct lookup map built via stdlib-derived extension on Known.entries
        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

        public fun fromWireKey(raw: String?): TrackerStatus {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }
    }
}
```

---

## 3. Comprehensive Database & Codebase Enum Catalog

The repository currently maintains 15 canonical enums plus 2 inline sub-enums across Firestore, Protobuf, Room SQLite, and Multiplatform Domain models. Every hierarchy provides an explicit default and an `Unrecognized` carrier:

| # | Enum Identifier | Firestore Wire Namespace (`String`) | Protobuf Type & Wire Number (`Int`) | Room Storage Type | Domain Model (`ui/kmp`) | Fallback Default |
| :-: | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `TrackerStatus` | `"status:*"` (10 variants: `lead`, `drafting`, `applied`, `interview`, `offer`, `hired`, `rejected`, `no-response`, `offer-declined`, `withdrawn`) | `tracker.v1.TrackerStatus` (1..9) | SQLite `TEXT` (wireKey) | `TrackerStatus` (Sealed) | `Known.UNSPECIFIED` / `Known.DRAFTING` |
| **2** | `TrackerEventType` | `"event:*"` (21 variants: `applied`, `acknowledged`, `recruiter-screen`, `interview-scheduled`, etc.) | `tracker.v1.TrackerEventType` (1..20) | SQLite `TEXT` (wireKey) | `TrackerEventType` (Sealed) | `Known.UNSPECIFIED` |
| **3** | `ApplicationChannel` | `"channel:*"` (12 variants: `greenhouse`, `workday`, `lever`, `ashby`, `smartrecruiters`, `linkedin`, `referral`, `direct`, `other`, `outreach:linkedin`, `outreach:email`, `outreach:referral`) | `tracker.v1.ApplicationChannel` (1..9) | SQLite `TEXT` (wireKey) | `ApplicationChannel` (Sealed) | `Known.UNSPECIFIED` |
| **4** | `TrackerArtifactKind`| `"artifact:*"` (5 variants: `cv`, `cover-letter`, `outcome`, `posting`, `assessment`) | `tracker.v1.TrackerArtifactKind` (1..5) | SQLite `TEXT` (wireKey) | `TrackerArtifactKind` (Sealed) | `Known.UNSPECIFIED` |
| **5** | `WorkMode` | `"work-mode:*"` (3 variants: `onsite`, `hybrid`, `remote`) | `tracker.v1.WorkMode` (1..3) | SQLite `TEXT` (wireKey) | `WorkMode` (Sealed) | `Known.UNSPECIFIED` |
| **6** | `EventActor` | `"actor:*"` (5 variants: `user`, `agent`, `gmail-sync`, `import`, `scheduler`) | `tracker.v1.EventActor` (1..5) | SQLite `TEXT` (wireKey) | `EventActor` (Sealed) | `Known.UNSPECIFIED` |
| **7** | `TrackerEditState` | `"proposal:*"` (4 variants: `pending`, `approved`, `rejected`, `expired`) | `tracker.v1.TrackerEditState` (1..4) | SQLite `TEXT` (wireKey) | `TrackerEditState` (Sealed) | `Known.UNSPECIFIED` |
| **8** | `TrackerEditKind` | `"proposal:*"` (3 variants: `gmail-sync`, `sheet-sync`, `lead`) | `tracker.v1.TrackerEditKind` (1..2) | SQLite `TEXT` (wireKey) | `TrackerEditKind` (Sealed) | `Known.UNSPECIFIED` |
| **9** | `Trigger` | `"trigger:*"` (4 variants: `cron`, `application-created`, `application-updated`, `manual`) | `tracker.v1.Trigger` (1..4) | SQLite `TEXT` (wireKey) | `Trigger` (Sealed) | `Known.UNSPECIFIED` |
| **10** | `SyncKind` | `"sync:*"` (1 variant: `gmail-collect` / `email-collect`) | `tracker.v1.SyncKind` (1) | SQLite `TEXT` (wireKey) | `SyncKind` (Sealed) | `Known.UNSPECIFIED` |
| **11** | `SyncState` | `"sync:*"` (4 variants: `pending`, `claimed`, `done`, `failed`) | `tracker.v1.SyncState` (1..4) | SQLite `TEXT` (wireKey) | `SyncState` (Sealed) | `Known.UNSPECIFIED` |
| **12** | `Confidence` | `"high"`, `"medium"`, `"low"` (bare strings) | `tracker.v1.Confidence` (1..3) | SQLite `TEXT` (wireKey) | `Confidence` (Sealed) | `Known.UNSPECIFIED` |
| **13** | `DerivedFrom` | `"manual"`, `"notes-regex"`, `"gmail"`, `"csv-import"` | `tracker.v1.DerivedFrom` (1..4) | SQLite `TEXT` (wireKey) | `DerivedFrom` (Sealed) | `Known.UNSPECIFIED` |
| **14** | `Region` | `"PIT"`, `"NYC"`, `"SF"`, `"LA"`, `"DC"`, `"REMOTE-US"` | `tracker.v1.Region` (1..6) | SQLite `TEXT` (wireKey) | `Region` (Sealed) | `Known.UNSPECIFIED` |
| **15** | `Tag` | Controlled tags (19 variants: `relocation:*`, `compensation:*`, `fit:*`, `process:*`) | `tracker.v1.Tag` (1..18) | SQLite `TEXT` (wireKey list) | `Tag` (Sealed) | `Known.UNSPECIFIED` |
| **16** | `CompPeriod` | `"year"`, `"month"`, `"hour"` (JSON Schema sub-enum) | N/A (string field in proto) | SQLite `TEXT` | `CompPeriod` (Sealed) | `Known.YEAR` |
| **17** | `CompVsTarget` | `"below"`, `"at"`, `"above"` (JSON Schema sub-enum) | N/A (string field in proto) | SQLite `TEXT` | `CompVsTarget` (Sealed) | `Known.AT` |

---

## 4. Stdlib-Derived Extension Functions: `toWireLookupMap`

To limit superfluous global namespace pollution and leverage idiomatic Kotlin standard library constructs, the lookup builder is defined as an `internal` extension function on `Iterable<E>` using `buildMap`:

### Method 1: Canonical Wire & Name Lookup (Zero-Argument Overload)
Used by standard enums where every entry indexes its canonical `wireKey.lowercase()` and `name.lowercase()`:

```kotlin
package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Builds an immutable O(1) lookup map indexing entries by their canonical [WireContract.wireKey]
 * and enum [name].
 */
internal inline fun <E> Iterable<E>.toWireLookupMap(): Map<String, E>
    where E : Enum<E>, E : WireContract = buildMap {
        for (entry in this@toWireLookupMap) {
            put(entry.wireKey.lowercase(), entry)
            put(entry.name.lowercase(), entry)
        }
    }
```

### Method 2: Multi-Key Alias Lookup (Parameterized Overload)
Used when an enum requires backward-compatible aliases or alternative forms (e.g. `ApplicationChannel` accepting un-namespaced `"greenhouse"` or `"workday"`):

```kotlin
/**
 * Builds an immutable O(1) lookup map indexing entries by canonical [WireContract.wireKey],
 * enum [name], and additional custom aliases provided by [aliasSelector].
 */
internal inline fun <E> Iterable<E>.toWireLookupMap(
    crossinline aliasSelector: (E) -> List<String>
): Map<String, E> where E : Enum<E>, E : WireContract = buildMap {
    for (entry in this@toWireLookupMap) {
        put(entry.wireKey.lowercase(), entry)
        put(entry.name.lowercase(), entry)
        for (alias in aliasSelector(entry)) {
            put(alias.lowercase(), entry)
        }
    }
}
```

### Usage in Enum Companion Objects:
```kotlin
// In standard enums:
private val lookup: Map<String, Known> = Known.entries.toWireLookupMap()

// In enums with legacy aliases:
private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
    listOf(entry.wireKey.removePrefix("channel:"))
}
```

---

## 5. Room SQLite Converter Modernization (`DatabaseConverters.kt`)

Room converters serialize directly to `wireKey` and deserialize via `fromWireKey`:

```kotlin
@ColumnTypeConverter
public fun fromTrackerStatus(status: TrackerStatus?): String? = status?.wireKey

@ColumnTypeConverter
public fun toTrackerStatus(value: String?): TrackerStatus =
    TrackerStatus.fromWireKey(value)

@ColumnTypeConverter
public fun fromApplicationChannel(channel: ApplicationChannel?): String? = channel?.wireKey

@ColumnTypeConverter
public fun toApplicationChannel(value: String?): ApplicationChannel =
    ApplicationChannel.fromWireKey(value)
```

**Backward Compatibility Guarantee:**
Because `toWireLookupMap()` indexes both `.wireKey` (`"status:applied"`) and `.name` (`"APPLIED"`), existing SQLite database rows holding legacy enum `.name` strings deserialize seamlessly with zero migration errors.

---

## 6. Elimination of `EnumParsing.kt`

Per user directive, `EnumParsing.kt` is **NOT** deprecated — it will be **COMPLETELY ELIMINATED**.
- Call sites in data sources, repositories, and UI will call `TrackerStatus.fromWireKey(raw)` directly on the canonical companion object.
- Removes 108 lines of redundant dispatch redirection and indirection.

---

## 7. Elimination of `EnumMapping.kt` & Direct Proto Alignment

`EnumMapping.kt` in `records/parser` is a 347-line monolithic static dictionary housing 15 disconnected `Map<String, Enum>` and `Map<Int, String>` tables. It serves no unique purpose that embedding values into the enums cannot handle better, and is the primary source of domain drift between `records` and `tracker.schema.json`.

Per user directive, **`EnumMapping.kt` will be COMPLETELY ELIMINATED**:
1. **Self-Contained Proto Extensions:**
   Each Protobuf enum in `records` will have its wire representation defined directly via per-enum extension properties and lookup functions (e.g. `TrackerStatus.wireKey` and `TrackerStatus.fromWireKey(raw)`), or mapped directly through the canonical domain sealed types via integer `wireTag`.
2. **Exhaustive Type-Safety:**
   Direct mapping eliminates external dictionary maps and ensures compile-time exhaustiveness (`when (this)`).
3. **Delete Monolith:**
   Delete `records/parser/src/main/kotlin/dev/fnc314/tools/jobsearch/parser/EnumMapping.kt` and update all caller call sites to use the direct enum properties.

---

## 8. Phased Implementation Roadmap

### Phase 1: Sealed Interface & Open Enum Refactor (`ui/kmp:core:data`)
- [x] Create `WireContract.kt` and `toWireLookupMap` extension functions in `core:data`.
- [x] Refactor all 11 existing domain enums to sealed interfaces with `Known` enum and `Unrecognized` data class.
- [x] Implement missing domain sealed types: `SyncKind`, `SyncState`, `Region`, `Tag`, `CompPeriod`, `CompVsTarget`.
- [x] **ELIMINATE** `EnumParsing.kt` completely and update all call sites.

### Phase 2: Room SQLite Converter Modernization
- [x] Update `DatabaseConverters.kt` in `core:data` to serialize enums to `wireKey` and deserialize via `fromWireKey`.
- [x] Run Room database unit tests verifying persistence and round-trip fidelity for both known and unrecognized variants.

### Phase 3: Elimination of `EnumMapping.kt` & Records Alignment (`records:parser`)
- [x] Reconcile schema naming (`gmail-sync` vs `email-sync`, `LEAD` status).
- [x] **ELIMINATE** `EnumMapping.kt` entirely; replace with direct self-contained proto extension properties or domain mapping.
- [x] Update any tests or consumers to invoke `wireKey` and `fromWireKey` directly.

### Phase 4: Architectural Enforcement (Konsist & Detekt)
- [x] Add Konsist architecture test:
  - Asserts all types implementing `WireContract` declare a companion object with `fromWireKey`.
  - Asserts zero top-level `val *ByString: Map<String, *>` exist anywhere in the repository.

---

## 9. Verification & Testing Checklist

- [x] `./gradlew compileCommonMainKotlinMetadata` passes cleanly with zero warnings under Kotlin Explicit API Mode.
- [x] Unit tests verify round-trip fidelity for both known entries and `Unrecognized(raw)` variants.
- [x] Room round-trip tests confirm legacy enum `.name` strings and canonical `wireKey` strings deserialize identically.
- [x] `./gradlew :records-parser:test` executes and passes 100%.
- [x] Module `README.md` files updated in `core/data/README.md` and `records/parser/README.md`.