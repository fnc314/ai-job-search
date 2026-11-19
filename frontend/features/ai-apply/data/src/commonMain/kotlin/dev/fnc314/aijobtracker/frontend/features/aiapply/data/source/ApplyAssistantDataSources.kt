package dev.fnc314.aijobtracker.frontend.features.aiapply.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import kotlinx.coroutines.flow.Flow

internal interface LocalApplyAssistantDataSource {
    fun observeLatestSession(): Flow<ApplySessionState?>
    suspend fun getLatestSession(): ApplySessionState?
    suspend fun getSession(sessionId: String): ApplySessionState?
    suspend fun saveSession(session: ApplySessionState)
    suspend fun clearSessions()
}

internal interface NetworkApplyAssistantDataSource {
    suspend fun evaluateDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): NetworkDeterministicResult

    suspend fun refineWithAi(
        applicationId: String,
        postingText: String,
        company: String,
        role: String,
        deterministicScore: Int,
        strengths: List<String>,
        gaps: List<String>,
        dealBreakers: List<String>,
    ): NetworkAiRankingResult

    suspend fun startArtifactJob(
        applicationId: String,
        company: String,
        role: String,
        postingText: String,
    ): ApplyArtifactJobState

    suspend fun getArtifactJob(jobId: String): ApplyArtifactJobState?
}

internal interface RemoteApplyAssistantDataSource {
    fun observeArtifactJob(jobId: String): Flow<ApplyArtifactJobState?>

    suspend fun syncApplicationStatus(
        applicationId: String,
        company: String,
        role: String,
        status: TrackerStatus,
        fitRating: Int?,
        jobUrl: String?,
        note: String,
        evaluation: ApplyEvaluationResult? = null,
        artifactJob: ApplyArtifactJobState? = null,
    )
}

internal data class NetworkDeterministicResult(
    val applicationId: String,
    val company: String,
    val role: String,
    val score: Int,
    val verdict: String,
    val strengths: List<String>,
    val gaps: List<String>,
    val dealBreakers: List<String>,
    val extractedText: String = "",
)

internal data class NetworkAiRankingResult(
    val score: Int,
    val verdict: String,
    val aiRationale: String,
    val strengths: List<String>,
    val gaps: List<String>,
    val dealBreakers: List<String>,
)
