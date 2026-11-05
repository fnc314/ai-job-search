package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.repository.StorageRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.GetArtifactSignedUrlUseCase

internal class GetArtifactSignedUrlUseCaseImpl(
    private val repository: StorageRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getSignedUrl(params.path)
}
