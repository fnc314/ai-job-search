package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public interface LoadJobApplicationUseCase : UseCase<LoadJobApplicationUseCase.Params, JobApplication?> {
    public data class Params(public val id: String)
}
