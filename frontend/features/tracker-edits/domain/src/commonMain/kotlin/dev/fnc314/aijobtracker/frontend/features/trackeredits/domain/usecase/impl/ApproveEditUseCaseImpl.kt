package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.ApproveEditUseCase

internal class ApproveEditUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ApproveEditUseCase {
    override suspend fun invoke(params: ApproveEditUseCase.Params): TrackerEdit =
        repository.approveEdit(params.id, params.note)
}
