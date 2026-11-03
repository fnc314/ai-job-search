package dev.fnc314.tools.jobtracker.domain.impl.filter

import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus

object FilterUtil {

    private val closedStatuses = setOf(
        TrackerStatus.REJECTED,
        TrackerStatus.NO_RESPONSE,
        TrackerStatus.OFFER_DECLINED,
        TrackerStatus.APPLICATION_WITHDRAWN,
        TrackerStatus.HIRED,
    )

    fun filterOpen(applications: List<JobApplication>): List<JobApplication> =
        applications.filter { it.status !in closedStatuses }

    fun filterRejected(applications: List<JobApplication>): List<JobApplication> =
        applications.filter { it.status in closedStatuses }

    fun filterLeads(applications: List<JobApplication>): List<JobApplication> =
        applications.filter { it.status == TrackerStatus.DRAFTING }
}
