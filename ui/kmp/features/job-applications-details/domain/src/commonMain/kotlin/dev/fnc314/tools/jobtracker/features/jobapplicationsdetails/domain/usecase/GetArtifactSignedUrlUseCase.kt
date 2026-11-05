package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

public interface GetArtifactSignedUrlUseCase : UseCase<GetArtifactSignedUrlUseCase.Params, String> {
    public data class Params(public val path: String)
}
