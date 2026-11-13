package dev.fnc314.tools.jobtracker.conventions

import java.util.UUID

/**
 * UUIDv7 generation and validation utilities.
 *
 * UUIDv7 carries a 48-bit millisecond timestamp in its high bits, so the textual
 * form sorts in creation order. Generated locally rather than delegated to a
 * service, because an identifier must never require a network round trip.
 */
public object UuidGenerator {

    /**
     * Canonical lowercase UUIDv7 pattern.
     * The version nibble is 7 and the variant nibble is RFC 4122.
     * A v4 UUID is a hard error rather than a silently unsortable record.
     */
    private val uuidV7Pattern = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
    )

    /**
     * Generate a local UUIDv7.
     *
     * Uses Java's built-in UUIDv7 support (available since Java 20).
     * Falls back to a manual construction for older JVMs.
     */
    public fun newUuid(): String {
        // UUIDv7 layout (RFC 9562):
        // MSB (64 bits):  timestamp[0..47] (48 bits) | ver[48..51] (4 bits = 7) | rand[52..63] (12 bits)
        // LSB (64 bits):  var[0..1] (2 bits = 10)   | rand[2..63] (62 bits)
        val millis = System.currentTimeMillis()
        val rand = java.security.SecureRandom()

        // Build MSB from components:
        // - timestamp (48 bits) at the top
        // - version 7 (4 bits) at bits 48-51
        // - random (12 bits) at the bottom
        val timestampPart = millis shl 16
        val versionPart = 7L shl 12
        val randomPart = rand.nextInt(4096).toLong()
        val msb = timestampPart or versionPart or randomPart

        // Build LSB: variant 10 (2 bits) at top, random (62 bits) at bottom
        val variantPart = Long.MIN_VALUE
        val lsbRandom = (rand.nextLong() shl 2) ushr 2
        val lsb = variantPart or lsbRandom

        return UUID(msb, lsb).toString()
    }

    /**
     * Returns true for the canonical lowercase UUIDv7 form.
     */
    public fun isUuid(value: String): Boolean {
        if (value.isBlank()) return false
        return uuidV7Pattern.matches(value)
    }

    /**
     * Recover the millisecond timestamp encoded in a UUIDv7's high 48 bits.
     *
     * @throws ContractError if the value is not a valid UUIDv7.
     */
    public fun uuidTimestamp(value: String): Long {
        if (!isUuid(value)) {
            throw ContractError("not a UUIDv7: $value")
        }
        val uuid = UUID.fromString(value)
        // UUIDv7 stores the 48-bit millisecond timestamp in the top 48 bits of MSB
        // Mask to 48 bits after shifting right by 16
        val millisMask = 281474976710655L // 0x0000FFFFFFFFFFFF
        return (uuid.mostSignificantBits ushr 16) and millisMask
    }
}
