package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.RejectEditUseCase

internal class RejectEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : RejectEditUseCase {
    override suspend fun invoke(params: RejectEditUseCase.Params): TrackerEdit =
        repository.rejectEdit(params.id, params.reason)
}
