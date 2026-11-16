package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit

/**
 * Use case approving an edit proposal and mutating the target application document.
 */
public interface ApproveEditUseCase : UseCase<ApproveEditUseCase.Params, TrackerEdit> {

    /**
     * Parameter carrier for proposal approval.
     *
     * @property id The proposal identifier.
     * @property note Optional approval note.
     */
    public data class Params(public val id: String, public val note: String? = null)
}
