package dev.fnc314.tools.jobtracker.util

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Formats a semantic timestamp following the precedence:
 * updatedAt -> createdAt -> null
 *
 * Rules:
 *  - X hours until 24
 *  - X days until 7
 *  - X Weeks and Y days until 4 Weeks
 *  - X Months, Y Weeks, and Z Days until 2 Months
 *  - YYYY-MM-DD (X Months ago) thereafter
 */
internal fun formatSemanticTimestamp(
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
 * Formats a single Instant into semantic human-friendly relative duration text.
 */
internal fun formatSemanticInstant(
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
        totalDays < 28 -> { // Until 4 Weeks
            val weeks = totalDays / 7
            val days = totalDays % 7
            val weekPart = if (weeks == 1L) "1 week" else "$weeks weeks"
            if (days == 0L) {
                "$weekPart ago"
            } else {
                val dayPart = if (days == 1L) "1 day" else "$days days"
                "$weekPart and $dayPart ago"
            }
        }
        totalDays < 60 -> { // Until 2 Months (~60 days)
            val months = (totalDays / 30).coerceAtLeast(1)
            val remainingDaysAfterMonths = totalDays % 30
            val weeks = remainingDaysAfterMonths / 7
            val days = remainingDaysAfterMonths % 7

            val parts = mutableListOf<String>()
            parts.add(if (months == 1L) "1 month" else "$months months")
            if (weeks > 0) {
                parts.add(if (weeks == 1L) "1 week" else "$weeks weeks")
            }
            if (days > 0) {
                parts.add(if (days == 1L) "1 day" else "$days days")
            }
            parts.joinToString(", ") + " ago"
        }
        else -> {
            // YYYY-MM-DD (X months ago) thereafter
            val months = (totalDays / 30.4375).toInt().coerceAtLeast(2)
            val dateStr = instant.toString().take(10)
            "$dateStr ($months months ago)"
        }
    }
}
