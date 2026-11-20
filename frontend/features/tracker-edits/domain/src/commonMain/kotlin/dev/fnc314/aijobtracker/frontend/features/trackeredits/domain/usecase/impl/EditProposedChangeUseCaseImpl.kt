package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.EditProposedChangeUseCase

internal class EditProposedChangeUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : EditProposedChangeUseCase {

    override suspend fun invoke(params: EditProposedChangeUseCase.Params): TrackerEdit =
        repository.editProposedChange(
            proposalId = params.proposalId,
            itemIndex = params.itemIndex,
            newEventType = params.newEventType,
            newStatusTo = params.newStatusTo,
            reason = params.reason
        )
}
