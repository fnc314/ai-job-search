package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.GetArtifactSignedUrlUseCase

public class GetArtifactSignedUrlUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getArtifactSignedUrl(params.path)
}
