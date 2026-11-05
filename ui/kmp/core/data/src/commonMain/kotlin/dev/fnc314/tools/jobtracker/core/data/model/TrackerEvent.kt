package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

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
    public val createdAt: Instant?,
)
