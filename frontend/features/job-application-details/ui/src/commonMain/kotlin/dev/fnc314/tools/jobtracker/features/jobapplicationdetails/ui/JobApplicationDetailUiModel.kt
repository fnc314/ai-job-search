@file:OptIn(ExperimentalTime::class)

package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.Compensation
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.DerivedFrom
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.widgets.formatRelativeInstant
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.data_missing
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_applied_on
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_not_applied
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Decoupled UI presentation model encapsulating the complete detail view state for a single application.
 *
 * Formats structured location, compensation boundaries, timeline events, and attached artifacts.
 *
 * @property id Unique application identifier.
 * @property companyName Display name of the hiring organization.
 * @property role Position title.
 * @property status Current lifecycle status [TrackerStatus].
 * @property statusDisplayText Human-readable status label.
 * @property formattedCompensation Formatted salary range string (e.g. `"$180,000 - $220,000 USD / year"`).
 * @property formattedLocation Formatted office and remote work location string.
 * @property channel Originating ATS channel [ApplicationChannel].
 * @property channelDisplayText Formatted channel name.
 * @property fitScore Numerical fit score (0..100) or null.
 * @property fitScoreDisplayText Formatted fit rating text.
 * @property fitGaps Identified alignment gaps.
 * @property tags Categorization tags.
 * @property jobUrl Live posting URL.
 * @property jobApplicationUrl Direct candidate portal link.
 * @property createdAt Record creation timestamp, or [Instant.DISTANT_PAST] when unknown.
 * @property updatedAt Last modification timestamp, or [Instant.DISTANT_PAST] when unknown.
 * @property company Associated employer profile [Company].
 * @property timelineEvents Ordered list of audit events [TrackerEvent] forming the timeline.
 * @property artifacts Attached document artifacts [TrackerArtifact].
 * @property submittedAt An [Instant] (default of [Instant.DISTANT_FUTURE]) for when the application was marked as submitted.
 * @property submissionConfirmedAt An [Instant]? for when external email or portal confirmed the submission.
 */
@Immutable
public data class JobApplicationDetailUiModel(
  public val id: String,
  public val companyName: String,
  public val role: String?,
  public val status: TrackerStatus,
  public val statusDisplayText: String,
  public val formattedCompensation: String?,
  public val formattedLocation: String?,
  public val channel: ApplicationChannel,
  public val channelDisplayText: String,
  public val fitScore: Int?,
  public val fitScoreDisplayText: String,
  public val fitGaps: ImmutableList<String>,
  public val tags: ImmutableList<String>,
  public val jobUrl: String?,
  public val jobApplicationUrl: String?,
  public val createdAt: Instant = Instant.DISTANT_PAST,
  public val updatedAt: Instant = Instant.DISTANT_PAST,
  public val company: Company?,
  public val timelineEvents: ImmutableList<TrackerEvent>,
  public val artifacts: ImmutableList<TrackerArtifact>,
  public val emailMessageIds: ImmutableList<String> = persistentListOf(),
  public val submittedAt: Instant = Instant.DISTANT_FUTURE,
  public val submissionConfirmedAt: Instant? = null,
) {

  /** Localized relative last-update text (falls back to creation), or [backup] when unknown. */
  @Composable
  public fun lastUpdatedDisplay(backup: String = stringResource(Res.string.data_missing)): String {
    val target = if (updatedAt != Instant.DISTANT_PAST) updatedAt else createdAt
    return formatRelativeInstant(target)
      ?: backup
  }

  @Composable
  public fun submittedAt(): String = when (submittedAt) {
    Instant.DISTANT_FUTURE -> stringResource(resource = Res.string.detail_not_applied)
    else -> stringResource(
      Res.string.detail_applied_on,
      submittedAt.toLocalDateTime(
        timeZone = TimeZone.currentSystemDefault()
      ).date.let { "${it.year}-${it.month.number}-${it.day}" }
    )
  }
}

internal fun createJobApplicationDetailUiModel(
  app: JobApplication,
  company: Company?,
  events: List<TrackerEvent>,
  artifacts: List<TrackerArtifact>,
): JobApplicationDetailUiModel {
  val derivedEvents = deriveTimelineEvents(app, events)
  return JobApplicationDetailUiModel(
    id = app.id,
    companyName = app.company,
    role = app.role,
    status = app.status,
    statusDisplayText = app.status.displayName(),
    formattedCompensation = formatCompensation(app.compensation),
    formattedLocation = app.location,
    channel = app.channel,
    channelDisplayText = app.channel.displayName(),
    fitScore = app.fitScore,
    fitScoreDisplayText = app.fitScore?.toString()
      ?: "N/A",
    fitGaps = app.fitEvaluation?.gaps?.toImmutableList()
      ?: persistentListOf(),
    tags = app.tags,
    jobUrl = app.jobUrl,
    jobApplicationUrl = app.jobApplicationUrl,
    createdAt = app.createdAt,
    updatedAt = app.updatedAt,
    company = company,
    timelineEvents = derivedEvents,
    artifacts = artifacts.toImmutableList(),
    emailMessageIds = app.emailMessageIds,
    submittedAt = app.submittedAt ?: Instant.DISTANT_FUTURE,
    submissionConfirmedAt = app.submissionConfirmedAt,
  )
}

internal fun deriveTimelineEvents(
  app: JobApplication,
  events: List<TrackerEvent>,
): ImmutableList<TrackerEvent> {
  if (events.isNotEmpty()) return events.sortedByDescending { it.createdAt }.toImmutableList()
  return buildList {
    if (app.createdAt != Instant.DISTANT_PAST) {
      add(
        TrackerEvent(
          id = "evt_created",
          applicationId = app.id,
          type = TrackerEventType.APPLY,
          statusFrom = null,
          statusTo = app.status,
          note = "Application initiated for ${app.company}",
          emailMessageId = null,
          actor = EventActor.USER,
          derivedFrom = DerivedFrom.MANUAL,
          confidence = Confidence.HIGH,
          createdAt = app.createdAt,
        ),
      )
    }
    if (app.updatedAt != Instant.DISTANT_PAST && app.updatedAt != app.createdAt) {
      add(
        TrackerEvent(
          id = "evt_updated",
          applicationId = app.id,
          type = TrackerEventType.CORRECT_STATUS,
          statusFrom = TrackerStatus.DRAFTING,
          statusTo = app.status,
          note = "Application updated to ${app.status.displayName()}",
          emailMessageId = null,
          actor = EventActor.AGENT,
          derivedFrom = DerivedFrom.MANUAL,
          confidence = Confidence.HIGH,
          createdAt = app.updatedAt,
        ),
      )
    }
  }.toImmutableList()
}

private fun formatCompensation(comp: Compensation?): String? {
  if (comp == null) return null
  val currency = comp.currency
    ?: "$"
  val period = comp.period
    ?.let { "/$it" }
    ?: ""
  val min = comp.min
  val max = comp.max
  return when {
    min != null && max != null -> "$currency${min.toInt()} - $currency${max.toInt()}$period"
    min != null -> "From $currency${min.toInt()}$period"
    max != null -> "Up to $currency${max.toInt()}$period"
    else -> null
  }
}

