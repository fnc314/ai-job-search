package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.repository.AndroidFirestoreJobApplicationRepository
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository

internal actual fun createJobApplicationRepository(): JobApplicationRepository {
    return AndroidFirestoreJobApplicationRepository()
}
