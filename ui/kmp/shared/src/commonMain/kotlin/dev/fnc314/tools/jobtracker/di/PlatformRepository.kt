package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository

/** Platform-specific factory for creating the JobApplicationRepository. */
expect fun createJobApplicationRepository(): JobApplicationRepository
