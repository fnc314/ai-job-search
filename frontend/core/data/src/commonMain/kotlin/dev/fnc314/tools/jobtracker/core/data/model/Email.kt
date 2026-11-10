package dev.fnc314.tools.jobtracker.core.data.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

/**
 * Domain entity representing an email message serving as evidence for pipeline decisions.
 *
 * Grounded in top-level collection `emails/{messageId}`.
 *
 * @property id The unique Gmail message ID.
 * @property from The sender address or name.
 * @property subject The subject line of the email.
 * @property snippet Plain text or Markdown preview of the message content.
 * @property labels Gmail or provider label names attached to the message.
 * @property receivedAt ISO timestamp of message receipt.
 * @property status Evidentiary categorization [EmailStatus].
 * @property applicationId Associated application document ID, or null if unlinked/noise.
 * @property companyId Associated company identifier, if linked to an application.
 * @property rectifiedAt Timestamp of manual attribution or status override.
 * @property createdAt Timestamp when record was initially created.
 * @property updatedAt Timestamp when record was last updated.
 */
@Immutable
public data class Email(
    public val id: String,
    public val from: String,
    public val subject: String,
    public val snippet: String,
    public val labels: ImmutableList<String> = persistentListOf(),
    public val receivedAt: Instant?,
    public val status: EmailStatus = EmailStatus.UNCLASSIFIED,
    public val applicationId: String? = null,
    public val companyId: String? = null,
    public val rectifiedAt: Instant? = null,
    public val createdAt: Instant = Instant.DISTANT_PAST,
    public val updatedAt: Instant = Instant.DISTANT_PAST,
)
