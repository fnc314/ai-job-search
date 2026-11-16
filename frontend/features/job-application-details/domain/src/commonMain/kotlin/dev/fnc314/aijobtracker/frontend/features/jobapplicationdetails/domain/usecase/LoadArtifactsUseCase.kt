package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.artifacts.model.TrackerArtifact
import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase

/**
 * Domain use case fetching all documents and artifacts attached to a specific job application.
 */
public interface LoadArtifactsUseCase : UseCase<LoadArtifactsUseCase.Params, List<TrackerArtifact>> {
    /**
     * Parameters for loading artifacts.
     *
     * @property applicationId Unique application identifier.
     */
    public data class Params(public val applicationId: String)
}
