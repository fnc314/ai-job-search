package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.di.Factory
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.CreateLeadUseCase
import kotlin.time.Clock
import kotlin.time.Instant

internal class CreateLeadUseCaseImpl(
    private val repository: JobApplicationRepository,
) : CreateLeadUseCase {
    override suspend fun invoke(params: CreateLeadUseCase.Params): String {
        val channel = when (params.channel?.lowercase()) {
            "linkedin" -> ApplicationChannel.LINKEDIN
            "referral" -> ApplicationChannel.REFERRAL
            "direct" -> ApplicationChannel.DIRECT
            else -> ApplicationChannel.OTHER
        }
        val app = Factory.createJobApplication(
            id = Clock.System.now().toString(),
            company = params.company,
            status = TrackerStatus.DRAFTING,
            channel = channel,
            tags = params.tags,
            notesRendered = params.notes.joinToString("\n"),
        )
        return repository.saveJobApplication(app)
    }
}
