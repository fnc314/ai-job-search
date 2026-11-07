package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

public fun interface RemoteJobApplicationsListDataSource {
    public suspend operator fun invoke(): List<JobApplication>
}
