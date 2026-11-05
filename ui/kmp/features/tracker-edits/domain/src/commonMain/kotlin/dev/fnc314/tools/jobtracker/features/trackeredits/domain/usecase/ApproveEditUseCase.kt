package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit

public interface ApproveEditUseCase : UseCase<ApproveEditUseCase.Params, TrackerEdit> {
    public data class Params(public val id: String, public val note: String? = null)
}
