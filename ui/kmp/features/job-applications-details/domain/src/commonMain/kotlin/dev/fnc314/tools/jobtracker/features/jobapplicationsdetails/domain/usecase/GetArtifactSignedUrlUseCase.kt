package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

interface GetArtifactSignedUrlUseCase : UseCase<GetArtifactSignedUrlUseCase.Params, String> {
    data class Params(val path: String)
}
