package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication

interface SortJobApplicationsUseCase : UseCase<SortJobApplicationsUseCase.Params, List<JobApplication>> {
    enum class SortColumn { DATE, COMPANY, STATUS, FIT, ROLE }
    data class Params(val applications: List<JobApplication>, val column: SortColumn, val ascending: Boolean)
}
