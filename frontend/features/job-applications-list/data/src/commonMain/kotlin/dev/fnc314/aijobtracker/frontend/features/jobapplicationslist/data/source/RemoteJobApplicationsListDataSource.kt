package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.JobApplication

internal fun interface RemoteJobApplicationsListDataSource {
    suspend operator fun invoke(): List<JobApplication>
}
