package dev.fnc314.tools.jobtracker.core.data.util

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Formats a semantic relative timestamp following precedence:
 * `updatedAt` -> `createdAt` -> `null`.
 *
 * Rules:
 *  - X hours until 24 hours
 *  - X days until 7 days
 *  - X Weeks and Y days until 4 Weeks
 *  - X Months, Y Weeks, and Z Days until 2 Months
 *  - YYYY-MM-DD (X Months ago) thereafter
 *
 * @param updatedAt Last modified timestamp.
 * @param createdAt Initial creation timestamp.
 * @param now Reference evaluation timestamp. Defaults to [Clock.System.now].
 * @return Formatted human-friendly relative string prefixed with `"Updated "` or `"Created "`, or null if both inputs are null.
 */
public fun formatSemanticTimestamp(
    updatedAt: Instant?,
    createdAt: Instant?,
    now: Instant = Clock.System.now(),
): String? {
    val targetInstant = updatedAt ?: createdAt ?: return null
    val isUpdated = updatedAt != null
    val prefix = if (isUpdated) "Updated " else "Created "
    return prefix + formatSemanticInstant(targetInstant, now)
}

/**
 * Formats a single [Instant] into human-friendly relative duration text.
 *
 * @param instant The past timestamp to format.
 * @param now Reference evaluation timestamp. Defaults to [Clock.System.now].
 * @return Semantic relative time description (e.g., `"2 hours ago"`, `"3 days ago"`, `"1 week and 2 days ago"`).
 */
@Suppress("CyclomaticComplexMethod", "MagicNumber")
public fun formatSemanticInstant(
    instant: Instant,
    now: Instant = Clock.System.now(),
): String {
    val duration = now - instant
    val totalSeconds = duration.inWholeSeconds.coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val totalDays = totalHours / 24

    return when {
        totalHours < 1 -> {
            if (totalMinutes <= 1) "just now" else "$totalMinutes hours ago"
        }
        totalHours < 24 -> {
            if (totalHours == 1L) "1 hour ago" else "$totalHours hours ago"
        }
        totalDays < 7 -> {
            if (totalDays == 1L) "1 day ago" else "$totalDays days ago"
        }
        totalDays < 28 -> {
            val weeks = totalDays / 7
            val remainingDays = totalDays % 7
            val weekPart = if (weeks == 1L) "1 week" else "$weeks weeks"
            val dayPart = when (remainingDays) {
                0L -> ""
                1L -> " and 1 day"
                else -> " and $remainingDays days"
            }
            "$weekPart$dayPart ago"
        }
        totalDays < 60 -> {
            val months = totalDays / 30
            val remDaysAfterMonth = totalDays % 30
            val weeks = remDaysAfterMonth / 7
            val remDaysAfterWeek = remDaysAfterMonth % 7

            val monthPart = if (months == 1L) "1 month" else "$months months"
            val weekPart = when (weeks) {
                0L -> ""
                1L -> ", 1 week"
                else -> ", $weeks weeks"
            }
            val dayPart = when (remDaysAfterWeek) {
                0L -> ""
                1L -> ", and 1 day"
                else -> ", and $remDaysAfterWeek days"
            }
            "$monthPart$weekPart$dayPart ago"
        }
        else -> {
            val months = totalDays / 30
            val monthText = if (months == 1L) "1 month ago" else "$months months ago"
            "${instant.toString().substringBefore('T')} ($monthText)"
        }
    }
}
