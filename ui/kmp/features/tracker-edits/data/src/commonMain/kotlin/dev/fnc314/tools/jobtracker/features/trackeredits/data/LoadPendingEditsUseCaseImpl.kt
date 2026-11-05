package dev.fnc314.tools.jobtracker.features.trackeredits.data

import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase

public class LoadPendingEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : LoadPendingEditsUseCase {
    override suspend fun invoke(params: LoadPendingEditsUseCase.Params): List<TrackerEdit> =
        repository.getPendingEdits()
}
