package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

public interface ExpireStaleEditsUseCase : UseCase<ExpireStaleEditsUseCase.Params, Int> {
    public class Params
}
