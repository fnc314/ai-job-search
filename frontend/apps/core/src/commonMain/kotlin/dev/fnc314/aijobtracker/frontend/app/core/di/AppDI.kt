package dev.fnc314.aijobtracker.frontend.app.core.di

import dev.fnc314.aijobtracker.frontend.core.artifacts.di.coreArtifactsModule
import dev.fnc314.aijobtracker.frontend.core.data.di.coreDataModule
import dev.fnc314.aijobtracker.frontend.core.logging.di.coreLoggingModule
import dev.fnc314.aijobtracker.frontend.core.network.di.coreNetworkModule
import dev.fnc314.aijobtracker.frontend.core.widgets.di.coreWidgetsModule
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.di.addJobApplicationFeatureModule
import dev.fnc314.aijobtracker.frontend.features.artifactpreviewer.ui.di.artifactPreviewerFeatureModule
import dev.fnc314.aijobtracker.frontend.features.companieslist.ui.di.companiesListFeatureModule
import dev.fnc314.aijobtracker.frontend.features.companydetails.ui.di.companyDetailsFeatureModule
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.ui.di.jobApplicationDetailsFeatureModule
import dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui.di.jobApplicationsListFeatureModule
import dev.fnc314.aijobtracker.frontend.features.settings.ui.di.settingsFeatureModule
import dev.fnc314.aijobtracker.frontend.features.signin.ui.di.signInFeatureModule
import dev.fnc314.aijobtracker.frontend.features.trackeredits.ui.di.trackerEditsFeatureModule
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.module.Module as KoinModule
import org.koin.dsl.module

/**
 * Root Koin module annotation for application dependency graph validation.
 */
@Module
public class AppModule

/**
 * Primary application entry point for compile-time Koin verification.
 */
@KoinApplication(modules = [AppModule::class])
public class MainApplication

/**
 * Master Koin module assembling all core, data, network, and feature modules for the application runtime.
 */
public val appModule: KoinModule = module {
  includes(
    coreArtifactsModule,
    coreDataModule,
    coreLoggingModule,
    coreNetworkModule,
    coreWidgetsModule,
    signInFeatureModule,
    settingsFeatureModule,
    addJobApplicationFeatureModule,
    trackerEditsFeatureModule,
    jobApplicationsListFeatureModule,
    jobApplicationDetailsFeatureModule,
    companiesListFeatureModule,
    companyDetailsFeatureModule,
    artifactPreviewerFeatureModule,
  )
}
