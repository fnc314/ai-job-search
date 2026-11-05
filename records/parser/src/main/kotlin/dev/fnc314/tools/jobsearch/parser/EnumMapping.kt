package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifactKind
import dev.fnc314.tools.jobsearch.tracker.v1.WorkMode
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditState
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditKind
import dev.fnc314.tools.jobsearch.tracker.v1.Trigger
import dev.fnc314.tools.jobsearch.tracker.v1.SyncKind
import dev.fnc314.tools.jobsearch.tracker.v1.SyncState
import dev.fnc314.tools.jobsearch.tracker.v1.Confidence
import dev.fnc314.tools.jobsearch.tracker.v1.DerivedFrom
import dev.fnc314.tools.jobsearch.tracker.v1.Region
import dev.fnc314.tools.jobsearch.tracker.v1.Tag

/**
 * Bidirectional mapper between Firestore namespaced canonical strings and Protocol Buffer enum representations.
 *
 * Provides type-safe lookups mapping serialized document strings (such as `"status:applied"` or `"work-mode:remote"`)
 * into generated [dev.fnc314.tools.jobsearch.tracker.v1] protobuf enum instances and back.
 */
public object EnumMapping {

    // ── TrackerStatus ──

    private val trackerStatusByString: Map<String, TrackerStatus> = mapOf(
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

    private val trackerStatusToString: Map<Int, String> = trackerStatusByString.values.associateBy(
        { it.number },
        { trackerStatusByString.entries.first { e -> e.value == it }.key }
    )

    /**
     * Converts a raw Firestore status string into its corresponding [TrackerStatus] enum value.
     *
     * @param raw The namespaced status string (e.g., `"status:applied"`).
     * @return The matching [TrackerStatus] enum or [TrackerStatus.TRACKER_STATUS_UNSPECIFIED] if unknown.
     */
    public fun trackerStatusToEnum(raw: String): TrackerStatus =
        trackerStatusByString[raw] ?: TrackerStatus.TRACKER_STATUS_UNSPECIFIED

    /**
     * Converts a [TrackerStatus] enum into its canonical namespaced Firestore string representation.
     *
     * @param enum The [TrackerStatus] proto enum instance.
     * @return The canonical Firestore status string (e.g. `"status:interview"`) or an empty string if unmapped.
     */
    public fun trackerStatusToString(enum: TrackerStatus): String =
        trackerStatusToString[enum.number] ?: ""

    // ── TrackerEventType ──

    private val trackerEventTypeByString: Map<String, TrackerEventType> = mapOf(
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

    /**
     * Converts a namespaced event type string into its corresponding [TrackerEventType] enum value.
     *
     * @param raw The namespaced event string (e.g., `"event:applied"`).
     * @return The matching [TrackerEventType] enum or [TrackerEventType.TRACKER_EVENT_TYPE_UNSPECIFIED].
     */
    public fun trackerEventTypeToEnum(raw: String): TrackerEventType =
        trackerEventTypeByString[raw] ?: TrackerEventType.TRACKER_EVENT_TYPE_UNSPECIFIED

    // ── ApplicationChannel ──

    private val applicationChannelByString: Map<String, ApplicationChannel> = mapOf(
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

    /**
     * Converts a namespaced application channel string into its corresponding [ApplicationChannel] enum value.
     *
     * @param raw The namespaced channel string (e.g., `"channel:linkedin"`).
     * @return The matching [ApplicationChannel] enum or [ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED].
     */
    public fun applicationChannelToEnum(raw: String): ApplicationChannel =
        applicationChannelByString[raw] ?: ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED

    // ── TrackerArtifactKind ──

    private val artifactKindByString: Map<String, TrackerArtifactKind> = mapOf(
        "artifact:cv" to TrackerArtifactKind.ARTIFACT_CV,
        "artifact:cover-letter" to TrackerArtifactKind.ARTIFACT_COVER_LETTER,
        "artifact:outcome" to TrackerArtifactKind.ARTIFACT_OUTCOME,
        "artifact:posting" to TrackerArtifactKind.ARTIFACT_POSTING,
        "artifact:assessment" to TrackerArtifactKind.ARTIFACT_ASSESSMENT,
    )

    /**
     * Converts a namespaced artifact kind string into its corresponding [TrackerArtifactKind] enum value.
     *
     * @param raw The namespaced artifact kind string (e.g., `"artifact:cv"`).
     * @return The matching [TrackerArtifactKind] enum or [TrackerArtifactKind.TRACKER_ARTIFACT_KIND_UNSPECIFIED].
     */
    public fun artifactKindToEnum(raw: String): TrackerArtifactKind =
        artifactKindByString[raw] ?: TrackerArtifactKind.TRACKER_ARTIFACT_KIND_UNSPECIFIED

    // ── WorkMode ──

    private val workModeByString: Map<String, WorkMode> = mapOf(
        "work-mode:onsite" to WorkMode.WORK_MODE_ONSITE,
        "work-mode:hybrid" to WorkMode.WORK_MODE_HYBRID,
        "work-mode:remote" to WorkMode.WORK_MODE_REMOTE,
    )

    /**
     * Converts a namespaced work mode string into its corresponding [WorkMode] enum value.
     *
     * @param raw The namespaced work mode string (e.g., `"work-mode:remote"`).
     * @return The matching [WorkMode] enum or [WorkMode.WORK_MODE_UNSPECIFIED].
     */
    public fun workModeToEnum(raw: String): WorkMode =
        workModeByString[raw] ?: WorkMode.WORK_MODE_UNSPECIFIED

    // ── EventActor ──

    private val eventActorByString: Map<String, EventActor> = mapOf(
        "actor:user" to EventActor.ACTOR_USER,
        "actor:agent" to EventActor.ACTOR_AGENT,
        "actor:email-sync" to EventActor.ACTOR_EMAIL_SYNC,
        "actor:import" to EventActor.ACTOR_IMPORT,
        "actor:scheduler" to EventActor.ACTOR_SCHEDULER,
    )

    /**
     * Converts a namespaced event actor string into its corresponding [EventActor] enum value.
     *
     * @param raw The namespaced event actor string (e.g., `"actor:agent"`).
     * @return The matching [EventActor] enum or [EventActor.EVENT_ACTOR_UNSPECIFIED].
     */
    public fun eventActorToEnum(raw: String): EventActor =
        eventActorByString[raw] ?: EventActor.EVENT_ACTOR_UNSPECIFIED

    // ── TrackerEditState ──

    private val editStateByString: Map<String, TrackerEditState> = mapOf(
        "proposal:pending" to TrackerEditState.EDIT_STATE_PENDING,
        "proposal:approved" to TrackerEditState.EDIT_STATE_APPROVED,
        "proposal:rejected" to TrackerEditState.EDIT_STATE_REJECTED,
        "proposal:expired" to TrackerEditState.EDIT_STATE_EXPIRED,
    )

    /**
     * Converts a namespaced tracker edit state string into its corresponding [TrackerEditState] enum value.
     *
     * @param raw The namespaced edit state string (e.g., `"proposal:pending"`).
     * @return The matching [TrackerEditState] enum or [TrackerEditState.TRACKER_EDIT_STATE_UNSPECIFIED].
     */
    public fun editStateToEnum(raw: String): TrackerEditState =
        editStateByString[raw] ?: TrackerEditState.TRACKER_EDIT_STATE_UNSPECIFIED

    // ── TrackerEditKind ──

    private val editKindByString: Map<String, TrackerEditKind> = mapOf(
        "proposal:email-sync" to TrackerEditKind.EDIT_KIND_EMAIL_SYNC,
        "proposal:sheet-sync" to TrackerEditKind.EDIT_KIND_SHEET_SYNC,
    )

    /**
     * Converts a namespaced tracker edit kind string into its corresponding [TrackerEditKind] enum value.
     *
     * @param raw The namespaced edit kind string (e.g., `"proposal:email-sync"`).
     * @return The matching [TrackerEditKind] enum or [TrackerEditKind.TRACKER_EDIT_KIND_UNSPECIFIED].
     */
    public fun editKindToEnum(raw: String): TrackerEditKind =
        editKindByString[raw] ?: TrackerEditKind.TRACKER_EDIT_KIND_UNSPECIFIED

    // ── Trigger ──

    private val triggerByString: Map<String, Trigger> = mapOf(
        "trigger:cron" to Trigger.TRIGGER_CRON,
        "trigger:application-created" to Trigger.TRIGGER_APPLICATION_CREATED,
        "trigger:application-updated" to Trigger.TRIGGER_APPLICATION_UPDATED,
        "trigger:manual" to Trigger.TRIGGER_MANUAL,
    )

    /**
     * Converts a namespaced trigger string into its corresponding [Trigger] enum value.
     *
     * @param raw The namespaced trigger string (e.g., `"trigger:cron"`).
     * @return The matching [Trigger] enum or [Trigger.TRIGGER_UNSPECIFIED].
     */
    public fun triggerToEnum(raw: String): Trigger =
        triggerByString[raw] ?: Trigger.TRIGGER_UNSPECIFIED

    // ── SyncKind ──

    private val syncKindByString: Map<String, SyncKind> = mapOf(
        "sync:email-collect" to SyncKind.SYNC_KIND_EMAIL_COLLECT,
    )

    /**
     * Converts a namespaced synchronization kind string into its corresponding [SyncKind] enum value.
     *
     * @param raw The namespaced sync kind string (e.g., `"sync:email-collect"`).
     * @return The matching [SyncKind] enum or [SyncKind.SYNC_KIND_UNSPECIFIED].
     */
    public fun syncKindToEnum(raw: String): SyncKind =
        syncKindByString[raw] ?: SyncKind.SYNC_KIND_UNSPECIFIED

    // ── SyncState ──

    private val syncStateByString: Map<String, SyncState> = mapOf(
        "sync:pending" to SyncState.SYNC_STATE_PENDING,
        "sync:claimed" to SyncState.SYNC_STATE_CLAIMED,
        "sync:done" to SyncState.SYNC_STATE_DONE,
        "sync:failed" to SyncState.SYNC_STATE_FAILED,
    )

    /**
     * Converts a namespaced synchronization state string into its corresponding [SyncState] enum value.
     *
     * @param raw The namespaced sync state string (e.g., `"sync:done"`).
     * @return The matching [SyncState] enum or [SyncState.SYNC_STATE_UNSPECIFIED].
     */
    public fun syncStateToEnum(raw: String): SyncState =
        syncStateByString[raw] ?: SyncState.SYNC_STATE_UNSPECIFIED

    // ── Confidence ──

    private val confidenceByString: Map<String, Confidence> = mapOf(
        "high" to Confidence.CONFIDENCE_HIGH,
        "medium" to Confidence.CONFIDENCE_MEDIUM,
        "low" to Confidence.CONFIDENCE_LOW,
    )

    /**
     * Converts an extraction confidence string into its corresponding [Confidence] enum value.
     *
     * @param raw The confidence level string (`"high"`, `"medium"`, or `"low"`).
     * @return The matching [Confidence] enum or [Confidence.CONFIDENCE_UNSPECIFIED].
     */
    public fun confidenceToEnum(raw: String): Confidence =
        confidenceByString[raw] ?: Confidence.CONFIDENCE_UNSPECIFIED

    // ── DerivedFrom ──

    private val derivedFromByString: Map<String, DerivedFrom> = mapOf(
        "manual" to DerivedFrom.DERIVED_MANUAL,
        "notes-regex" to DerivedFrom.DERIVED_NOTES_REGEX,
        "email" to DerivedFrom.DERIVED_EMAIL,
        "csv-import" to DerivedFrom.DERIVED_CSV_IMPORT,
    )

    /**
     * Converts a derivation source string into its corresponding [DerivedFrom] enum value.
     *
     * @param raw The derivation source identifier (e.g., `"email"` or `"manual"`).
     * @return The matching [DerivedFrom] enum or [DerivedFrom.DERIVED_FROM_UNSPECIFIED].
     */
    public fun derivedFromToEnum(raw: String): DerivedFrom =
        derivedFromByString[raw] ?: DerivedFrom.DERIVED_FROM_UNSPECIFIED

    // ── Region ──

    private val regionByString: Map<String, Region> = mapOf(
        "PIT" to Region.REGION_PIT,
        "NYC" to Region.REGION_NYC,
        "SF" to Region.REGION_SF,
        "LA" to Region.REGION_LA,
        "DC" to Region.REGION_DC,
        "REMOTE-US" to Region.REGION_REMOTE_US,
    )

    /**
     * Converts a geographic target region string into its corresponding [Region] enum value.
     *
     * @param raw The region code (e.g., `"PIT"`, `"NYC"`, or `"REMOTE-US"`).
     * @return The matching [Region] enum or [Region.REGION_UNSPECIFIED].
     */
    public fun regionToEnum(raw: String): Region =
        regionByString[raw] ?: Region.REGION_UNSPECIFIED

    // ── Tag ──

    private val tagByString: Map<String, Tag> = mapOf(
        "relocation:required" to Tag.RELOCATION_REQUIRED,
        "relocation:accepted" to Tag.RELOCATION_ACCEPTED,
        "relocation:declined" to Tag.RELOCATION_DECLINED,
        "compensation:above-super-commuter:NYC" to Tag.COMPENSATION_ABOVE_SUPER_COMMUTER_NYC,
        "compensation:above-super-commuter:SF" to Tag.COMPENSATION_ABOVE_SUPER_COMMUTER_SF,
        "compensation:below-nyc-floor" to Tag.COMPENSATION_BELOW_NYC_FLOOR,
        "compensation:below-band" to Tag.COMPENSATION_BELOW_BAND,
        "compensation:at-band" to Tag.COMPENSATION_AT_BAND,
        "compensation:above-band" to Tag.COMPENSATION_ABOVE_BAND,
        "compensation:equity-heavy" to Tag.COMPENSATION_EQUITY_HEAVY,
        "fit:strong" to Tag.FIT_STRONG,
        "fit:stretch" to Tag.FIT_STRETCH,
        "fit:gap-ios" to Tag.FIT_GAP_IOS,
        "fit:gap-cross-platform" to Tag.FIT_GAP_CROSS_PLATFORM,
        "process:take-home" to Tag.PROCESS_TAKE_HOME,
        "process:referral" to Tag.PROCESS_REFERRAL,
        "process:internal-contact" to Tag.PROCESS_INTERNAL_CONTACT,
        "process:no-response-30d" to Tag.PROCESS_NO_RESPONSE_30D,
    )

    /**
     * Converts a namespaced categorization tag string into its corresponding [Tag] enum value.
     *
     * @param raw The namespaced tag string (e.g., `"fit:strong"` or `"relocation:required"`).
     * @return The matching [Tag] enum or [Tag.TAG_UNSPECIFIED].
     */
    public fun tagToEnum(raw: String): Tag =
        tagByString[raw] ?: Tag.TAG_UNSPECIFIED
}
