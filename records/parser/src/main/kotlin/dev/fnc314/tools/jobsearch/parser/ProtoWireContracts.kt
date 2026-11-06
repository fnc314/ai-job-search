package dev.fnc314.tools.jobsearch.parser

import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.Confidence
import dev.fnc314.tools.jobsearch.tracker.v1.DerivedFrom
import dev.fnc314.tools.jobsearch.tracker.v1.EventActor
import dev.fnc314.tools.jobsearch.tracker.v1.Region
import dev.fnc314.tools.jobsearch.tracker.v1.RoleLocation
import dev.fnc314.tools.jobsearch.tracker.v1.SyncKind
import dev.fnc314.tools.jobsearch.tracker.v1.SyncState
import dev.fnc314.tools.jobsearch.tracker.v1.Tag
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerArtifactKind
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditKind
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEditState
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerEventType
import dev.fnc314.tools.jobsearch.tracker.v1.TrackerStatus
import dev.fnc314.tools.jobsearch.tracker.v1.Trigger

/**
 * Canonical Firestore wire key for [TrackerStatus] proto enum entries.
 */
public val TrackerStatus.wireKey: String
    get() = when (this) {
        TrackerStatus.TRACKER_STATUS_UNSPECIFIED, TrackerStatus.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [TrackerEventType] proto enum entries.
 */
public val TrackerEventType.wireKey: String
    get() = when (this) {
        TrackerEventType.TRACKER_EVENT_TYPE_UNSPECIFIED, TrackerEventType.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [ApplicationChannel] proto enum entries.
 */
public val ApplicationChannel.wireKey: String
    get() = when (this) {
        ApplicationChannel.APPLICATION_CHANNEL_UNSPECIFIED, ApplicationChannel.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [TrackerArtifactKind] proto enum entries.
 */
public val TrackerArtifactKind.wireKey: String
    get() = when (this) {
        TrackerArtifactKind.TRACKER_ARTIFACT_KIND_UNSPECIFIED, TrackerArtifactKind.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [RoleLocation] proto enum entries.
 */
public val RoleLocation.wireKey: String
    get() = when (this) {
        RoleLocation.ROLE_LOCATION_UNSPECIFIED, RoleLocation.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [EventActor] proto enum entries.
 */
public val EventActor.wireKey: String
    get() = when (this) {
        EventActor.EVENT_ACTOR_UNSPECIFIED, EventActor.UNRECOGNIZED -> ""
        EventActor.ACTOR_USER -> "USER"
        EventActor.ACTOR_AGENT -> "AGENT"
        EventActor.ACTOR_EMAIL_SYNC -> "EMAIL_SYNC"
        EventActor.ACTOR_IMPORT -> "IMPORT"
        EventActor.ACTOR_SCHEDULER -> "SCHEDULER"
    }

/**
 * Canonical Firestore wire key for [TrackerEditState] proto enum entries.
 */
public val TrackerEditState.wireKey: String
    get() = when (this) {
        TrackerEditState.TRACKER_EDIT_STATE_UNSPECIFIED, TrackerEditState.UNRECOGNIZED -> ""
        TrackerEditState.EDIT_STATE_PENDING -> "PENDING"
        TrackerEditState.EDIT_STATE_APPROVED -> "APPROVED"
        TrackerEditState.EDIT_STATE_REJECTED -> "REJECTED"
        TrackerEditState.EDIT_STATE_EXPIRED -> "EXPIRED"
    }

/**
 * Canonical Firestore wire key for [TrackerEditKind] proto enum entries.
 */
public val TrackerEditKind.wireKey: String
    get() = when (this) {
        TrackerEditKind.TRACKER_EDIT_KIND_UNSPECIFIED, TrackerEditKind.UNRECOGNIZED -> ""
        TrackerEditKind.EDIT_KIND_EMAIL_SYNC -> "EMAIL_SYNC"
        TrackerEditKind.EDIT_KIND_SHEET_SYNC -> "SHEET_SYNC"
        TrackerEditKind.EDIT_KIND_LEAD -> "LEAD"
    }

/**
 * Canonical Firestore wire key for [Trigger] proto enum entries.
 */
public val Trigger.wireKey: String
    get() = when (this) {
        Trigger.TRIGGER_UNSPECIFIED, Trigger.UNRECOGNIZED -> ""
        Trigger.TRIGGER_CRON -> "CRON"
        Trigger.TRIGGER_APPLICATION_CREATED -> "APPLICATION_CREATED"
        Trigger.TRIGGER_APPLICATION_UPDATED -> "APPLICATION_UPDATED"
        Trigger.TRIGGER_MANUAL -> "MANUAL"
    }

/**
 * Canonical Firestore wire key for [SyncKind] proto enum entries.
 */
public val SyncKind.wireKey: String
    get() = when (this) {
        SyncKind.SYNC_KIND_UNSPECIFIED, SyncKind.UNRECOGNIZED -> ""
        SyncKind.SYNC_KIND_EMAIL_COLLECT -> "EMAIL_COLLECT"
    }

/**
 * Canonical Firestore wire key for [SyncState] proto enum entries.
 */
public val SyncState.wireKey: String
    get() = when (this) {
        SyncState.SYNC_STATE_UNSPECIFIED, SyncState.UNRECOGNIZED -> ""
        SyncState.SYNC_STATE_PENDING -> "PENDING"
        SyncState.SYNC_STATE_CLAIMED -> "CLAIMED"
        SyncState.SYNC_STATE_DONE -> "DONE"
        SyncState.SYNC_STATE_FAILED -> "FAILED"
    }

/**
 * Canonical Firestore wire key for [Confidence] proto enum entries.
 */
public val Confidence.wireKey: String
    get() = when (this) {
        Confidence.CONFIDENCE_UNSPECIFIED, Confidence.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [DerivedFrom] proto enum entries.
 */
public val DerivedFrom.wireKey: String
    get() = when (this) {
        DerivedFrom.DERIVED_FROM_UNSPECIFIED, DerivedFrom.UNRECOGNIZED -> ""
        DerivedFrom.DERIVED_MANUAL -> "MANUAL"
        DerivedFrom.DERIVED_NOTES_REGEX -> "NOTES_REGEX"
        DerivedFrom.DERIVED_EMAIL -> "EMAIL"
        DerivedFrom.DERIVED_CSV_IMPORT -> "CSV_IMPORT"
    }

/**
 * Canonical Firestore wire key for [Region] proto enum entries.
 */
public val Region.wireKey: String
    get() = when (this) {
        Region.REGION_UNSPECIFIED, Region.UNRECOGNIZED -> ""
        else -> name
    }

/**
 * Canonical Firestore wire key for [Tag] proto enum entries.
 */
public val Tag.wireKey: String
    get() = when (this) {
        Tag.TAG_UNSPECIFIED, Tag.UNRECOGNIZED -> ""
        else -> name
    }
