package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

/**
 * An append-only audit event capturing a discreet state transition or activity in an application's history.
 *
 * In accordance with the system invariant, applications are updated by appending immutable events
 * to their respective subcollections, preserving a verifiable timeline.
 *
 * @property id Unique event identifier.
 * @property applicationId Target application slug to which this event belongs.
 * @property type Categorical event kind [TrackerEventType] (e.g. Apply, Acknowledge, Recruiter Screen).
 * @property statusFrom Previous lifecycle status [TrackerStatus] before this event occurred.
 * @property statusTo Resulting lifecycle status [TrackerStatus] after this event concluded.
 * @property note Descriptive contextual notes or recruiter feedback accompanying this event.
 * @property emailMessageId Linked email identifier if this event originated from correspondence.
 * @property actor The initiator or system component [EventActor] responsible for the event.
 * @property derivedFrom Derivation source [DerivedFrom] indicating how this event was captured.
 * @property confidence Machine extraction confidence rating [Confidence].
 * @property createdAt Timestamp when this event occurred.
 * @see TrackerEventType
 * @see TrackerStatus
 * @see EventActor
 */
public data class TrackerEvent(
    public val id: String,
    public val applicationId: String,
    public val type: TrackerEventType,
    public val statusFrom: TrackerStatus?,
    public val statusTo: TrackerStatus?,
    public val note: String?,
    public val emailMessageId: String?,
    public val actor: EventActor,
    public val derivedFrom: DerivedFrom,
    public val confidence: Confidence,
    public val createdAt: Instant = Instant.DISTANT_PAST,
)
