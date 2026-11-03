package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.domain.usecase.RejectEditUseCase

internal class RejectEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : RejectEditUseCase {
    override suspend fun invoke(params: RejectEditUseCase.Params): TrackerEdit =
        repository.rejectEdit(params.id, params.reason)
}
