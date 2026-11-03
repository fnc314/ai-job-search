package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class TrackerEvent(
    val id: String,
    val applicationId: String,
    val type: TrackerEventType,
    val statusFrom: TrackerStatus?,
    val statusTo: TrackerStatus?,
    val note: String?,
    val emailMessageId: String?,
    val actor: EventActor,
    val derivedFrom: DerivedFrom,
    val confidence: Confidence,
    val createdAt: Instant?,
)
