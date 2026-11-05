package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case capturing an informal unsubmitted opportunity lead in [dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus.DRAFTING] status.
 */
public interface CreateLeadUseCase : UseCase<CreateLeadUseCase.Params, String> {

    /**
     * Parameter carrier for creating an opportunity lead.
     *
     * @property company Hiring organization name.
     * @property contact Recruiter or internal referral contact name.
     * @property channel Sourcing channel identifier string.
     * @property notes Freeform strategy or initial outreach notes.
     * @property tags Categorization tags.
     */
    public data class Params(
        public val company: String,
        public val contact: String? = null,
        public val channel: String? = null,
        public val notes: List<String> = emptyList(),
        public val tags: List<String> = emptyList(),
    )
}
