package dev.fnc314.aijobtracker.backend.ai.client

/**
 * Functional service interface for drafting tailored LaTeX CV and cover letter sources.
 */
public fun interface ArtifactDraftingService {
    /**
     * Drafts tailored CV and cover letter LaTeX documents for a target role and posting.
     *
     * @param request Drafting request containing company, role, and job description text.
     * @return [Result] wrapping generated [DraftingResponse] LaTeX sources.
     */
    public suspend operator fun invoke(request: DraftingRequest): Result<DraftingResponse>
}
