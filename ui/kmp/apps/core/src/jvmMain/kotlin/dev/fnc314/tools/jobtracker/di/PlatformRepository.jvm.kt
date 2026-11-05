package dev.fnc314.tools.jobtracker.di

import dev.fnc314.tools.jobtracker.core.data.repository.JVMJobApplicationRepository
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.platform.FirebaseJVM

internal actual fun createJobApplicationRepository(): JobApplicationRepository {
    FirebaseJVM.initialize()
    return JVMJobApplicationRepository()
}
