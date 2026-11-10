package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Domain use case fetching all documents and artifacts attached to a specific job application.
 */
public interface LoadArtifactsUseCase : UseCase<LoadArtifactsUseCase.Params, List<TrackerArtifact>> {
    public data class Params(public val applicationId: String)
}
