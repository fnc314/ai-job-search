package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase

interface GetArtifactSignedUrlUseCase : UseCase<GetArtifactSignedUrlUseCase.Params, String> {
    data class Params(val path: String)
}
