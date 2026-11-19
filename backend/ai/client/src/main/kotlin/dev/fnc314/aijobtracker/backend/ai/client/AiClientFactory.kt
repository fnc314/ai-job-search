package dev.fnc314.aijobtracker.backend.ai.client

import dev.fnc314.aijobtracker.backend.ai.client.impl.DefaultAiRankingService
import dev.fnc314.aijobtracker.backend.ai.client.impl.DefaultArtifactDraftingService

/**
 * Factory providing concrete implementations of the AI ranking and artifact drafting services.
 */
public object AiClientFactory {
    /**
     * Creates an [AiRankingService] backed by generative AI with deterministic profile fallback.
     *
     * @return Configured [AiRankingService] instance.
     */
    public fun createAiRankingService(): AiRankingService = DefaultAiRankingService()

    /**
     * Creates an [ArtifactDraftingService] backed by generative AI with template-grounded fallback.
     *
     * @return Configured [ArtifactDraftingService] instance.
     */
    public fun createArtifactDraftingService(): ArtifactDraftingService = DefaultArtifactDraftingService()
}
