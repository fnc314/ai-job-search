package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

public interface CreateLeadUseCase : UseCase<CreateLeadUseCase.Params, String> {
    public data class Params(
        public val company: String,
        public val contact: String? = null,
        public val channel: String? = null,
        public val notes: List<String> = emptyList(),
        public val tags: List<String> = emptyList(),
    )
}
