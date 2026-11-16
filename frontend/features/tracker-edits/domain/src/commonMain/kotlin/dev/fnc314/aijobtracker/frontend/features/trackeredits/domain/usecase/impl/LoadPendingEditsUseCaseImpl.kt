package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.LoadPendingEditsUseCase

internal class LoadPendingEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : LoadPendingEditsUseCase {
    override suspend fun invoke(params: LoadPendingEditsUseCase.Params): List<TrackerEdit> =
        repository.getPendingEdits()
}
