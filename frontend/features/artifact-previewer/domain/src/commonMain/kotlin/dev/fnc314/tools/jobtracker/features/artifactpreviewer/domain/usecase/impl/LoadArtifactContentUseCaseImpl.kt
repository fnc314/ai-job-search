package dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactsRepository
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileReader
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository.ArtifactPreviewerRepository
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase.LoadArtifactContentUseCase

internal class LoadArtifactContentUseCaseImpl(
    private val artifactsRepository: ArtifactsRepository,
    private val contentRepository: ArtifactPreviewerRepository,
) : LoadArtifactContentUseCase {

    override suspend fun invoke(params: LoadArtifactContentUseCase.Params): String {
        val artifact = artifactsRepository.getArtifact(params.applicationId, params.artifactId)
            ?: throw IllegalStateException("Artifact not found: ${params.artifactId}")

        val storage = artifact.storage
        val externalUrl = artifact.externalUrl
        val localPath = artifact.localPath
        val inlineContent = artifact.content
        var fetched = false
        var content: String? = null

        // Try 1: Use inline content stored in Firestore (best for mobile)
        if (!fetched && inlineContent != null) {
            content = inlineContent
            fetched = true
        }

        // Try 2: Fetch from external URL (signed URL or public link)
        if (!fetched && externalUrl != null) {
            val urlContent = contentRepository.fetchContent(externalUrl)
            if (urlContent != null) {
                content = urlContent
                fetched = true
            }
        }

        // Try 3: Fall back to GCS storage URL
        if (!fetched && storage != null) {
            val gcsUrl = "https://storage.googleapis.com/${storage.bucket}/${storage.path}"
            val gcsContent = contentRepository.fetchContent(gcsUrl)
            if (gcsContent != null) {
                content = gcsContent
                fetched = true
            }
        }

        // Try 4: Read from local filesystem (desktop/JVM only)
        if (!fetched && localPath != null) {
            val localContent = PlatformFileReader.readText(localPath)
            if (localContent != null) {
                content = localContent
                fetched = true
            }
        }

        if (!fetched) {
            throw IllegalStateException("No content available for artifact: ${artifact.filename}")
        }

        return content!!
    }
}
