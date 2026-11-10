package dev.fnc314.tools.jobtracker.core.data.database

import dev.fnc314.tools.jobtracker.core.data.database.entity.JobApplicationEntity
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.time.Instant

/**
 * Bidirectional mapper converting between domain [JobApplication] instances and SQLite [JobApplicationEntity] records.
 */
public object JobApplicationMapper {

    /**
     * Maps a domain [JobApplication] entity into a Room [JobApplicationEntity] suitable for SQLite persistence.
     *
     * Synthesizes a new [Uuid7] if [JobApplication.uuid] is null.
     *
     * @param domain The domain model to convert.
     * @return The resulting database entity.
     */
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
            createdAt = domain.createdAt.takeUnless { it == Instant.DISTANT_PAST },
            updatedAt = domain.updatedAt.takeUnless { it == Instant.DISTANT_PAST },
            jobApplicationUrl = domain.jobApplicationUrl,
            emailMessageIds = domain.emailMessageIds,
            appliedAt = domain.appliedAt,
        )
    }

    /**
     * Maps a Room [JobApplicationEntity] from SQLite into a rich domain [JobApplication] instance.
     *
     * Preserves primary key [JobApplicationEntity.uuid] and schema migration version.
     *
     * @param entity The database entity to convert.
     * @return The resulting domain model.
     */
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
            createdAt = entity.createdAt ?: Instant.DISTANT_PAST,
            updatedAt = entity.updatedAt ?: Instant.DISTANT_PAST,
            jobApplicationUrl = entity.jobApplicationUrl,
            emailMessageIds = entity.emailMessageIds,
            uuid = entity.uuid,
            schemaVersion = entity.schemaVersion,
            appliedAt = entity.appliedAt,
        )
    }

    /**
     * Maps a list of [JobApplicationEntity] records into domain [JobApplication] entities.
     *
     * @param entities Collection of database entities.
     * @return List of domain models.
     */
    public fun toDomainList(entities: List<JobApplicationEntity>): List<JobApplication> =
        entities.map { toDomain(it) }

    /**
     * Maps a list of domain [JobApplication] entities into Room [JobApplicationEntity] records.
     *
     * @param domains Collection of domain models.
     * @return List of database entities.
     */
    public fun toEntityList(domains: List<JobApplication>): List<JobApplicationEntity> =
        domains.map { toEntity(it) }
}
