package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.TrackerEdit

interface RejectEditUseCase : UseCase<RejectEditUseCase.Params, TrackerEdit> {
    data class Params(val id: String, val reason: String)
}
