package dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Domain use case that resolves the full text content of an artifact
 * using a 4-tier pipeline: inline → external URL → GCS → local filesystem.
 */
public interface LoadArtifactContentUseCase : UseCase<LoadArtifactContentUseCase.Params, String> {
    public data class Params(
        public val applicationId: String,
        public val artifactId: String,
    )
}
