package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase

internal class GetArtifactSignedUrlUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getArtifactSignedUrl(params.path)
}
