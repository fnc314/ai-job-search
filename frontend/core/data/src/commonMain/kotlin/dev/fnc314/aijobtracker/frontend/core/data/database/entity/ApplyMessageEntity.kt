package dev.fnc314.aijobtracker.frontend.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Local persistence entity storing an individual message or structured card within an Apply Assistant session.
 *
 * @property id Unique message identifier.
 * @property sessionId Owning [ApplySessionEntity.id].
 * @property sender Originator of the message (`USER`, `ASSISTANT`, `SYSTEM`).
 * @property messageType Presentation card kind (`TEXT`, `EVALUATION_CARD`, `YES_NO_PROMPT`, `ARTIFACTS_CARD`).
 * @property text Primary rendered message body text.
 * @property payloadJson Optional JSON metadata payload for structured cards.
 * @property timestamp Epoch milliseconds creation timestamp.
 */
@Entity(
    tableName = "apply_messages",
    indices = [
        Index(value = ["sessionId"])
    ],
)
public data class ApplyMessageEntity(
    @PrimaryKey public val id: String,
    public val sessionId: String,
    public val sender: String,
    public val messageType: String,
    public val text: String,
    public val payloadJson: String? = null,
    public val timestamp: Long = 0L,
)
