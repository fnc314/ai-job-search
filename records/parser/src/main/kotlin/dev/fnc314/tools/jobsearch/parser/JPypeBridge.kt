package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.JobApplication
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifact
import com.google.protobuf.Timestamp

/**
 * JPype-compatible API surface for the TUI.
 * All functions are @JvmStatic so Python can call them as:
 *   TrackerParser.fromFirestore(value)
 *   TrackerParser.normalizeStatus(raw)
 *   TrackerParser.rowFromStore(appMap, artifactsList)
 */
object TrackerParser {

    /**
     * Convert a Firestore Timestamp (as Map) to ISO date string.
     * Accepts a Map with "seconds" and "nanos" keys, or a raw Timestamp object.
     */
    @JvmStatic
    fun fromFirestore(value: Any?): Any? {
        return TimestampConverter.fromFirestore(value)
    }

    /**
     * Convert an ISO date string to a Firestore Timestamp (as Map).
     */
    @JvmStatic
    fun toFirestore(value: Any?): Any? {
        return TimestampConverter.toFirestore(value)
    }

    /**
     * Normalize a legacy/human status string to a namespaced status value.
     */
    @JvmStatic
    fun normalizeStatus(raw: String): String {
        return StatusNormalizer.normalizeStatus(raw)
    }

    /**
     * Compute derived status fields: statusRank and isOpen.
     */
    @JvmStatic
    fun deriveStatusFields(status: String): Map<String, Any> {
        return StatusNormalizer.deriveStatusFields(status)
    }

    /**
     * Get the status ranking map.
     */
    @JvmStatic
    fun getStatusRank(): Map<String, Int> {
        return StatusNormalizer.statusRank
    }

    /**
     * Get terminal status values.
     */
    @JvmStatic
    fun getTerminalStatuses(): Set<String> {
        return StatusNormalizer.terminalStatuses
    }

    /**
     * Convert namespaced status string to legacy label.
     */
    @JvmStatic
    fun legacyStatus(value: String?): String {
        return LegacyRowProjector.legacyStatus(value)
    }

    /**
     * Project a JobApplication (as Map) to the legacy flat row shape.
     * This is the main entry point for the TUI.
     *
     * @param appMap Firestore document as a Map<String, Any>
     * @param artifactsList List of artifact Maps
     * @return Flat row Map<String, String>
     */
    @JvmStatic
    fun rowFromStore(
        appMap: Map<String, Any>,
        artifactsList: List<Any> = emptyList(),
    ): Map<String, String> {
        // For now, return a basic projection from the raw map.
        // Full proto-based projection requires building JobApplication from the map,
        // which is done in the KMP client side.
        val row = mutableMapOf<String, String>()
        for ((key, value) in appMap) {
            if (value != null) {
                row[key] = value.toString()
            }
        }
        // Add derived fields
        val status = row["status"] ?: "status:applied"
        row["status_label"] = LegacyRowProjector.legacyStatus(status)
        row["status_rank"] = StatusNormalizer.statusRank[status]?.toString() ?: "99"
        row["is_open"] = (status !in StatusNormalizer.terminalStatuses).toString()
        return row
    }

    /**
     * Get the list of legacy column keys.
     */
    @JvmStatic
    fun getLegacyKeys(): List<String> {
        return LegacyRowProjector.LEGACY_KEYS
    }
}
