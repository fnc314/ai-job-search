package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.Confidence
import dev.fnc314.tools.jobsearch.tracker.v1.DerivedFrom
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.Region
import dev.fnc314.tools.jobsearch.tracker.v1.SyncKind
import dev.fnc314.tools.jobsearch.tracker.v1.SyncState
import dev.fnc314.tools.jobsearch.tracker.v1.Tag
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifactKind
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditKind
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditState
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobsearch.tracker.v1.Trigger
import dev.fnc314.tools.jobsearch.tracker.v1.WorkMode

/**
 * Canonical Firestore wire key for [TrackerStatus] proto enum entries.
 */
public val TrackerStatus.wireKey: String
    get() = when (this) {
        TrackerStatus.LEAD -> "status:lead"
        TrackerStatus.DRAFTING -> "status:drafting"
        TrackerStatus.APPLIED -> "status:applied"
        TrackerStatus.INTERVIEW -> "status:interview"
        TrackerStatus.OFFER_EXPECTED -> "status:offer"
        TrackerStatus.HIRED -> "status:hired"
        TrackerStatus.REJECTED -> "status:rejected"
        TrackerStatus.NO_RESPONSE -> "status:no-response"
        TrackerStatus.OFFER_DECLINED -> "status:offer-declined"
        TrackerStatus.APPLICATION_WITHDRAWN -> "status:withdrawn"
        TrackerStatus.TRACKER_STATUS_UNSPECIFIED, TrackerStatus.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [TrackerEventType] proto enum entries.
 */
public val TrackerEventType.wireKey: String
    get() = when (this) {
        TrackerEventType.APPLY -> "event:applied"
        TrackerEventType.ACKNOWLEDGE -> "event:acknowledged"
        TrackerEventType.RECRUITER_SCREEN -> "event:recruiter-screen"
        TrackerEventType.SCHEDULE_INTERVIEW -> "event:interview-scheduled"
        TrackerEventType.COMPLETE_INTERVIEW -> "event:interview-completed"
        TrackerEventType.ASSESS -> "event:assessment"
        TrackerEventType.VISIT_ONSITE -> "event:onsite"
        TrackerEventType.OFFER -> "event:offer"
        TrackerEventType.ACCEPT_OFFER -> "event:offer-accepted"
        TrackerEventType.DECLINE_OFFER -> "event:offer-declined"
        TrackerEventType.REJECT -> "event:rejection"
        TrackerEventType.GHOST -> "event:no-response"
        TrackerEventType.WITHDRAW -> "event:withdrawn"
        TrackerEventType.SEND_FOLLOW_UP -> "event:follow-up-sent"
        TrackerEventType.TAILOR_DOCUMENT -> "event:document-tailored"
        TrackerEventType.RECALCULATE_FIT -> "event:fit-recalculated"
        TrackerEventType.ADD_ARTIFACT -> "event:artifact-added"
        TrackerEventType.ADD_NOTE -> "event:note-added"
        TrackerEventType.IMPORT -> "event:imported"
        TrackerEventType.CORRECT_STATUS -> "event:status-corrected"
        TrackerEventType.RECRUITER_OUTREACH -> "event:recruiter-outreach"
        TrackerEventType.TRACKER_EVENT_TYPE_UNSPECIFIED, TrackerEventType.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [ApplicationChannel] proto enum entries.
 */
public val ApplicationChannel.wireKey: String
    get() = when (this) {
        ApplicationChannel.GREENHOUSE -> "channel:greenhouse"
        ApplicationChannel.WORKDAY -> "channel:workday"
        ApplicationChannel.LEVER -> "channel:lever"
        ApplicationChannel.ASHBY -> "channel:ashby"
        ApplicationChannel.SMARTRECRUITERS -> "channel:smartrecruiters"
        ApplicationChannel.LINKEDIN -> "channel:linkedin"
        ApplicationChannel.REFERRAL -> "channel:referral"
        ApplicationChannel.DIRECT -> "channel:direct"
        ApplicationChannel.OTHER -> "channel:other"
        ApplicationChannel.OUTREACH_LINKEDIN -> "channel:outreach:linkedin"
        ApplicationChannel.OUTREACH_EMAIL -> "channel:outreach:email"
        ApplicationChannel.OUTREACH_REFERRAL -> "channel:outreach:referral"
        ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED, ApplicationChannel.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [TrackerArtifactKind] proto enum entries.
 */
public val TrackerArtifactKind.wireKey: String
    get() = when (this) {
        TrackerArtifactKind.ARTIFACT_CV -> "artifact:cv"
        TrackerArtifactKind.ARTIFACT_COVER_LETTER -> "artifact:cover-letter"
        TrackerArtifactKind.ARTIFACT_OUTCOME -> "artifact:outcome"
        TrackerArtifactKind.ARTIFACT_POSTING -> "artifact:posting"
        TrackerArtifactKind.ARTIFACT_ASSESSMENT -> "artifact:assessment"
        TrackerArtifactKind.TRACKER_ARTIFACT_KIND_UNSPECIFIED, TrackerArtifactKind.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [WorkMode] proto enum entries.
 */
public val WorkMode.wireKey: String
    get() = when (this) {
        WorkMode.WORK_MODE_ONSITE -> "work-mode:onsite"
        WorkMode.WORK_MODE_HYBRID -> "work-mode:hybrid"
        WorkMode.WORK_MODE_REMOTE -> "work-mode:remote"
        WorkMode.WORK_MODE_UNSPECIFIED, WorkMode.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [EventActor] proto enum entries.
 */
public val EventActor.wireKey: String
    get() = when (this) {
        EventActor.ACTOR_USER -> "actor:user"
        EventActor.ACTOR_AGENT -> "actor:agent"
        EventActor.ACTOR_EMAIL_SYNC -> "actor:gmail-sync"
        EventActor.ACTOR_IMPORT -> "actor:import"
        EventActor.ACTOR_SCHEDULER -> "actor:scheduler"
        EventActor.EVENT_ACTOR_UNSPECIFIED, EventActor.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [TrackerEditState] proto enum entries.
 */
public val TrackerEditState.wireKey: String
    get() = when (this) {
        TrackerEditState.EDIT_STATE_PENDING -> "proposal:pending"
        TrackerEditState.EDIT_STATE_APPROVED -> "proposal:approved"
        TrackerEditState.EDIT_STATE_REJECTED -> "proposal:rejected"
        TrackerEditState.EDIT_STATE_EXPIRED -> "proposal:expired"
        TrackerEditState.TRACKER_EDIT_STATE_UNSPECIFIED, TrackerEditState.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [TrackerEditKind] proto enum entries.
 */
public val TrackerEditKind.wireKey: String
    get() = when (this) {
        TrackerEditKind.EDIT_KIND_EMAIL_SYNC -> "proposal:gmail-sync"
        TrackerEditKind.EDIT_KIND_SHEET_SYNC -> "proposal:sheet-sync"
        TrackerEditKind.EDIT_KIND_LEAD -> "proposal:lead"
        TrackerEditKind.TRACKER_EDIT_KIND_UNSPECIFIED, TrackerEditKind.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [Trigger] proto enum entries.
 */
public val Trigger.wireKey: String
    get() = when (this) {
        Trigger.TRIGGER_CRON -> "trigger:cron"
        Trigger.TRIGGER_APPLICATION_CREATED -> "trigger:application-created"
        Trigger.TRIGGER_APPLICATION_UPDATED -> "trigger:application-updated"
        Trigger.TRIGGER_MANUAL -> "trigger:manual"
        Trigger.TRIGGER_UNSPECIFIED, Trigger.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [SyncKind] proto enum entries.
 */
public val SyncKind.wireKey: String
    get() = when (this) {
        SyncKind.SYNC_KIND_EMAIL_COLLECT -> "sync:gmail-collect"
        SyncKind.SYNC_KIND_UNSPECIFIED, SyncKind.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [SyncState] proto enum entries.
 */
public val SyncState.wireKey: String
    get() = when (this) {
        SyncState.SYNC_STATE_PENDING -> "sync:pending"
        SyncState.SYNC_STATE_CLAIMED -> "sync:claimed"
        SyncState.SYNC_STATE_DONE -> "sync:done"
        SyncState.SYNC_STATE_FAILED -> "sync:failed"
        SyncState.SYNC_STATE_UNSPECIFIED, SyncState.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [Confidence] proto enum entries.
 */
public val Confidence.wireKey: String
    get() = when (this) {
        Confidence.CONFIDENCE_HIGH -> "high"
        Confidence.CONFIDENCE_MEDIUM -> "medium"
        Confidence.CONFIDENCE_LOW -> "low"
        Confidence.CONFIDENCE_UNSPECIFIED, Confidence.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [DerivedFrom] proto enum entries.
 */
public val DerivedFrom.wireKey: String
    get() = when (this) {
        DerivedFrom.DERIVED_MANUAL -> "manual"
        DerivedFrom.DERIVED_NOTES_REGEX -> "notes-regex"
        DerivedFrom.DERIVED_EMAIL -> "gmail"
        DerivedFrom.DERIVED_CSV_IMPORT -> "csv-import"
        DerivedFrom.DERIVED_FROM_UNSPECIFIED, DerivedFrom.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [Region] proto enum entries.
 */
public val Region.wireKey: String
    get() = when (this) {
        Region.REGION_PIT -> "PIT"
        Region.REGION_NYC -> "NYC"
        Region.REGION_SF -> "SF"
        Region.REGION_LA -> "LA"
        Region.REGION_DC -> "DC"
        Region.REGION_REMOTE_US -> "REMOTE-US"
        Region.REGION_UNSPECIFIED, Region.UNRECOGNIZED -> ""
    }

/**
 * Canonical Firestore wire key for [Tag] proto enum entries.
 */
public val Tag.wireKey: String
    get() = when (this) {
        Tag.RELOCATION_REQUIRED -> "relocation:required"
        Tag.RELOCATION_ACCEPTED -> "relocation:accepted"
        Tag.RELOCATION_DECLINED -> "relocation:declined"
        Tag.COMPENSATION_ABOVE_SUPER_COMMUTER_NYC -> "compensation:above-super-commuter:NYC"
        Tag.COMPENSATION_ABOVE_SUPER_COMMUTER_SF -> "compensation:above-super-commuter:SF"
        Tag.COMPENSATION_BELOW_NYC_FLOOR -> "compensation:below-nyc-floor"
        Tag.COMPENSATION_BELOW_BAND -> "compensation:below-band"
        Tag.COMPENSATION_AT_BAND -> "compensation:at-band"
        Tag.COMPENSATION_ABOVE_BAND -> "compensation:above-band"
        Tag.COMPENSATION_EQUITY_HEAVY -> "compensation:equity-heavy"
        Tag.FIT_STRONG -> "fit:strong"
        Tag.FIT_STRETCH -> "fit:stretch"
        Tag.FIT_GAP_IOS -> "fit:gap-ios"
        Tag.FIT_GAP_CROSS_PLATFORM -> "fit:gap-cross-platform"
        Tag.PROCESS_TAKE_HOME -> "process:take-home"
        Tag.PROCESS_REFERRAL -> "process:referral"
        Tag.PROCESS_INTERNAL_CONTACT -> "process:internal-contact"
        Tag.PROCESS_NO_RESPONSE_30D -> "process:no-response-30d"
        Tag.PROCESS_RECRUITER_OUTREACH -> "process:recruiter:outreach"
        Tag.TAG_UNSPECIFIED, Tag.UNRECOGNIZED -> ""
    }
