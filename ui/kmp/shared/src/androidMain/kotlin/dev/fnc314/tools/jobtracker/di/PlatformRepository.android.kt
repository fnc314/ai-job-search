package dev.fnc314.tools.jobtracker.di

import android.app.Application
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.signin.data.PlatformSignInHandler

object AndroidApplicationContextHolder {
    var application: Application? = null
}

actual fun createJobApplicationRepository(): JobApplicationRepository {
    return AndroidFirestoreJobApplicationRepository()
}

actual fun createPlatformSignInHandler(): PlatformSignInHandler {
    val context = AndroidApplicationContextHolder.application
        ?: throw IllegalStateException("Android application context must be initialized before using PlatformSignInHandler")
    return PlatformSignInHandler(context)
}
