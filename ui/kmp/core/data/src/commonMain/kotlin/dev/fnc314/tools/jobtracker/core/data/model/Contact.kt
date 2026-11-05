package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Domain entity representing an individual contact person (recruiter, hiring manager, or referrer).
 *
 * @property id Unique identifier for this contact.
 * @property name Full name of the contact.
 * @property title Professional position or role (e.g., `"Senior Technical Recruiter"`).
 * @property company Associated organization or recruiting agency.
 * @property email Direct email address for correspondence.
 * @property phone Contact telephone number.
 * @property linkedinUrl Profile URL on LinkedIn.
 * @property applicationIds List of application slugs linked to interactions with this contact.
 */
public data class Contact(
    public val id: String,
    public val name: String,
    public val title: String?,
    public val company: String?,
    public val email: String?,
    public val phone: String?,
    public val linkedinUrl: String?,
    public val applicationIds: List<String>,
)
