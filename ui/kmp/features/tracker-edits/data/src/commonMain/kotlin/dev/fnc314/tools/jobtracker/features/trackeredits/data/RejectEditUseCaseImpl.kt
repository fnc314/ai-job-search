package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase

public class RejectEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : RejectEditUseCase {
    override suspend fun invoke(params: RejectEditUseCase.Params): TrackerEdit =
        repository.rejectEdit(params.id, params.reason)
}
