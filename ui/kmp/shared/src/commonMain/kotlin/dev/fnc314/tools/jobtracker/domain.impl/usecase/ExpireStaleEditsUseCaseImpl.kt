package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.domain.repository.TrackerEditsRepository
import dev.fnc314.tools.jobtracker.domain.usecase.ExpireStaleEditsUseCase

internal class ExpireStaleEditsUseCaseImpl(
    private val repository: TrackerEditsRepository,
) : ExpireStaleEditsUseCase {
    override suspend fun invoke(params: ExpireStaleEditsUseCase.Params): Int =
        repository.expireStaleEdits()
}
