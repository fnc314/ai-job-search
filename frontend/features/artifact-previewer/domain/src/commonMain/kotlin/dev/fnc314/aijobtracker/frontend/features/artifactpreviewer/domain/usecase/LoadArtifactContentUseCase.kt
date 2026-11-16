package dev.fnc314.aijobtracker.frontend.features.artifactpreviewer.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase

/**
 * Domain use case that resolves the full text content of an artifact
 * using a 4-tier pipeline: inline → external URL → GCS → local filesystem.
 */
public fun interface LoadArtifactContentUseCase : UseCase<LoadArtifactContentUseCase.Params, String> {
    /**
     * Parameters specifying the target artifact to retrieve.
     *
     * @property applicationId Unique application identifier.
     * @property artifactId Unique artifact document identifier.
     */
    public data class Params(
        public val applicationId: String,
        public val artifactId: String,
    )
}
