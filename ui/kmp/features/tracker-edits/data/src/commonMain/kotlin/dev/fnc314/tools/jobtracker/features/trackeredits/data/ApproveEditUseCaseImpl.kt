package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.core.data.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase

internal class ApproveEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ApproveEditUseCase {
    override suspend fun invoke(params: ApproveEditUseCase.Params): TrackerEdit =
        repository.approveEdit(params.id, params.note)
}
