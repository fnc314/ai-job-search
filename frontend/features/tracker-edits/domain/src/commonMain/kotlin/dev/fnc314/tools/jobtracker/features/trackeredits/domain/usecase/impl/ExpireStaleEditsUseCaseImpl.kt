package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl

import dev.fnc314.tools.jobtracker.features.trackeredits.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase

internal class ExpireStaleEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ExpireStaleEditsUseCase {
    override suspend fun invoke(params: ExpireStaleEditsUseCase.Params): Int =
        repository.expireStaleEdits()
}
