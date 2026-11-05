package dev.fnc314.tools.jobtracker.core.data.factory

import dev.fnc314.tools.jobtracker.core.data.model.*
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Clock
import kotlin.time.Instant

public object Factory {

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
        createdAt: Instant? = Clock.System.now(),
        updatedAt: Instant? = Clock.System.now(),
        jobApplicationUrl: String? = null,
    ): JobApplication {
        val resolvedScore = fitRating ?: fitEvaluation?.score
        val resolvedFitEvaluation = fitEvaluation ?: resolvedScore?.let {
            createTrackerFit(score = it)
        }
        return JobApplication(
            id = id,
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
        )
    }

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
        createdAt: Instant? = Clock.System.now(),
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

    public fun createCompany(
        id: String,
        name: String,
        aliases: List<String> = emptyList(),
        website: String? = null,
        industry: String? = null,
        createdAt: Instant? = Clock.System.now(),
        updatedAt: Instant? = Clock.System.now(),
    ): Company = Company(
        id = id,
        name = name,
        aliases = aliases,
        website = website,
        industry = industry,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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

    public fun createLocation(
        city: String? = null,
        region: String? = null,
        country: String? = null,
        workMode: WorkMode = WorkMode.UNSPECIFIED,
        relocation: Boolean = false,
        superCommuter: Boolean = false,
    ): Location = Location(
        city = city,
        region = region,
        country = country,
        workMode = workMode,
        relocation = relocation,
        superCommuter = superCommuter,
    )

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

    public fun createTimeline(
        firstActivityAt: Instant? = null,
        lastActivityAt: Instant? = null,
        daysSinceActivity: Int = 0,
        nextFollowUpAt: Instant? = null,
    ): Timeline = Timeline(
        firstActivityAt = firstActivityAt,
        lastActivityAt = lastActivityAt,
        daysSinceActivity = daysSinceActivity,
        nextFollowUpAt = nextFollowUpAt,
    )
}
