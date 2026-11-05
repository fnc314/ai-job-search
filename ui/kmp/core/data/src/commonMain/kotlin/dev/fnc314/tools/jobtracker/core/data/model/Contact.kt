package dev.fnc314.tools.jobtracker.core.data.model

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
