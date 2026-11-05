package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent

interface RecordEventUseCase : UseCase<RecordEventUseCase.Params, String> {
    data class Params(val event: TrackerEvent)
}
