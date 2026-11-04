package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.data.firestore.FirestoreJobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.signin.data.PlatformSignInHandler

actual fun createJobApplicationRepository(): JobApplicationRepository {
    return FirestoreJobApplicationRepository()
}

actual fun createPlatformSignInHandler(): PlatformSignInHandler = PlatformSignInHandler()
