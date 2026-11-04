package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository

actual fun createJobApplicationRepository(): JobApplicationRepository {
    return AndroidFirestoreJobApplicationRepository()
}
