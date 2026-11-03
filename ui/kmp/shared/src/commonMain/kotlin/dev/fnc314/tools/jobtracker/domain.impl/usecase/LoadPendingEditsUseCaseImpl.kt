package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.domain.usecase.LoadPendingEditsUseCase

internal class LoadPendingEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : LoadPendingEditsUseCase {
    override suspend fun invoke(params: LoadPendingEditsUseCase.Params): List<TrackerEdit> =
        repository.getPendingEdits()
}
