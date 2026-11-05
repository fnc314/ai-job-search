package dev.fnc314.tools.jobtracker.core.data.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "job_applications")
public data class JobApplicationEntity(
    @PrimaryKey public val id: String,
    public val company: String,
    public val role: String,
    public val status: String,
    public val fitScore: Int = 0,
    public val appliedDate: String? = null,
    public val updatedAt: String? = null,
)
