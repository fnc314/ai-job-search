package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository

/** Platform-specific factory for creating the JobApplicationRepository. */
internal expect fun createJobApplicationRepository(): JobApplicationRepository
