package dev.fnc314.aijobtracker.frontend.core.data.model

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Enumerated lifecycle states and evidentiary classifications for email records.
 */
public enum class EmailStatus {
    UNCLASSIFIED,
    ACKNOWLEDGEMENT,
    INTERVIEW_REQUEST,
    INTERVIEW_CONFIRMATION,
    ASSESSMENT,
    OFFER,
    REJECTION,
    CORRESPONDENCE,
    NOISE;

    /** Whether this email represents background noise or irrelevant correspondence. */
    public val isNoise: Boolean get() = this == NOISE

    /** Whether this email requires immediate candidate attention or response. */
    public val isActionable: Boolean get() = this == INTERVIEW_REQUEST || this == OFFER || this == ASSESSMENT

    /** Resolves the container surface color for this status in Material 3 UI. */
    @Composable
    public fun containerColor(): Color = when (this) {
        UNCLASSIFIED -> MaterialTheme.colorScheme.surfaceVariant
        ACKNOWLEDGEMENT -> MaterialTheme.colorScheme.surfaceContainerHigh
        INTERVIEW_REQUEST -> MaterialTheme.colorScheme.secondaryFixed
        INTERVIEW_CONFIRMATION -> MaterialTheme.colorScheme.primaryFixed
        ASSESSMENT -> MaterialTheme.colorScheme.tertiaryFixed
        OFFER -> MaterialTheme.colorScheme.primaryContainer
        REJECTION -> MaterialTheme.colorScheme.errorContainer
        CORRESPONDENCE -> MaterialTheme.colorScheme.surfaceContainer
        NOISE -> MaterialTheme.colorScheme.surfaceDim
    }

    /** Resolves the content text/icon color for this status in Material 3 UI. */
    @Composable
    public fun contentColor(): Color = when (this) {
        UNCLASSIFIED -> MaterialTheme.colorScheme.onSurfaceVariant
        ACKNOWLEDGEMENT -> MaterialTheme.colorScheme.onSurface
        INTERVIEW_REQUEST -> MaterialTheme.colorScheme.onSecondaryFixed
        INTERVIEW_CONFIRMATION -> MaterialTheme.colorScheme.onPrimaryFixed
        ASSESSMENT -> MaterialTheme.colorScheme.onTertiaryFixed
        OFFER -> MaterialTheme.colorScheme.onPrimaryContainer
        REJECTION -> MaterialTheme.colorScheme.onErrorContainer
        CORRESPONDENCE -> MaterialTheme.colorScheme.onSurface
        NOISE -> MaterialTheme.colorScheme.outline
    }

    /** Factory helpers for [EmailStatus]. */
    public companion object {
        /**
         * Resolves a raw string name to an [EmailStatus].
         *
         * @param raw Raw status string.
         * @return Matched [EmailStatus] or [UNCLASSIFIED].
         */
        public fun fromString(raw: String?): EmailStatus {
            if (raw.isNullOrBlank()) return UNCLASSIFIED
            val normalized = raw.trim().uppercase().replace("-", "_")
            return entries.firstOrNull { it.name == normalized } ?: UNCLASSIFIED
        }
    }
}
