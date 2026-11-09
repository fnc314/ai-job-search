package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource

internal interface NetworkJobApplicationDetailsDataSource {
    public suspend fun getArtifactSignedUrl(path: String): String
}
