package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.repository.FirestoreJobApplicationRepository
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository

actual fun createJobApplicationRepository(): JobApplicationRepository {
    return FirestoreJobApplicationRepository()
}
