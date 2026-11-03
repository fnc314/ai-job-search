package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.domain.usecase.ApproveEditUseCase

internal class ApproveEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ApproveEditUseCase {
    override suspend fun invoke(params: ApproveEditUseCase.Params): TrackerEdit =
        repository.approveEdit(params.id, params.note)
}
