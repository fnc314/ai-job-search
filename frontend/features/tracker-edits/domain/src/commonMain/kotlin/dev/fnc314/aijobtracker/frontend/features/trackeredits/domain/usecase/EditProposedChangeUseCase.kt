package dev.fnc314.aijobtracker.frontend.features.trackeredits.domain.usecase

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEdit
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEventType
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.data.repository.UseCase

/**
 * Use case permitting manual correction or override of a staged proposal item prior to approval.
 */
public fun interface EditProposedChangeUseCase : UseCase<EditProposedChangeUseCase.Params, TrackerEdit> {

    /**
     * Parameter carrier for editing a proposed transition.
     *
     * @property proposalId Identifier of the staged proposal.
     * @property itemIndex Index of the change item within the proposal.
     * @property newEventType Updated event type classification.
     * @property newStatusTo Optional updated target funnel status.
     * @property reason Human rationale or review note.
     */
    public data class Params(
        public val proposalId: String,
        public val itemIndex: Int,
        public val newEventType: TrackerEventType,
        public val newStatusTo: TrackerStatus? = null,
        public val reason: String? = null,
    )
}
