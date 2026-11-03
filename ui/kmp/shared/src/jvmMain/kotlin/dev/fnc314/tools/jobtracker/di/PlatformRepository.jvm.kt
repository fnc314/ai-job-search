package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.data.firestore.JVMJobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.platform.FirebaseJVM

actual fun createJobApplicationRepository(): JobApplicationRepository {
    FirebaseJVM.initialize()
    return JVMJobApplicationRepository()
}
