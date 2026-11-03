package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class Company(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val website: String?,
    val industry: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
