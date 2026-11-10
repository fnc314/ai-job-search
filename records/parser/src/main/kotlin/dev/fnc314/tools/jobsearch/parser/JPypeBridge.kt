package dev.fnc314.tools.jobsearch.parser

/**
 * JPype-compatible API bridge exposing tracker parsing and projection facilities to Python and the curses TUI.
 *
 * All functions are annotated with [@JvmStatic] to allow seamless direct invocations from Python scripts:
 * ```python
 * from dev.fnc314.tools.jobsearch.parser import TrackerParser
 *
 * iso_date = TrackerParser.fromFirestore(timestamp_map)
 * normalized = TrackerParser.normalizeStatus("interviewing")
 * flat_row = TrackerParser.rowFromStore(app_dict, artifacts_list)
 * ```
 */
public object TrackerParser {

    /**
     * Converts a Firestore timestamp representation into an ISO-8601 formatted date/time string.
     *
     * Accepts a map containing `"seconds"` and `"nanos"` numeric keys, or a protobuf [com.google.protobuf.Timestamp] instance.
     *
     * @param value The raw Firestore timestamp representation or proto instance.
     * @return The formatted ISO-8601 string, or null if the input value was null or unparseable.
     * @see TimestampConverter.fromFirestore
     */
    @JvmStatic
    public fun fromFirestore(value: Any?): Any? {
        return TimestampConverter.fromFirestore(value)
    }

    /**
     * Converts an ISO-8601 date/time string into a Firestore timestamp map representation.
     *
     * @param value The ISO-8601 date string (e.g., `"2026-03-15T12:00:00Z"`).
     * @return A map containing `"seconds"` and `"nanos"` keys compatible with Firestore, or null if parsing fails.
     * @see TimestampConverter.toFirestore
     */
    @JvmStatic
    public fun toFirestore(value: Any?): Any? {
        return TimestampConverter.toFirestore(value)
    }

    /**
     * Normalizes a legacy or human-readable status string into its canonical namespaced identifier.
     *
     * For example, maps `"screening"` or `"applied"` to `"status:applied"`, and `"onsite"` to `"status:interview"`.
     *
     * @param raw The raw status string entered by the user or read from an archived record.
     * @return The canonical namespaced status identifier (e.g. `"status:interview"`).
     * @see StatusNormalizer.normalizeStatus
     */
    @JvmStatic
    public fun normalizeStatus(raw: String): String {
        return StatusNormalizer.normalizeStatus(raw)
    }

    /**
     * Computes derived status fields required by the legacy UI and reporting layers.
     *
     * Calculates `statusRank` (integer ordinal for sorting in lists) and `isOpen` (boolean flag
     * indicating whether an application is still actively in progress).
     *
     * @param status The canonical namespaced status string.
     * @return A map containing `"statusRank"` ([Int]) and `"isOpen"` ([Boolean]).
     * @see StatusNormalizer.deriveStatusFields
     */
    @JvmStatic
    public fun deriveStatusFields(status: String): Map<String, Any> {
        return StatusNormalizer.deriveStatusFields(status)
    }

    /**
     * Returns the complete lookup table mapping canonical status strings to their numerical sorting rank.
     *
     * @return An immutable map of status strings to integer priority ranks.
     * @see StatusNormalizer.statusRank
     */
    @JvmStatic
    public fun getStatusRank(): Map<String, Int> {
        return StatusNormalizer.statusRank
    }

    /**
     * Returns the set of terminal statuses for which no further state transitions are expected.
     *
     * Applications in a terminal status are considered closed.
     *
     * @return A set of terminal status string identifiers.
     * @see StatusNormalizer.terminalStatuses
     */
    @JvmStatic
    public fun getTerminalStatuses(): Set<String> {
        return StatusNormalizer.terminalStatuses
    }

    /**
     * Converts a namespaced status string into a human-readable legacy presentation label.
     *
     * @param value The namespaced status string (e.g., `"status:offer-declined"`).
     * @return A human-facing label (e.g., `"Offer Declined"`).
     * @see LegacyRowProjector.legacyStatus
     */
    @JvmStatic
    public fun legacyStatus(value: String?): String {
        return LegacyRowProjector.legacyStatus(value)
    }

    /**
     * Projects a nested Firestore application document into a flattened key-value dictionary.
     *
     * Primary projection entrypoint consumed by the terminal UI (TUI) and legacy tabular views.
     * Synthesizes derived properties such as `status_label`, `status_rank`, and `is_open`.
     *
     * @param appMap Nested Firestore document represented as a map.
     * @param artifactsList Associated collection of artifact documents.
     * @return Flattened row representation mapping column keys to string values.
     * @see LegacyRowProjector.rowFromStore
     */
    @JvmStatic
    @Suppress("UnusedParameter")
    public fun rowFromStore(
        appMap: Map<String, Any>,
        artifactsList: List<Any> = emptyList(),
    ): Map<String, String> {
        val row = mutableMapOf<String, String>()
        for ((key, value) in appMap) {
            row[key] = value.toString()
        }
        val status = row["status"] ?: "status:applied"
        row["status_label"] = LegacyRowProjector.legacyStatus(status)
        row["status_rank"] = StatusNormalizer.statusRank[status]?.toString() ?: "99"
        row["is_open"] = (status !in StatusNormalizer.terminalStatuses).toString()
        return row
    }

    /**
     * Returns the list of standard column keys expected by legacy CSV export and terminal table renderers.
     *
     * @return Ordered list of column key strings.
     * @see LegacyRowProjector.LEGACY_KEYS
     */
    @JvmStatic
    public fun getLegacyKeys(): List<String> {
        return LegacyRowProjector.LEGACY_KEYS
    }

    /**
     * Strips known tracking and telemetry query parameters from a URL.
     *
     * @param rawUrl The raw URL string.
     * @return Sanitized URL string, or null if input was null.
     * @see UrlSanitizer.cleanUrl
     */
    @JvmStatic
    public fun cleanUrl(rawUrl: String?): String? {
        return UrlSanitizer.cleanUrl(rawUrl)
    }
}
