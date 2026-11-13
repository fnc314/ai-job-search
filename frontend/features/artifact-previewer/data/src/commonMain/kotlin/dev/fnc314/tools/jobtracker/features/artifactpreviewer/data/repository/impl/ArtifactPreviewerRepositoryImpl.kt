package dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository.impl

import dev.fnc314.tools.jobtracker.core.data.source.NetworkDataSource
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository.ArtifactPreviewerRepository

internal class ArtifactPreviewerRepositoryImpl(
    private val networkDataSource: NetworkDataSource,
) : ArtifactPreviewerRepository {
    override suspend fun fetchContent(url: String): String? =
        runCatching { networkDataSource.get(url) }.getOrNull()
}
