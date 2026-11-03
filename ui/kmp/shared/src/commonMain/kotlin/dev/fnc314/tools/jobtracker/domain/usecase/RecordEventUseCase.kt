package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.TrackerEvent

interface RecordEventUseCase : UseCase<RecordEventUseCase.Params, String> {
    data class Params(val event: TrackerEvent)
}
