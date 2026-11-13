package dev.fnc314.tools.jobtracker.core.logging.di

import dev.fnc314.tools.jobtracker.core.logging.AppLogger
import org.koin.core.module.Module
import org.koin.dsl.module

public val coreLoggingModule: Module = module {
    single { AppLogger }
}
