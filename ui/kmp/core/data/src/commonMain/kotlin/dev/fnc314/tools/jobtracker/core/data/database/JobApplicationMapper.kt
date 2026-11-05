package dev.fnc314.tools.jobtracker.core.data.database

import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7

public object JobApplicationMapper {

    public fun toEntity(domain: JobApplication): JobApplicationEntity {
        return JobApplicationEntity(
            uuid = domain.uuid ?: Uuid7.generate(),
            slugId = domain.id,
            schemaVersion = domain.schemaVersion,
            company = domain.company,
            role = domain.role,
            location = domain.location,
            status = domain.status,
            channel = domain.channel,
            jobId = domain.jobId,
            jobUrl = domain.jobUrl,
            description = domain.description,
            tags = domain.tags,
            contactIds = domain.contactIds,
            fitScore = domain.fitScore,
            notesRendered = domain.notesRendered,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            jobApplicationUrl = domain.jobApplicationUrl,
        )
    }

    public fun toDomain(entity: JobApplicationEntity): JobApplication {
        return Factory.createJobApplication(
            id = entity.slugId,
            company = entity.company,
            role = entity.role,
            location = entity.location,
            status = entity.status,
            channel = entity.channel,
            jobId = entity.jobId,
            jobUrl = entity.jobUrl,
            description = entity.description,
            tags = entity.tags,
            contactIds = entity.contactIds,
            fitRating = entity.fitScore,
            notesRendered = entity.notesRendered,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            jobApplicationUrl = entity.jobApplicationUrl,
            uuid = entity.uuid,
            schemaVersion = entity.schemaVersion,
        )
    }

    public fun toDomainList(entities: List<JobApplicationEntity>): List<JobApplication> =
        entities.map { toDomain(it) }

    public fun toEntityList(domains: List<JobApplication>): List<JobApplicationEntity> =
        domains.map { toEntity(it) }
}
