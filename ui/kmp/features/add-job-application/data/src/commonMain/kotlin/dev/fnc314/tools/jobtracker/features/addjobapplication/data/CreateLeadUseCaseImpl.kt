package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import kotlin.time.Clock

public class CreateLeadUseCaseImpl(
    private val repository: AddJobApplicationRepository,
) : CreateLeadUseCase {
    override suspend fun invoke(params: CreateLeadUseCase.Params): String {
        val channel = ApplicationChannel.fromRaw(params.channel).let {
            if (it == ApplicationChannel.UNSPECIFIED) ApplicationChannel.OTHER else it
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
