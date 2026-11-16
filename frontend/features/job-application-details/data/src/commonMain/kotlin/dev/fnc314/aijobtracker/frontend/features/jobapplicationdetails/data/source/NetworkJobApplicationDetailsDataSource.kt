package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.data.source

import dev.fnc314.aijobtracker.frontend.core.data.source.NetworkDataSource

internal interface NetworkJobApplicationDetailsDataSource {
    suspend fun getArtifactSignedUrl(path: String): String
}
