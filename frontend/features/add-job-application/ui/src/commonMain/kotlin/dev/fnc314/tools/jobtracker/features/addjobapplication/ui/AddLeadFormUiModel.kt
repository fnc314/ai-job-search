package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Form presentation model capturing input values for recording a prospective opportunity lead.
 *
 * @property company Name of the potential employer organization.
 * @property contact Key recruiter, hiring manager, or referrer name and info.
 * @property role Proposed or discussed role title.
 * @property channel Originating outreach or referral channel.
 * @property notes Chronological or qualitative notes regarding the lead.
 * @property tags Comma-delimited tags for filtering.
 * @property validationError User-facing validation error message, or `null` if valid.
 */
@Immutable
public data class AddLeadFormUiModel(
    public val company: String = "",
    public val contact: String = "",
    public val role: String = "",
    public val channel: String = "",
    public val notes: ImmutableList<String> = persistentListOf(),
    public val tags: String = "",
    public val validationError: String? = null,
)
