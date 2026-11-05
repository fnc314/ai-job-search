package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

public data class Company(
    public val id: String,
    public val name: String,
    public val aliases: List<String>,
    public val website: String?,
    public val industry: String?,
    public val createdAt: Instant?,
    public val updatedAt: Instant?,
)
