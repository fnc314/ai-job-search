package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Lifecycle states of a job application tracked within the system.
 *
 * Each status maps directly to a canonical namespaced string stored in Firestore (`firestoreValue`),
 * defines semantic presentation semantics (such as active vs. terminal state), and provides Material 3
 * container and content colors for UI badges.
 *
 * @property displayName Human-readable label suitable for UI buttons, badges, and filters.
 * @property firestoreValue Canonical namespaced Firestore identifier (e.g., `"status:applied"`).
 */
public enum class TrackerStatus(
    public val displayName: String,
    public val firestoreValue: String,
) {
    /** Sentinel value representing an unfiltered filter state matching all applications. */
    NONE(displayName = "All", firestoreValue = "status:none"),

    /** Fallback status when the Firestore status string is missing or unrecognized. */
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "status:unspecified"),

    /** Initial opportunity lead captured prior to formal submission. */
    DRAFTING(displayName = "Lead", firestoreValue = "status:drafting"),

    /** Formal application submitted to company or hiring platform. */
    APPLIED(displayName = "Applied", firestoreValue = "status:applied"),

    /** Active interview process underway (screens, technical rounds, onsites). */
    INTERVIEW(displayName = "Interview", firestoreValue = "status:interview"),

    /** Candidate expecting or negotiating a formal compensation offer. */
    OFFER_EXPECTED(displayName = "Offer Expected", firestoreValue = "status:offer"),

    /** Candidate accepted offer and successfully completed hiring. */
    HIRED(displayName = "Hired", firestoreValue = "status:hired"),

    /** Application was rejected by employer. */
    REJECTED(displayName = "Rejected", firestoreValue = "status:rejected"),

    /** Employer did not respond within standard follow-up horizon (>30 days). */
    NO_RESPONSE(displayName = "No Response", firestoreValue = "status:no-response"),

    /** Candidate declined an offer extended by the company. */
    OFFER_DECLINED(displayName = "Offer Declined", firestoreValue = "status:offer-declined"),

    /** Candidate voluntarily withdrew application from consideration. */
    APPLICATION_WITHDRAWN(displayName = "Application Withdrawn", firestoreValue = "status:withdrawn");

    /**
     * Returns the human-readable display label.
     */
    public fun displayName(): String = displayName

    /**
     * Returns the canonical Firestore document string representation.
     */
    public fun toFirestore(): String = firestoreValue

    /**
     * Indicates whether this status represents a final terminal state where the pipeline has closed.
     */
    public val isTerminal: Boolean
        get() = when (this) {
            HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> true
            NONE, UNSPECIFIED, DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> false
        }

    /**
     * Indicates whether the application is currently active and requires ongoing monitoring or engagement.
     */
    public val isActive: Boolean
        get() = when (this) {
            DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> true
            NONE, UNSPECIFIED, HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> false
        }

    /**
     * Resolves the theme-aware Material 3 container background color for this status.
     *
     * @return The [Color] instance matching the current [MaterialTheme.colorScheme].
     */
    @Composable
    public fun containerColor(): Color = when (this) {
        DRAFTING -> MaterialTheme.colorScheme.surfaceVariant
        APPLIED -> MaterialTheme.colorScheme.primaryContainer
        INTERVIEW -> MaterialTheme.colorScheme.secondaryContainer
        OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.tertiaryContainer
        REJECTED -> MaterialTheme.colorScheme.errorContainer
        NO_RESPONSE -> MaterialTheme.colorScheme.surfaceContainerHigh
        OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.surfaceVariant
        NONE, UNSPECIFIED -> MaterialTheme.colorScheme.surfaceVariant
    }

    /**
     * Resolves the theme-aware Material 3 content on-container text/icon color for this status.
     *
     * @return The [Color] instance matching the current [MaterialTheme.colorScheme].
     */
    @Composable
    public fun contentColor(): Color = when (this) {
        DRAFTING -> MaterialTheme.colorScheme.onSurfaceVariant
        APPLIED -> MaterialTheme.colorScheme.onPrimaryContainer
        INTERVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
        OFFER_EXPECTED, HIRED -> MaterialTheme.colorScheme.onTertiaryContainer
        REJECTED -> MaterialTheme.colorScheme.onErrorContainer
        NO_RESPONSE -> MaterialTheme.colorScheme.outline
        OFFER_DECLINED, APPLICATION_WITHDRAWN -> MaterialTheme.colorScheme.onSurfaceVariant
        NONE, UNSPECIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    public companion object {
        /**
         * Resolves a raw Firestore document status string into its matching [TrackerStatus] constant.
         *
         * @param raw The raw string from Firestore or local storage.
         * @return The resolved [TrackerStatus], or [UNSPECIFIED] if null, blank, or unmapped.
         */
        public fun fromFirestore(raw: String?): TrackerStatus {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            return entries.firstOrNull { 
                it.firestoreValue.equals(raw, ignoreCase = true) ||
                it.name.equals(raw, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
