package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEvent
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.GetEventsUseCase

internal class GetEventsUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : GetEventsUseCase {
    override suspend fun invoke(params: GetEventsUseCase.Params): List<TrackerEvent> =
        repository.getEvents(params.applicationId)
}
