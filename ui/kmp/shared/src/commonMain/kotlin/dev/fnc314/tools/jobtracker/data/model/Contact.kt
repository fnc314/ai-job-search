package dev.fnc314.tools.jobtracker.data.model

data class Contact(
    val id: String,
    val name: String,
    val title: String?,
    val company: String?,
    val email: String?,
    val phone: String?,
    val linkedinUrl: String?,
    val applicationIds: List<String>,
)
