package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication

interface FilterJobApplicationsUseCase : UseCase<FilterJobApplicationsUseCase.Params, List<JobApplication>> {
    enum class FilterType { ALL, OPEN, LEADS, REJECTED }
    data class Params(val applications: List<JobApplication>, val filter: FilterType)
}
