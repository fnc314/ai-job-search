package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Strongly-typed representation of an RFC 4122 Variant UUIDv7 (time-ordered 128-bit identifier).
 *
 * Wraps [kotlin.uuid.Uuid] with zero memory overhead at runtime via `@JvmInline value class`.
 *
 * ### Architectural Justification:
 * 1. **Zero Runtime Overhead**: As an `@JvmInline value class`, it compiles down to the underlying [Uuid]
 *    (or primitive fields) on JVM/Android/iOS/Wasm platforms without additional heap allocations.
 * 2. **Type Safety & Domain Invariance**: Prevents accidental confusion between human-readable slug
 *    identifiers (e.g. `2026-03-pnc-lead`), foreign key references, and canonical immutable entity UUIDv7s.
 * 3. **Time-Ordered Monotonicity**: UUIDv7 embeds a millisecond-precision Unix epoch timestamp in its
 *    most significant 48 bits, providing naturally sorted, index-friendly primary keys in SQLite/Room.
 * 4. **Schema v7 Conformance**: Complies with the `#/$defs/uuidV7` JSON schema invariant defined in
 *    `tracker.schema.json` and `application.proto`.
 */
@OptIn(ExperimentalUuidApi::class)
@JvmInline
public value class Uuid7(
    public val value: Uuid,
) : Comparable<Uuid7> {

    /**
     * Formats this UUIDv7 into the standard 8-4-4-4-12 canonical lower-case hexadecimal string.
     */
    override fun toString(): String = value.toString()

    /**
     * Compares two UUIDv7 identifiers by their underlying natural 128-bit time-ordered value.
     */
    override fun compareTo(other: Uuid7): Int = value.compareTo(other.value)

    public companion object {
        /**
         * Generates a new cryptographically secure, time-ordered monotonic UUIDv7.
         */
        public fun generate(): Uuid7 = Uuid7(Uuid.generateV7())

        /**
         * Parses a 36-character hexadecimal RFC 4122 string into a [Uuid7].
         *
         * @throws IllegalArgumentException if [value] is not a valid UUID format.
         */
        public fun parse(value: String): Uuid7 = Uuid7(Uuid.parse(value))

        /**
         * Parses a 36-character hexadecimal RFC 4122 string into a [Uuid7], or returns `null`
         * if the string cannot be parsed as a valid UUID.
         */
        public fun parseOrNull(value: String?): Uuid7? =
            if (value == null) null else runCatching { Uuid7(Uuid.parse(value)) }.getOrNull()
    }
}
