package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

/**
 * Domain repository contract querying the collection of job applications for list presentation.
 */
public fun interface JobApplicationsListRepository {

  /**
   * Retrieves all job applications, following a cache-first query strategy backed by the local database.
   *
   * @return List of all known [JobApplication] domain records.
   */
  public suspend operator fun invoke(): List<JobApplication>
}
