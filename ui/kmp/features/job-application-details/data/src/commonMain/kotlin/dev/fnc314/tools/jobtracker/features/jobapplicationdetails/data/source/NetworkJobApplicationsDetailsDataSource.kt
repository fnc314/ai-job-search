package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource

public interface NetworkJobApplicationsDetailsDataSource {
    public suspend fun getArtifactSignedUrl(path: String): String
}

internal class NetworkJobApplicationsDetailsDataSourceImpl(
    private val networkDataSource: NetworkDataSource? = null,
) : NetworkJobApplicationsDetailsDataSource {
    override suspend fun getArtifactSignedUrl(path: String): String {
        return "https://storage.googleapis.com/jobsearch-artifacts/$path"
    }
}
