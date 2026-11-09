package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Lifecycle states of a job application tracked within the system.
 *
 * Implemented as a sealed interface hierarchy supporting Swift-style associated values:
 * [Known] encapsulates all compile-time closed-vocabulary funnel states, while [Unrecognized]
 * captures arbitrary or forward-compatible wire strings without data loss.
 */
public sealed interface TrackerStatus : WireContract {

    /** Returns the human-readable display label. */
    public fun displayName(): String = displayName

    /** Returns the canonical Firestore document string representation. */
    public fun toFirestore(): String = wireKey

    /** Indicates whether this status represents a final terminal state where the pipeline has closed. */
    public val isTerminal: Boolean

    /** Indicates whether the application is currently active and requires ongoing monitoring or engagement. */
    public val isActive: Boolean

    /** Resolves the theme-aware Material 3 container background color for this status. */
    @Composable
    public fun containerColor(): Color

    /** Resolves the theme-aware Material 3 content on-container text/icon color for this status. */
    @Composable
    public fun contentColor(): Color

    /**
     * Closed vocabulary of known application lifecycle states.
     */
    public enum class Known(
        override val displayName: String,
        override val wireKey: String,
        override val wireTag: Int = -1,
    ) : TrackerStatus {
        /** Sentinel value representing an unfiltered filter state matching all applications. */
        NONE(displayName = "All", wireKey = "NONE"),

        /** Fallback status when the Firestore status string is missing or unspecified. */
        UNSPECIFIED(displayName = "Unspecified", wireKey = "UNSPECIFIED", wireTag = 0),

        /** Pre-funnel opportunity lead captured prior to active drafting. */
        LEAD(displayName = "Lead", wireKey = "LEAD", wireTag = 10),

        /** Initial opportunity drafting state prior to formal submission. */
        DRAFTING(displayName = "Drafting", wireKey = "DRAFTING", wireTag = 1),

        /** Formal application submitted to company or hiring platform. */
        APPLIED(displayName = "Applied", wireKey = "APPLIED", wireTag = 2),

        /** Active interview process underway (screens, technical rounds, onsites). */
        INTERVIEW(displayName = "Interview", wireKey = "INTERVIEW", wireTag = 3),

        /** Candidate expecting or negotiating a formal compensation offer. */
        OFFER_EXPECTED(displayName = "Offer Expected", wireKey = "OFFER_EXPECTED", wireTag = 4),

        /** Candidate accepted offer and successfully completed hiring. */
        HIRED(displayName = "Hired", wireKey = "HIRED", wireTag = 5),

        /** Application was rejected by employer. */
        REJECTED(displayName = "Rejected", wireKey = "REJECTED", wireTag = 6),

        /** Employer did not respond within standard follow-up horizon (>30 days). */
        NO_RESPONSE(displayName = "No Response", wireKey = "NO_RESPONSE", wireTag = 7),

        /** Candidate declined an offer extended by the company. */
        OFFER_DECLINED(displayName = "Offer Declined", wireKey = "OFFER_DECLINED", wireTag = 8),

        /** Candidate voluntarily withdrew application from consideration. */
        APPLICATION_WITHDRAWN(displayName = "Application Withdrawn", wireKey = "APPLICATION_WITHDRAWN", wireTag = 9);

        override val isTerminal: Boolean
            get() = when (this) {
                HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> true
                NONE, UNSPECIFIED, LEAD, DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> false
            }

        override val isActive: Boolean
            get() = when (this) {
                LEAD, DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> true
                NONE, UNSPECIFIED, HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> false
            }

        @Composable
        override fun containerColor(): Color = when (this) {
            LEAD, DRAFTING -> MaterialTheme.colorScheme.surfaceVariant
            APPLIED -> MaterialTheme.colorScheme.primaryContainer
            INTERVIEW -> MaterialTheme.colorScheme.secondaryContainer
            OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.tertiaryContainer
            REJECTED -> MaterialTheme.colorScheme.errorContainer
            NO_RESPONSE -> MaterialTheme.colorScheme.surfaceContainerHigh
            OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
            NONE, UNSPECIFIED -> MaterialTheme.colorScheme.surfaceVariant
        }

        @Composable
        override fun contentColor(): Color = when (this) {
            LEAD, DRAFTING -> MaterialTheme.colorScheme.onSurfaceVariant
            APPLIED -> MaterialTheme.colorScheme.onPrimaryContainer
            INTERVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
            OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.onTertiaryContainer
            REJECTED -> MaterialTheme.colorScheme.onErrorContainer
            NO_RESPONSE -> MaterialTheme.colorScheme.outline
            OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.onSurfaceVariant
            NONE, UNSPECIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    /**
     * Associated-value carrier for unmapped, future, or custom status strings.
     * Preserves the verbatim incoming [wireKey] without loss of fidelity.
     */
    public data class Unrecognized(
        override val wireKey: String
    ) : TrackerStatus {
        override val displayName: String get() = "Unknown ($wireKey)"
        override val isTerminal: Boolean get() = false
        override val isActive: Boolean get() = false

        @Composable
        override fun containerColor(): Color = MaterialTheme.colorScheme.surfaceVariant

        @Composable
        override fun contentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
    }

    public companion object {
        public val DEFAULT: TrackerStatus = Known.UNSPECIFIED

        // Static O(1) multi-key lookup map built using stdlib extension with legacy aliases
        private val lookup: Map<String, Known> = Known.entries.toWireLookupMap { entry ->
            val legacy = when (entry) {
                Known.LEAD -> listOf("status:lead", "lead")
                Known.DRAFTING -> listOf("status:drafting", "drafting", "draft")
                Known.APPLIED -> listOf("status:applied", "applied", "screening")
                Known.INTERVIEW -> listOf("status:interview", "interview", "interviewing", "onsite")
                Known.OFFER_EXPECTED -> listOf("status:offer", "offer")
                Known.HIRED -> listOf("status:hired", "hired")
                Known.REJECTED -> listOf("status:rejected", "rejected")
                Known.NO_RESPONSE -> listOf("status:no-response", "no-response", "no_response")
                Known.OFFER_DECLINED -> listOf("status:offer-declined", "offer-declined", "offer_declined")
                Known.APPLICATION_WITHDRAWN -> listOf("status:withdrawn", "withdrawn")
                Known.NONE -> listOf("status:none", "all")
                Known.UNSPECIFIED -> listOf("status:unspecified", "unspecified")
            }
            legacy + listOf(entry.name.lowercase().replace("_", "-"))
        }

        // Backward-compatible shortcut properties matching standard enum names
        public val NONE: TrackerStatus = Known.NONE
        public val UNSPECIFIED: TrackerStatus = Known.UNSPECIFIED
        public val LEAD: TrackerStatus = Known.LEAD
        public val DRAFTING: TrackerStatus = Known.DRAFTING
        public val APPLIED: TrackerStatus = Known.APPLIED
        public val INTERVIEW: TrackerStatus = Known.INTERVIEW
        public val OFFER_EXPECTED: TrackerStatus = Known.OFFER_EXPECTED
        public val HIRED: TrackerStatus = Known.HIRED
        public val REJECTED: TrackerStatus = Known.REJECTED
        public val NO_RESPONSE: TrackerStatus = Known.NO_RESPONSE
        public val OFFER_DECLINED: TrackerStatus = Known.OFFER_DECLINED
        public val APPLICATION_WITHDRAWN: TrackerStatus = Known.APPLICATION_WITHDRAWN

        /** All known canonical entries for pickers, dropdowns, and filters. */
        public val entries: List<TrackerStatus> = Known.entries

        /**
         * Resolves a raw wire string into its matching [TrackerStatus] instance.
         *
         * @param raw The raw string representation.
         * @return The matched [Known] status or [Unrecognized] containing the raw string.
         */
        public fun fromWireKey(raw: String?): TrackerStatus {
            if (raw.isNullOrBlank()) return DEFAULT
            val cleaned = raw.trim().lowercase()
            return lookup[cleaned] ?: Unrecognized(raw)
        }

        /**
         * Resolves a raw Firestore document status string into its matching [TrackerStatus] constant.
         *
         * @param raw The raw string from Firestore or local storage.
         * @return The resolved [TrackerStatus], or [UNSPECIFIED] if null/blank, or [Unrecognized] if unmapped.
         */
        public fun fromFirestore(raw: String?): TrackerStatus = fromWireKey(raw)
    }
}
