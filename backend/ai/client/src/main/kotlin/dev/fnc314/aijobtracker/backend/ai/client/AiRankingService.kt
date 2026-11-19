package dev.fnc314.aijobtracker.backend.ai.client

/**
 * Functional service interface for AI-refined semantic job fit evaluation.
 */
public fun interface AiRankingService {
    /**
     * Evaluates a job posting using an AI model, refining the deterministic baseline score.
     *
     * @param request Evaluation request carrying posting text and Tier 1 deterministic baseline.
     * @return [Result] wrapping the calibrated [AiRankingResponse].
     */
    public suspend operator fun invoke(request: AiRankingRequest): Result<AiRankingResponse>
}
