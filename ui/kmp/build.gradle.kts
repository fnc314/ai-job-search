plugins {
    alias(libs.plugins.dokka)
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.local.multiplatform.library) apply false
    alias(libs.plugins.local.kotlin.multiplatform) apply false
    alias(libs.plugins.local.android.kmp.library) apply false
    alias(libs.plugins.local.compose) apply false
    alias(libs.plugins.local.dependencies.compose) apply false
    alias(libs.plugins.local.dependencies.serialization) apply false
    alias(libs.plugins.local.dependencies.koin) apply false
}

dependencies {
    // Type-Safe Project Accessors exclusively for KMP subprojects
    dokka(projects.coreData)
    dokka(projects.coreDesignSystem)
    dokka(projects.coreNetwork)
    dokka(projects.coreWidgets)
    dokka(projects.coreNavigation)
    dokka(projects.featuresSignInDomain)
    dokka(projects.featuresSignInData)
    dokka(projects.featuresSignInUi)
    dokka(projects.featuresJobApplicationsListDomain)
    dokka(projects.featuresJobApplicationsListData)
    dokka(projects.featuresJobApplicationsListUi)
    dokka(projects.featuresJobApplicationsDetailsDomain)
    dokka(projects.featuresJobApplicationsDetailsData)
    dokka(projects.featuresJobApplicationsDetailsUi)
    dokka(projects.featuresSettingsDomain)
    dokka(projects.featuresSettingsData)
    dokka(projects.featuresSettingsUi)
    dokka(projects.featuresAddJobApplicationDomain)
    dokka(projects.featuresAddJobApplicationData)
    dokka(projects.featuresAddJobApplicationUi)
    dokka(projects.featuresTrackerEditsDomain)
    dokka(projects.featuresTrackerEditsData)
    dokka(projects.featuresTrackerEditsUi)
    dokka(projects.appsCore)

    // Included Builds bundled directly into 1 unified Dokka set (substituted by Gradle)
    dokka(libs.records.parser)
    dokka(libs.records.proto)
    dokka(libs.project.plugins)
}

dokka {
    dokkaPublications.html {
        moduleName.set("Job Search & Tracker Multiplatform Documentation")
        outputDirectory.set(layout.projectDirectory.dir("dokka"))
    }
}
