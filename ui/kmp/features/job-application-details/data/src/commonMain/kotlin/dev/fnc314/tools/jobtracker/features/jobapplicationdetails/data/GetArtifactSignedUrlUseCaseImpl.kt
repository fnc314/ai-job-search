package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase

public class GetArtifactSignedUrlUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getArtifactSignedUrl(params.path)
}
