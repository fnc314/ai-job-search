package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

internal fun interface RemoteJobApplicationsListDataSource {
    public suspend operator fun invoke(): List<JobApplication>
}
