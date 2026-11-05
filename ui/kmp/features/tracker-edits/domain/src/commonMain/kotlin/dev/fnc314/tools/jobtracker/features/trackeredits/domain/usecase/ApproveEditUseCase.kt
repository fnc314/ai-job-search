package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

interface ApproveEditUseCase : UseCase<ApproveEditUseCase.Params, TrackerEdit> {
    data class Params(val id: String, val note: String? = null)
}
