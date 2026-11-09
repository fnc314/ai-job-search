package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.NetworkJobApplicationDetailsDataSource

internal class NetworkJobApplicationDetailsDataSourceImpl(
    private val networkDataSource: NetworkDataSource,
) : NetworkJobApplicationDetailsDataSource {
    override suspend fun getArtifactSignedUrl(path: String): String {
        return "https://storage.googleapis.com/jobsearch-artifacts/$path"
    }
}
