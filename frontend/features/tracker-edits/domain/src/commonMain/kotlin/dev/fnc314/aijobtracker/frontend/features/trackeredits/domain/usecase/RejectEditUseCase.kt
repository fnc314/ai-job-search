package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit

/**
 * Use case rejecting an edit proposal.
 */
public interface RejectEditUseCase : UseCase<RejectEditUseCase.Params, TrackerEdit> {

    /**
     * Parameter carrier for proposal rejection.
     *
     * @property id The proposal identifier.
     * @property reason Justification for rejection.
     */
    public data class Params(public val id: String, public val reason: String)
}
