package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public interface CreateJobApplicationUseCase : UseCase<CreateJobApplicationUseCase.Params, String> {
    public data class Params(public val application: JobApplication)
}
