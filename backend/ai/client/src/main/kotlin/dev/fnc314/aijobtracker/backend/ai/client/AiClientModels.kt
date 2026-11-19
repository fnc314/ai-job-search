package dev.fnc314.aijobtracker.backend.ai.client

import kotlinx.serialization.Serializable

/**
 * Input payload for AI-refined semantic job fit ranking.
 */
@Serializable
public data class AiRankingRequest(
    /** Raw text or Markdown content of the target job posting. */
    public val postingText: String,
    /** Target employer name, if known. */
    public val company: String = "",
    /** Target job role title, if known. */
    public val title: String = "",
    /** Tier 1 deterministic baseline score computed by RankScorer. */
    public val deterministicScore: Int = 0,
    /** Baseline strengths identified deterministically. */
    public val strengths: List<String> = emptyList(),
    /** Baseline skill or experience gaps identified deterministically. */
    public val gaps: List<String> = emptyList(),
    /** Baseline deal-breakers identified deterministically. */
    public val dealBreakers: List<String> = emptyList(),
)

/**
 * Output payload returned from Tier 2 AI semantic job ranking.
 */
@Serializable
public data class AiRankingResponse(
    /** Calibrated overall fit score (0..100). */
    public val score: Int,
    /** Qualitative fit verdict (e.g. STRONG_MATCH, GOOD_FIT, STRETCH). */
    public val verdict: String,
    /** Always true when returned from the AI tier. */
    public val isAiEvaluated: Boolean = true,
    /** Concise AI explanation describing alignment and nuances. */
    public val aiRationale: String,
    /** Refined list of candidate strengths matched to this role. */
    public val strengths: List<String>,
    /** Refined list of candidate skill or experience gaps. */
    public val gaps: List<String>,
    /** Identified deal-breakers (e.g. consulting, React Native primary). */
    public val dealBreakers: List<String>,
    /** Estimated target compensation band or benchmark summary. */
    public val salaryEstimate: String = "",
)

/**
 * Input payload for tailoring CV and cover letter LaTeX sources.
 */
@Serializable
public data class DraftingRequest(
    /** Canonical Firestore application identifier associated with this draft, if already persisted. */
    public val applicationId: String = "",
    /** Target company name. */
    public val company: String,
    /** Target job title or role description. */
    public val role: String,
    /** Job posting text used to tailor keyword emphasis and alignment. */
    public val postingText: String,
    /** Optional fit score from evaluation. */
    public val fitScore: Int? = null,
    /** Evaluated candidate strengths to emphasize. */
    public val strengths: List<String> = emptyList(),
    /** Evaluated skill gaps to frame or mitigate honestly. */
    public val gaps: List<String> = emptyList(),
)

/**
 * Output payload containing tailored LaTeX document sources.
 */
@Serializable
public data class DraftingResponse(
    /** Complete compilable LaTeX source for the tailored CV. */
    public val cvTex: String,
    /** Complete compilable LaTeX source for the tailored cover letter. */
    public val coverLetterTex: String,
    /** Identifier of the model or engine used to draft the sources. */
    public val modelUsed: String,
)
