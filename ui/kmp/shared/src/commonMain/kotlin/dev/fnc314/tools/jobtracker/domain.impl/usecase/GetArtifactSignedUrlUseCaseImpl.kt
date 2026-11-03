package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.domain.repository.StorageRepository
import dev.fnc314.tools.jobtracker.domain.usecase.GetArtifactSignedUrlUseCase

internal class GetArtifactSignedUrlUseCaseImpl(
    private val repository: StorageRepository,
) : GetArtifactSignedUrlUseCase {
    override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String =
        repository.getSignedUrl(params.path)
}
