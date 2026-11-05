package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

public enum class TrackerStatus(
    public val displayName: String,
    public val firestoreValue: String,
) {
    NONE(displayName = "All", firestoreValue = "status:none"),
    UNSPECIFIED(displayName = "Unspecified", firestoreValue = "status:unspecified"),
    DRAFTING(displayName = "Lead", firestoreValue = "status:drafting"),
    APPLIED(displayName = "Applied", firestoreValue = "status:applied"),
    INTERVIEW(displayName = "Interview", firestoreValue = "status:interview"),
    OFFER_EXPECTED(displayName = "Offer Expected", firestoreValue = "status:offer"),
    HIRED(displayName = "Hired", firestoreValue = "status:hired"),
    REJECTED(displayName = "Rejected", firestoreValue = "status:rejected"),
    NO_RESPONSE(displayName = "No Response", firestoreValue = "status:no-response"),
    OFFER_DECLINED(displayName = "Offer Declined", firestoreValue = "status:offer-declined"),
    APPLICATION_WITHDRAWN(displayName = "Application Withdrawn", firestoreValue = "status:withdrawn");

    public fun displayName(): String = displayName
    public fun toFirestore(): String = firestoreValue

    public val isTerminal: Boolean
        get() = when (this) {
            HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> true
            NONE, UNSPECIFIED, DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> false
        }

    public val isActive: Boolean
        get() = when (this) {
            DRAFTING, APPLIED, INTERVIEW, OFFER_EXPECTED -> true
            NONE, UNSPECIFIED, HIRED, REJECTED, NO_RESPONSE, OFFER_DECLINED, APPLICATION_WITHDRAWN -> false
        }

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
        public fun fromFirestore(raw: String?): TrackerStatus {
            if (raw.isNullOrBlank()) return UNSPECIFIED
            return entries.firstOrNull { 
                it.firestoreValue.equals(raw, ignoreCase = true) ||
                it.name.equals(raw, ignoreCase = true)
            } ?: UNSPECIFIED
        }
    }
}
