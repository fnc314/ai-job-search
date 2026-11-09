package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.runtime.Composable
import fnc314jobsearchtracker.core_widgets.generated.resources.Res
import fnc314jobsearchtracker.core_widgets.generated.resources.time_day_single
import fnc314jobsearchtracker.core_widgets.generated.resources.time_days_ago
import fnc314jobsearchtracker.core_widgets.generated.resources.time_hour_single
import fnc314jobsearchtracker.core_widgets.generated.resources.time_hours_ago
import fnc314jobsearchtracker.core_widgets.generated.resources.time_just_now
import fnc314jobsearchtracker.core_widgets.generated.resources.time_minute_single
import fnc314jobsearchtracker.core_widgets.generated.resources.time_minutes_ago
import fnc314jobsearchtracker.core_widgets.generated.resources.time_months_ago
import fnc314jobsearchtracker.core_widgets.generated.resources.time_week_single
import fnc314jobsearchtracker.core_widgets.generated.resources.time_weeks_ago
import fnc314jobsearchtracker.core_widgets.generated.resources.time_weeks_days_ago
import kotlin.time.Clock
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L
private const val DAYS_PER_FOUR_WEEKS = 28L
private const val DAYS_PER_MONTH = 30L

/**
 * Formats [instant] as a localized relative duration (e.g. "3 hours ago"), resolved entirely via `Res.string`.
 *
 * @param instant The past timestamp to format.
 * @param now Reference evaluation timestamp. Defaults to [Clock.System.now].
 * @return The localized relative string, or `null` when [instant] is [Instant.DISTANT_PAST] (no data),
 * so callers can substitute their own localized fallback.
 */
@Composable
@Suppress("CyclomaticComplexMethod")
public fun formatRelativeInstant(
    instant: Instant,
    now: Instant = Clock.System.now(),
): String? {
    if (instant == Instant.DISTANT_PAST) return null
    val totalSeconds = (now - instant).inWholeSeconds.coerceAtLeast(0)
    val totalMinutes = totalSeconds / SECONDS_PER_MINUTE
    val totalHours = totalMinutes / MINUTES_PER_HOUR
    val totalDays = totalHours / HOURS_PER_DAY

    return when {
        totalMinutes <= 0L -> stringResource(Res.string.time_just_now)
        totalMinutes == 1L -> stringResource(Res.string.time_minute_single)
        totalHours < 1L -> stringResource(Res.string.time_minutes_ago, totalMinutes)
        totalHours == 1L -> stringResource(Res.string.time_hour_single)
        totalHours < HOURS_PER_DAY -> stringResource(Res.string.time_hours_ago, totalHours)
        totalDays == 1L -> stringResource(Res.string.time_day_single)
        totalDays < DAYS_PER_WEEK -> stringResource(Res.string.time_days_ago, totalDays)
        totalDays < DAYS_PER_FOUR_WEEKS -> {
            val weeks = totalDays / DAYS_PER_WEEK
            val remainingDays = totalDays % DAYS_PER_WEEK
            when {
                remainingDays != 0L -> stringResource(Res.string.time_weeks_days_ago, weeks, remainingDays)
                weeks == 1L -> stringResource(Res.string.time_week_single)
                else -> stringResource(Res.string.time_weeks_ago, weeks)
            }
        }
        else -> stringResource(
            Res.string.time_months_ago,
            instant.toString().substringBefore('T'),
            totalDays / DAYS_PER_MONTH,
        )
    }
}
