package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase

interface CreateLeadUseCase : UseCase<CreateLeadUseCase.Params, String> {
    data class Params(
        val company: String,
        val contact: String? = null,
        val channel: String? = null,
        val notes: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
    )
}
