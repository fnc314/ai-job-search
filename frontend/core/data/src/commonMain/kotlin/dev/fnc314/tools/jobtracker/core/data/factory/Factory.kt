package dev.fnc314.tools.jobtracker.core.data.factory

import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.ArtifactStorage
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.Compensation
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.DerivedFrom
import dev.fnc314.tools.jobtracker.core.data.model.EditChange
import dev.fnc314.tools.jobtracker.core.data.model.EditScope
import dev.fnc314.tools.jobtracker.core.data.model.EditUnmatched
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Location
import dev.fnc314.tools.jobtracker.core.data.model.SourceEmail
import dev.fnc314.tools.jobtracker.core.data.model.Timeline
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifactKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerCounts
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerFit
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Trigger
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import dev.fnc314.tools.jobtracker.core.data.model.RoleLocation
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Central domain object factory synthesizing default-initialized, validated, and immutable domain models.
 *
 * Enforces schema v7 invariants, monotonic UUIDv7 synthesis via [Uuid7.generate], timestamp defaults via [Clock.System.now],
 * and defensive conversion from standard [List] instances to [kotlinx.collections.immutable.ImmutableList].
 */
public object Factory {

    /**
     * Constructs a new [JobApplication] entity with defensive list copies and automatic UUIDv7 synthesis.
     *
     * @param id Slug identifier (e.g., `"stripe-staff-android"`).
     * @param company Hiring organization name.
     * @param role Title of the position.
     * @param location Textual summary of office/remote location.
     * @param status Current lifecycle state [TrackerStatus]. Defaults to [TrackerStatus.DRAFTING].
     * @param channel Sourcing channel [ApplicationChannel]. Defaults to [ApplicationChannel.UNSPECIFIED].
     * @param jobId Requisition or external job portal ID.
     * @param jobUrl URL of the live posting.
     * @param description Full job description or summary.
     * @param tags Categorization tags. Converted defensively to [kotlinx.collections.immutable.ImmutableList].
     * @param contactIds Contact references. Converted defensively to [kotlinx.collections.immutable.ImmutableList].
     * @param compensation Structured compensation boundaries [Compensation].
     * @param locationDetail Structured location parameters [Location].
     * @param fitRating Integer fit score (0..100).
     * @param fitEvaluation Structured rubric fit assessment [TrackerFit].
     * @param counters Aggregated metric counters [TrackerCounts].
     * @param timeline Milestone timeline timestamps [Timeline].
     * @param notesRendered Markdown notes content.
     * @param createdAt Initial creation timestamp. Defaults to [Clock.System.now].
     * @param updatedAt Last update timestamp. Defaults to [Clock.System.now].
     * @param jobApplicationUrl Direct candidate portal link.
     * @param uuid Primary key [Uuid7]. Automatically synthesized via [Uuid7.generate] if null.
     * @param schemaVersion Schema version number. Defaults to `8`.
     * @param submittedAt An [Instant] for when the application was recorded as submitted
     * @param submissionConfirmedAt An [Instant] for when the application submission was confirmed
     * @return Fully constructed and immutable [JobApplication] instance.
     */
    public fun createJobApplication(
        id: String,
        company: String,
        role: String? = null,
        location: String? = null,
        status: TrackerStatus = TrackerStatus.DRAFTING,
        channel: ApplicationChannel = ApplicationChannel.UNSPECIFIED,
        jobId: String? = null,
        jobUrl: String? = null,
        description: String? = null,
        tags: List<String> = emptyList(),
        contactIds: List<String> = emptyList(),
        compensation: Compensation? = null,
        locationDetail: Location? = null,
        fitRating: Int? = null,
        fitEvaluation: TrackerFit? = null,
        counters: TrackerCounts? = null,
        timeline: Timeline? = null,
        notesRendered: String? = null,
        createdAt: Instant = Clock.System.now(),
        updatedAt: Instant = Clock.System.now(),
        jobApplicationUrl: String? = null,
        emailMessageIds: List<String> = emptyList(),
        uuid: Uuid7? = null,
        schemaVersion: Int = 8,
        submittedAt: Instant? = null,
        submissionConfirmedAt: Instant? = null,
    ): JobApplication {
        val resolvedScore = fitRating ?: fitEvaluation?.score
        val resolvedFitEvaluation = fitEvaluation ?: resolvedScore?.let {
            createTrackerFit(score = it)
        }
        return JobApplication(
            id = id,
            uuid = uuid ?: Uuid7.generate(),
            schemaVersion = schemaVersion,
            company = company,
            role = role,
            location = location,
            status = status,
            channel = channel,
            jobId = jobId,
            jobUrl = jobUrl,
            description = description,
            tags = tags.toImmutableList(),
            contactIds = contactIds.toImmutableList(),
            compensation = compensation,
            locationDetail = locationDetail,
            fitRating = resolvedScore,
            fitEvaluation = resolvedFitEvaluation,
            counters = counters,
            timeline = timeline,
            notesRendered = notesRendered,
            createdAt = createdAt,
            updatedAt = updatedAt,
            jobApplicationUrl = jobApplicationUrl,
            emailMessageIds = emailMessageIds.toImmutableList(),
            submittedAt = submittedAt,
            submissionConfirmedAt = submissionConfirmedAt,
        )
    }

    /**
     * Constructs a new [TrackerEvent] instance.
     *
     * @param id Unique event identifier.
     * @param applicationId Associated application slug.
     * @param type Event kind [TrackerEventType]. Defaults to [TrackerEventType.UNSPECIFIED].
     * @param statusFrom Previous application status [TrackerStatus].
     * @param statusTo Resulting application status [TrackerStatus].
     * @param note Contextual notes or feedback.
     * @param emailMessageId Originating message identifier.
     * @param actor Initiating actor [EventActor]. Defaults to [EventActor.USER].
     * @param derivedFrom Source derivation mechanism [DerivedFrom]. Defaults to [DerivedFrom.MANUAL].
     * @param confidence Extraction confidence [Confidence]. Defaults to [Confidence.HIGH].
     * @param createdAt Creation timestamp. Defaults to [Clock.System.now].
     * @return A constructed [TrackerEvent] instance.
     */
    public fun createTrackerEvent(
        id: String,
        applicationId: String,
        type: TrackerEventType = TrackerEventType.UNSPECIFIED,
        statusFrom: TrackerStatus? = null,
        statusTo: TrackerStatus? = null,
        note: String? = null,
        emailMessageId: String? = null,
        actor: EventActor = EventActor.USER,
        derivedFrom: DerivedFrom = DerivedFrom.MANUAL,
        confidence: Confidence = Confidence.HIGH,
        createdAt: Instant = Clock.System.now(),
    ): TrackerEvent = TrackerEvent(
        id = id,
        applicationId = applicationId,
        type = type,
        statusFrom = statusFrom,
        statusTo = statusTo,
        note = note,
        emailMessageId = emailMessageId,
        actor = actor,
        derivedFrom = derivedFrom,
        confidence = confidence,
        createdAt = createdAt,
    )

    /**
     * Constructs a new [TrackerEdit] proposal.
     *
     * @param id Unique proposal identifier.
     * @param applicationId Target application slug.
     * @param kind Proposal category [TrackerEditKind]. Defaults to [TrackerEditKind.STATUS].
     * @param state Initial review state [TrackerEditState]. Defaults to [TrackerEditState.PENDING].
     * @param trigger Ingestion trigger [Trigger]. Defaults to [Trigger.APPLICATION_CREATED].
     * @param changes Proposed field mutations [EditChange].
     * @param unmatched Unmatched source fields [EditUnmatched].
     * @param sourceEmail Provenance email details [SourceEmail].
     * @param scope Target document scope [EditScope].
     * @param confidence Extraction confidence [Confidence]. Defaults to [Confidence.HIGH].
     * @param createdAt Staging timestamp. Defaults to [Clock.System.now].
     * @param expiresAt Optional expiration timestamp.
     * @return A constructed [TrackerEdit] instance.
     */
    public fun createTrackerEdit(
        id: String,
        applicationId: String = "",
        kind: TrackerEditKind = TrackerEditKind.STATUS,
        state: TrackerEditState = TrackerEditState.PENDING,
        trigger: Trigger = Trigger.APPLICATION_CREATED,
        changes: List<EditChange> = emptyList(),
        unmatched: List<EditUnmatched> = emptyList(),
        sourceEmail: SourceEmail? = null,
        scope: EditScope? = null,
        confidence: Confidence = Confidence.HIGH,
        createdAt: Instant? = Clock.System.now(),
        expiresAt: Instant? = null,
    ): TrackerEdit = TrackerEdit(
        id = id,
        applicationId = applicationId,
        kind = kind,
        state = state,
        trigger = trigger,
        changes = changes,
        unmatched = unmatched,
        sourceEmail = sourceEmail,
        scope = scope,
        confidence = confidence,
        createdAt = createdAt,
        expiresAt = expiresAt,
    )

    /**
     * Constructs a new [TrackerArtifact] instance.
     *
     * @param id Unique artifact identifier.
     * @param applicationId Associated application slug.
     * @param kind Functional artifact classification [TrackerArtifactKind]. Defaults to [TrackerArtifactKind.UNSPECIFIED].
     * @param filename Document filename with extension.
     * @param contentType MIME content type.
     * @param size Size in bytes.
     * @param sha256 Content hash.
     * @param storage Cloud Storage bucket coordinates [ArtifactStorage].
     * @param localPath Local filesystem path.
     * @param externalUrl Remote access URL.
     * @param createdAt Creation timestamp. Defaults to [Clock.System.now].
     * @return A constructed [TrackerArtifact] instance.
     */
    public fun createTrackerArtifact(
        id: String,
        applicationId: String,
        kind: TrackerArtifactKind = TrackerArtifactKind.UNSPECIFIED,
        filename: String,
        contentType: String? = null,
        size: Long? = null,
        sha256: String? = null,
        storage: ArtifactStorage? = null,
        localPath: String? = null,
        externalUrl: String? = null,
        createdAt: Instant? = Clock.System.now(),
    ): TrackerArtifact = TrackerArtifact(
        id = id,
        applicationId = applicationId,
        kind = kind,
        filename = filename,
        contentType = contentType,
        size = size,
        sha256 = sha256,
        storage = storage,
        localPath = localPath,
        externalUrl = externalUrl,
        createdAt = createdAt,
    )

    /**
     * Constructs a new [Company] entity.
     *
     * @param id Slug identifier.
     * @param name Official company name.
     * @param aliases Alternate trade names or abbreviations.
     * @param website Corporate URL.
     * @param industry Industry sector.
     * @param createdAt Creation timestamp. Defaults to [Clock.System.now].
     * @param updatedAt Last update timestamp. Defaults to [Clock.System.now].
     * @param linkedinUrl LinkedIn company page URL.
     * @return A constructed [Company] instance.
     */
    public fun createCompany(
        id: String,
        name: String,
        aliases: List<String> = emptyList(),
        website: String? = null,
        industry: String? = null,
        createdAt: Instant? = Clock.System.now(),
        updatedAt: Instant? = Clock.System.now(),
        logoUrl: String? = null,
        logoFormat: dev.fnc314.tools.jobtracker.core.data.model.LogoFormat = dev.fnc314.tools.jobtracker.core.data.model.LogoFormat.UNKNOWN,
        logoWidth: Int = 0,
        logoHeight: Int = 0,
        logoShape: dev.fnc314.tools.jobtracker.core.data.model.LogoShape = dev.fnc314.tools.jobtracker.core.data.model.LogoShape.UNKNOWN,
        logoFetchedAt: Instant = Instant.DISTANT_PAST,
        metrics: dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics? = null,
        linkedinUrl: String? = null,
    ): Company = Company(
        id = id,
        name = name,
        aliases = aliases,
        website = website,
        industry = industry,
        createdAt = createdAt,
        updatedAt = updatedAt,
        logoUrl = logoUrl,
        logoFormat = logoFormat,
        logoWidth = logoWidth,
        logoHeight = logoHeight,
        logoShape = logoShape,
        logoFetchedAt = logoFetchedAt,
        metrics = metrics,
        linkedinUrl = linkedinUrl,
    )

    /**
     * Constructs a new [Compensation] specification.
     *
     * @param min Minimum compensation boundary.
     * @param max Maximum compensation boundary.
     * @param currency Three-letter currency code (e.g. `"USD"`).
     * @param period Payment frequency (e.g. `"year"`).
     * @param bandSource Attribution source.
     * @param vsTarget Floor evaluation string.
     * @return A constructed [Compensation] instance.
     */
    public fun createCompensation(
        min: Double? = null,
        max: Double? = null,
        currency: String? = null,
        period: String? = null,
        bandSource: String? = null,
        vsTarget: String? = null,
    ): Compensation = Compensation(
        min = min,
        max = max,
        currency = currency,
        period = period,
        bandSource = bandSource,
        vsTarget = vsTarget,
    )

    /**
     * Constructs a new [Location] specification.
     *
     * @param city City or metropolitan area.
     * @param region State or administrative region.
     * @param country Country code or name.
     * @param workMode Remote policy [WorkMode]. Defaults to [WorkMode.UNSPECIFIED].
     * @param relocation Relocation requirement flag. Defaults to `false`.
     * @param superCommuter Super-commuting eligibility flag. Defaults to `false`.
     * @return A constructed [Location] instance.
     */
    public fun createLocation(
        city: String? = null,
        region: String? = null,
        country: String? = null,
        roleLocation: RoleLocation = RoleLocation.UNSPECIFIED,
        relocation: Boolean = false,
        superCommuter: Boolean = false,
    ): Location = Location(
        city = city,
        region = region,
        country = country,
        roleLocation = roleLocation,
        relocation = relocation,
        superCommuter = superCommuter,
    )

    /**
     * Constructs a new [TrackerFit] rubric evaluation.
     *
     * @param rubricVersion Algorithm version. Defaults to `1`.
     * @param score Fit rating (0..100).
     * @param evaluatedAt Evaluation timestamp. Defaults to [Clock.System.now].
     * @param gaps Identified alignment gaps. Converted to [kotlinx.collections.immutable.ImmutableList].
     * @return A constructed [TrackerFit] instance.
     */
    public fun createTrackerFit(
        rubricVersion: Int = 1,
        score: Int? = null,
        evaluatedAt: Instant? = Clock.System.now(),
        gaps: List<String> = emptyList(),
    ): TrackerFit = TrackerFit(
        rubricVersion = rubricVersion,
        score = score,
        evaluatedAt = evaluatedAt,
        gaps = gaps.toImmutableList(),
    )

    /**
     * Constructs a new [TrackerCounts] metric summary.
     *
     * @param events Count of logged events. Defaults to `0`.
     * @param followUpsSent Count of follow-up communications. Defaults to `0`.
     * @param interviews Count of interview rounds. Defaults to `0`.
     * @param rejections Count of rejections. Defaults to `0`.
     * @return A constructed [TrackerCounts] instance.
     */
    public fun createTrackerCounts(
        events: Int = 0,
        followUpsSent: Int = 0,
        interviews: Int = 0,
        rejections: Int = 0,
    ): TrackerCounts = TrackerCounts(
        events = events,
        followUpsSent = followUpsSent,
        interviews = interviews,
        rejections = rejections,
    )

    /**
     * Constructs a new [Timeline] milestone schedule.
     *
     * @param firstActivityAt First recorded interaction timestamp.
     * @param lastActivityAt Most recent interaction timestamp.
     * @param daysSinceActivity Days elapsed since last activity. Defaults to `0`.
     * @param nextFollowUpAt Scheduled next follow-up communication timestamp.
     * @return A constructed [Timeline] instance.
     */
    public fun createTimeline(
        firstActivityAt: Instant = Instant.DISTANT_PAST,
        lastActivityAt: Instant = Instant.DISTANT_PAST,
        daysSinceActivity: Int = 0,
        nextFollowUpAt: Instant = Instant.DISTANT_PAST,
    ): Timeline = Timeline(
        firstActivityAt = firstActivityAt,
        lastActivityAt = lastActivityAt,
        daysSinceActivity = daysSinceActivity,
        nextFollowUpAt = nextFollowUpAt,
    )
}
