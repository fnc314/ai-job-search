package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case generating a time-limited signed HTTPS URL for accessing a Cloud Storage document artifact.
 */
public interface GetArtifactSignedUrlUseCase : UseCase<GetArtifactSignedUrlUseCase.Params, String> {

    /**
     * Parameter carrier specifying the artifact cloud storage location.
     *
     * @property path Relative object key path in the GCS bucket.
     */
    public data class Params(public val path: String)
}
