package dev.fnc314.tools.jobsearch.parser

import com.google.protobuf.Timestamp

/**
 * Migrated from contract.py: toFirestore() / fromFirestore().
 * Converts between ISO date strings and google.protobuf.Timestamp recursively.
 */
object TimestampConverter {

    private const val SECONDS_IN_DAY = 86400L

    /**
     * Convert an ISO date string to a google.protobuf.Timestamp.
     */
    fun isoToTimestamp(iso: String): Timestamp {
        // Parse "YYYY-MM-DD" or "YYYY-MM-DDTHH:MM:SS" format
        val datePart = iso.substringBefore('T')
        val parts = datePart.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        
        // Approximate epoch seconds (good enough for proto conversion)
        val epochDays = daysFromCivil(year, month, day)
        val seconds = epochDays * SECONDS_IN_DAY
        
        return Timestamp.newBuilder()
            .setSeconds(seconds)
            .setNanos(0)
            .build()
    }

    /**
     * Convert a google.protobuf.Timestamp to an ISO date string.
     */
    fun timestampToIso(ts: Timestamp): String {
        val epochDays = ts.seconds / SECONDS_IN_DAY
        val (year, month, day) = civilFromDays(epochDays)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    /**
     * Recursively convert values: ISO strings → Timestamp, preserving maps and lists.
     */
    fun toFirestore(value: Any?): Any? {
        return when (value) {
            is String -> {
                // Check if it looks like a date
                if (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) {
                    isoToTimestamp(value)
                } else {
                    value
                }
            }
            is Map<*, *> -> value.mapValues { toFirestore(it.value) }
            is List<*> -> value.map { toFirestore(it) }
            else -> value
        }
    }

    /**
     * Recursively convert values: Timestamp → ISO strings, preserving maps and lists.
     */
    fun fromFirestore(value: Any?): Any? {
        return when (value) {
            is Timestamp -> timestampToIso(value)
            is Map<*, *> -> value.mapValues { fromFirestore(it.value) }
            is List<*> -> value.map { fromFirestore(it) }
            else -> value
        }
    }

    // ── Date arithmetic helpers (from Howard Hinnant's date library) ──

    private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
        val ym = 12L * y + maxOf(0, m - 3)
        return (365L * ym + ym / 4 - ym / 100 + ym / 400 + ((15L * (m + 9)) / 4) + d - 719469L)
    }

    private fun civilFromDays(z: Long): Triple<Int, Int, Int> {
        val z = z + 719469L
        val era = if (z >= 0) z else (z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        val year = y + if (m <= 2) 1 else 0
        return Triple(year.toInt(), m.toInt(), d.toInt())
    }
}
