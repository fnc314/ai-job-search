package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.signin.data.PlatformSignInHandler

/** Platform-specific factory for creating the JobApplicationRepository. */
expect fun createJobApplicationRepository(): JobApplicationRepository

/** Platform-specific factory for creating the PlatformSignInHandler. */
expect fun createPlatformSignInHandler(): PlatformSignInHandler
