package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase

internal class ExpireStaleEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ExpireStaleEditsUseCase {
    override suspend fun invoke(params: ExpireStaleEditsUseCase.Params): Int =
        repository.expireStaleEdits()
}
