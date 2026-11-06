# Architecture Plan: Canonical Enum Mapping & Self-Contained Relationships (Refactor Proposal)

**Status:** Draft / Proposed  
**Date:** 2026-11-05  
**Target:** ui/kmp (and records)  

---

## 1. Problem Statement & Architectural Concern

Throughout `ui/kmp` (and `records`), there are numerous standalone, manually declared dictionaries of the form `Map<String, Enum>` or `Map<Int, Enum>` (for instance, in standalone mapping files or parser utilities). 

### Current Drawbacks:
1. **Disconnected Domain Knowledge:** The relationship between an enum entry and its canonical string identifier (or wire format key) is separated from the enum declaration itself.
2. **Maintenance Overhead & Drift:** Adding a new enum variant requires updating both the enum definition and the separate map(s) in other files, risking omissions or incomplete mappings that fail silently at runtime.
3. **Redundant Boilerplate:** Hand-written `mapOf("key" to Enum.VALUE, ...)` duplicates information that should be intrinsic to the enum entries.

---

## 2. Proposed Architectural Pattern

### Principle: Enums House Their Own Relationships
Each enum representing domain concepts or wire contracts should declare its string identifier as a constructor property (e.g., `val wireKey: String` or `val id: String`). Lookups should be hosted directly in the enum's `companion object` and built dynamically from `entries`.

### Implementation Pattern:
```kotlin
public enum class TrackerStatus(public val rawValue: String) {
    DRAFTING("status:drafting"),
    APPLIED("status:applied"),
    INTERVIEW("status:interview"),
    OFFER("status:offer"),
    REJECTED("status:rejected"),
    ARCHIVED("status:archived"),
    WITHDRAWN("status:withdrawn");

    public companion object {
        /**
         * Fast O(1) lookup map derived from [entries].
         */
        public val byRawValue: Map<String, TrackerStatus> = entries.associateBy { it.rawValue }

        /**
         * Safe lookup with fallback.
         */
        public fun fromRawValueOrNull(raw: String?): TrackerStatus? = byRawValue[raw]
    }
}
```

### Generalized Inline Helper (Zero-Boilerplate Companion Utility):
To avoid repeating `.associateBy` across every enum, a reusable inline utility can be provided in `core:data`:

```kotlin
public inline fun <reified E : Enum<E>> enumLookupMap(
    crossinline keySelector: (E) -> String
): Map<String, E> = enumValues<E>().associateBy(keySelector)
```

Or with an accumulator/reduce optimization:
```kotlin
public inline fun <reified E : Enum<E>> enumLookupMapFast(
    crossinline keySelector: (E) -> String
): Map<String, E> = enumValues<E>().fold(HashMap()) { acc, entry ->
    acc.apply { put(keySelector(entry), entry) }
}
```

---

## 3. Tooling & Enforcement Opportunities (Konsist & Detekt)

1. **Konsist Architectural Assertion:**
   - Konsist can inspect all enum classes and verify that:
     - Enums containing string mappings expose a `companion object` with a lookup function/map.
     - No external top-level `val *ByString: Map<String, SomeEnum>` exist outside the enum's own file or companion object.
2. **Custom Detekt Rule (`ForbiddenStandaloneEnumMap`):**
   - Flag any `Map<String, E>` where `E` is an enum type if declared outside of `E`'s companion object or class body.

---

## 4. Next Steps
- This proposal is captured for a dedicated subsequent refactoring pass after Detekt and Konsist baseline integration is complete.
