package dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case expiring pending proposals whose review window has lapsed.
 */
public interface ExpireStaleEditsUseCase : UseCase<ExpireStaleEditsUseCase.Params, Int> {

    /**
     * Parameter carrier for triggering proposal expiration.
     */
    public class Params
}
