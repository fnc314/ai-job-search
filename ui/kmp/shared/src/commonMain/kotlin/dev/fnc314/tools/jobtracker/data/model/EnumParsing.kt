package dev.fnc314.tools.jobtracker.data.model

/**
 * Parses namespaced Firestore strings into Kotlin enums.
 * Firestore stores: "status:applied", "channel:greenhouse", "event:recruiter-screen", etc.
 * Uses explicit maps matching the records/parser EnumMapping.
 */
object EnumParsing {

    // ── TrackerStatus ──

    private val trackerStatusMap = mapOf(
        "status:drafting" to TrackerStatus.DRAFTING,
        "status:applied" to TrackerStatus.APPLIED,
        "status:interview" to TrackerStatus.INTERVIEW,
        "status:offer" to TrackerStatus.OFFER_EXPECTED,
        "status:hired" to TrackerStatus.HIRED,
        "status:rejected" to TrackerStatus.REJECTED,
        "status:no-response" to TrackerStatus.NO_RESPONSE,
        "status:offer-declined" to TrackerStatus.OFFER_DECLINED,
        "status:withdrawn" to TrackerStatus.APPLICATION_WITHDRAWN,
    )

    fun parseTrackerStatus(raw: String?): TrackerStatus =
        if (raw.isNullOrBlank()) TrackerStatus.UNSPECIFIED else trackerStatusMap[raw] ?: TrackerStatus.UNSPECIFIED

    // ── ApplicationChannel ──

    private val applicationChannelMap = mapOf(
        "channel:greenhouse" to ApplicationChannel.GREENHOUSE,
        "channel:workday" to ApplicationChannel.WORKDAY,
        "channel:lever" to ApplicationChannel.LEVER,
        "channel:ashby" to ApplicationChannel.ASHBY,
        "channel:smartrecruiters" to ApplicationChannel.SMARTRECRUITERS,
        "channel:linkedin" to ApplicationChannel.LINKEDIN,
        "channel:referral" to ApplicationChannel.REFERRAL,
        "channel:direct" to ApplicationChannel.DIRECT,
        "channel:other" to ApplicationChannel.OTHER,
    )

    fun parseApplicationChannel(raw: String?): ApplicationChannel =
        if (raw.isNullOrBlank()) ApplicationChannel.UNSPECIFIED else applicationChannelMap[raw] ?: ApplicationChannel.UNSPECIFIED

    // ── TrackerEventType ──

    private val trackerEventTypeMap = mapOf(
        "event:applied" to TrackerEventType.APPLY,
        "event:acknowledged" to TrackerEventType.ACKNOWLEDGE,
        "event:recruiter-screen" to TrackerEventType.RECRUITER_SCREEN,
        "event:interview-scheduled" to TrackerEventType.SCHEDULE_INTERVIEW,
        "event:interview-completed" to TrackerEventType.COMPLETE_INTERVIEW,
        "event:assessment" to TrackerEventType.ASSESS,
        "event:onsite" to TrackerEventType.VISIT_ONSITE,
        "event:offer" to TrackerEventType.OFFER,
        "event:offer-accepted" to TrackerEventType.ACCEPT_OFFER,
        "event:offer-declined" to TrackerEventType.DECLINE_OFFER,
        "event:rejection" to TrackerEventType.REJECT,
        "event:no-response" to TrackerEventType.GHOST,
        "event:withdrawn" to TrackerEventType.WITHDRAW,
        "event:follow-up-sent" to TrackerEventType.SEND_FOLLOW_UP,
        "event:document-tailored" to TrackerEventType.TAILOR_DOCUMENT,
        "event:fit-recalculated" to TrackerEventType.RECALCULATE_FIT,
        "event:artifact-added" to TrackerEventType.ADD_ARTIFACT,
        "event:note-added" to TrackerEventType.ADD_NOTE,
        "event:imported" to TrackerEventType.IMPORT,
        "event:status-corrected" to TrackerEventType.CORRECT_STATUS,
    )

    fun parseTrackerEventType(raw: String?): TrackerEventType =
        if (raw.isNullOrBlank()) TrackerEventType.UNSPECIFIED else trackerEventTypeMap[raw] ?: TrackerEventType.UNSPECIFIED

    // ── TrackerArtifactKind ──

    private val artifactKindMap = mapOf(
        "artifact:cv" to TrackerArtifactKind.CV,
        "artifact:cover-letter" to TrackerArtifactKind.COVER_LETTER,
        "artifact:outcome" to TrackerArtifactKind.OUTCOME,
        "artifact:posting" to TrackerArtifactKind.POSTING,
        "artifact:assessment" to TrackerArtifactKind.ASSESSMENT,
    )

    fun parseTrackerArtifactKind(raw: String?): TrackerArtifactKind =
        if (raw.isNullOrBlank()) TrackerArtifactKind.UNSPECIFIED else artifactKindMap[raw] ?: TrackerArtifactKind.UNSPECIFIED

    // ── WorkMode ──

    private val workModeMap = mapOf(
        "work-mode:onsite" to WorkMode.ONSITE,
        "work-mode:hybrid" to WorkMode.HYBRID,
        "work-mode:remote" to WorkMode.REMOTE,
    )

    fun parseWorkMode(raw: String?): WorkMode =
        if (raw.isNullOrBlank()) WorkMode.UNSPECIFIED else workModeMap[raw] ?: WorkMode.UNSPECIFIED

    // ── EventActor ──

    private val eventActorMap = mapOf(
        "actor:user" to EventActor.USER,
        "actor:agent" to EventActor.AGENT,
        "actor:email-sync" to EventActor.EMAIL_SYNC,
        "actor:import" to EventActor.IMPORT,
        "actor:scheduler" to EventActor.SCHEDULER,
    )

    fun parseEventActor(raw: String?): EventActor =
        if (raw.isNullOrBlank()) EventActor.UNSPECIFIED else eventActorMap[raw] ?: EventActor.UNSPECIFIED

    // ── Confidence ──

    private val confidenceMap = mapOf(
        "confidence:high" to Confidence.HIGH,
        "confidence:medium" to Confidence.MEDIUM,
        "confidence:low" to Confidence.LOW,
    )

    fun parseConfidence(raw: String?): Confidence =
        if (raw.isNullOrBlank()) Confidence.UNSPECIFIED else confidenceMap[raw] ?: Confidence.UNSPECIFIED

    // ── DerivedFrom ──

    private val derivedFromMap = mapOf(
        "derived:manual" to DerivedFrom.MANUAL,
        "derived:notes-regex" to DerivedFrom.NOTES_REGEX,
        "derived:email" to DerivedFrom.EMAIL,
        "derived:csv-import" to DerivedFrom.CSV_IMPORT,
    )

    fun parseDerivedFrom(raw: String?): DerivedFrom =
        if (raw.isNullOrBlank()) DerivedFrom.UNSPECIFIED else derivedFromMap[raw] ?: DerivedFrom.UNSPECIFIED

    // ── TrackerEditState ──

    private val editStateMap = mapOf(
        "proposal:pending" to TrackerEditState.PENDING,
        "proposal:approved" to TrackerEditState.APPROVED,
        "proposal:rejected" to TrackerEditState.REJECTED,
        "proposal:expired" to TrackerEditState.EXPIRED,
    )

    fun parseTrackerEditState(raw: String?): TrackerEditState =
        if (raw.isNullOrBlank()) TrackerEditState.UNSPECIFIED else editStateMap[raw] ?: TrackerEditState.UNSPECIFIED

    // ── TrackerEditKind ──

    private val editKindMap = mapOf(
        "edit:status" to TrackerEditKind.STATUS,
        "edit:event" to TrackerEditKind.EVENT,
        "edit:score" to TrackerEditKind.SCORE,
        "edit:artifact" to TrackerEditKind.ARTIFACT,
        "edit:note" to TrackerEditKind.NOTE,
    )

    fun parseTrackerEditKind(raw: String?): TrackerEditKind =
        if (raw.isNullOrBlank()) TrackerEditKind.UNSPECIFIED else editKindMap[raw] ?: TrackerEditKind.UNSPECIFIED

    // ── Trigger ──

    private val triggerMap = mapOf(
        "trigger:manual" to Trigger.MANUAL,
        "trigger:email-sync" to Trigger.EMAIL_SYNC,
        "trigger:scheduler" to Trigger.SCHEDULER,
        "trigger:import" to Trigger.IMPORT,
    )

    fun parseTrigger(raw: String?): Trigger =
        if (raw.isNullOrBlank()) Trigger.UNSPECIFIED else triggerMap[raw] ?: Trigger.UNSPECIFIED
}
