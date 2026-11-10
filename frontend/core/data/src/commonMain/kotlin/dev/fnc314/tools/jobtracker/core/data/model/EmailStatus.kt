package dev.fnc314.tools.jobtracker.core.data.model

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

    public val isNoise: Boolean get() = this == NOISE
    public val isActionable: Boolean get() = this == INTERVIEW_REQUEST || this == OFFER || this == ASSESSMENT

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

    public companion object {
        public fun fromString(raw: String?): EmailStatus {
            if (raw.isNullOrBlank()) return UNCLASSIFIED
            val normalized = raw.trim().uppercase().replace("-", "_")
            return entries.firstOrNull { it.name == normalized } ?: UNCLASSIFIED
        }
    }
}
