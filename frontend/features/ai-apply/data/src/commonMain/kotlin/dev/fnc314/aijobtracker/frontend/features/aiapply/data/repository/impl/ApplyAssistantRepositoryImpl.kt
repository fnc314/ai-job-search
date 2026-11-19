package dev.fnc314.aijobtracker.frontend.features.aiapply.data.repository.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.data.model.Uuid7
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.LocalApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkDeterministicResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.RemoteApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl.CanonicalArtifactHelper
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyChatMessage
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyInputMode
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageSender
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageType
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyWorkflowStep
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository.ApplyAssistantRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val POLL_INTERVAL_MS = 2_500L
private const val MAX_PREVIEW_CHARS = 280

internal class ApplyAssistantRepositoryImpl(
    private val localDataSource: LocalApplyAssistantDataSource,
    private val networkDataSource: NetworkApplyAssistantDataSource,
    private val remoteDataSource: RemoteApplyAssistantDataSource,
) : ApplyAssistantRepository {

    override fun observeLatestSession(): Flow<ApplySessionState?> =
        localDataSource.observeLatestSession()

    override suspend fun evaluateDeterministic(
        url: String?,
        text: String?,
        company: String?,
        role: String?,
    ): ApplySessionState {
        val now = Clock.System.now().toEpochMilliseconds()
        val eval = networkDataSource.evaluateDeterministic(url, text, company, role)
        remoteDataSource.syncApplicationStatus(
            applicationId = eval.applicationId,
            company = eval.company,
            role = eval.role,
            status = TrackerStatus.RANKED,
            fitRating = eval.score,
            jobUrl = url,
            note = "Evaluated fit score ${eval.score}/100 (${eval.verdict})",
        )
        val evalModel = eval.toEvaluationResult()
        val session = ApplySessionState(
            id = Uuid7.generate().toString(),
            inputMode = if (!url.isNullOrBlank()) ApplyInputMode.URL else ApplyInputMode.DESCRIPTION,
            postingUrl = url.orEmpty(),
            postingText = text?.takeIf { it.isNotBlank() } ?: eval.extractedText,
            company = eval.company,
            role = eval.role,
            evaluation = evalModel,
            currentStep = ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION,
            messages = buildInitialMessages(url, text, eval.company, eval.role, evalModel, now),
            updatedAt = now,
        )
        localDataSource.saveSession(session)
        return session
    }

    private fun NetworkDeterministicResult.toEvaluationResult(): ApplyEvaluationResult =
        ApplyEvaluationResult(
            applicationId = applicationId,
            company = company,
            role = role,
            score = score,
            deterministicScore = score,
            aiScore = null,
            isAiEvaluated = false,
            verdict = verdict,
            strengths = strengths,
            gaps = gaps,
            dealBreakers = dealBreakers,
            status = TrackerStatus.RANKED,
        )

    private fun buildInitialMessages(
        url: String?,
        text: String?,
        company: String,
        role: String,
        evalModel: ApplyEvaluationResult,
        now: Long,
    ): List<ApplyChatMessage> {
        val userDisplayText = if (!url.isNullOrBlank()) url else text.orEmpty().take(MAX_PREVIEW_CHARS)
        return listOf(
            newChatMessage(ApplyMessageSender.USER, ApplyMessageType.TEXT, userDisplayText, now),
            ApplyChatMessage(
                id = Uuid7.generate().toString(),
                sender = ApplyMessageSender.ASSISTANT,
                messageType = ApplyMessageType.EVALUATION_CARD,
                text = "$company — $role",
                evaluation = evalModel,
                timestamp = now + 1,
            ),
            ApplyChatMessage(
                id = Uuid7.generate().toString(),
                sender = ApplyMessageSender.ASSISTANT,
                messageType = ApplyMessageType.YES_NO_PROMPT,
                text = "Generate tailored CV & Cover Letter assets?",
                promptStep = ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION,
                timestamp = now + 2,
            ),
        )
    }

    private fun newChatMessage(
        sender: ApplyMessageSender,
        messageType: ApplyMessageType,
        text: String,
        timestamp: Long,
    ): ApplyChatMessage = ApplyChatMessage(
        id = Uuid7.generate().toString(),
        sender = sender,
        messageType = messageType,
        text = text,
        timestamp = timestamp,
    )

    override suspend fun refineWithAi(sessionId: String): ApplySessionState {
        val current = localDataSource.getSession(sessionId) ?: localDataSource.getLatestSession()
            ?: error("Apply session not found: $sessionId")
        val existingEval = current.evaluation ?: return current
        val posting = current.postingText.ifBlank { "${current.company} ${current.role} ${current.postingUrl}" }
        val ai = networkDataSource.refineWithAi(
            applicationId = existingEval.applicationId,
            postingText = posting,
            company = current.company,
            role = current.role,
            deterministicScore = existingEval.deterministicScore,
            strengths = existingEval.strengths,
            gaps = existingEval.gaps,
            dealBreakers = existingEval.dealBreakers,
        )
        val updatedEval = existingEval.copy(
            score = ai.score,
            aiScore = ai.score,
            isAiEvaluated = true,
            verdict = ai.verdict,
            aiRationale = ai.aiRationale,
            strengths = ai.strengths,
            gaps = ai.gaps,
            dealBreakers = ai.dealBreakers,
        )
        val updatedMessages = current.messages.map { msg ->
            if (msg.messageType == ApplyMessageType.EVALUATION_CARD) {
                msg.copy(evaluation = updatedEval)
            } else {
                msg
            }
        }
        val updated = current.copy(
            evaluation = updatedEval,
            messages = updatedMessages,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        localDataSource.saveSession(updated)
        return updated
    }

    override suspend fun respondToGenerateAssets(
        sessionId: String,
        generate: Boolean,
    ): ApplySessionState {
        val current = localDataSource.getSession(sessionId) ?: localDataSource.getLatestSession()
            ?: error("Apply session not found: $sessionId")
        val eval = current.evaluation ?: error("Missing evaluation in session: $sessionId")
        val now = Clock.System.now().toEpochMilliseconds()
        if (!generate) return declineGenerateAssets(current, eval, now)

        val job = networkDataSource.startArtifactJob(
            applicationId = eval.applicationId,
            company = current.company,
            role = current.role,
            postingText = current.postingText.ifBlank { current.postingUrl },
        )
        remoteDataSource.syncApplicationStatus(
            applicationId = eval.applicationId,
            company = current.company,
            role = current.role,
            status = TrackerStatus.DRAFTING,
            fitRating = eval.score,
            jobUrl = current.postingUrl.ifBlank { null },
            note = "Drafting tailored CV and cover letter (${job.jobId})",
        )
        val updated = current.copy(
            evaluation = eval.copy(status = TrackerStatus.DRAFTING),
            artifactJob = job,
            currentStep = ApplyWorkflowStep.DRAFTING,
            messages = current.messages + listOf(
                newChatMessage(ApplyMessageSender.USER, ApplyMessageType.TEXT, "Yes — generate tailored assets", now),
                ApplyChatMessage(
                    id = Uuid7.generate().toString(),
                    sender = ApplyMessageSender.ASSISTANT,
                    messageType = ApplyMessageType.ARTIFACTS_CARD,
                    text = "Drafting CV & Cover Letter…",
                    artifactJob = job,
                    timestamp = now + 1,
                ),
            ),
            updatedAt = now,
        )
        localDataSource.saveSession(updated)
        return updated
    }

    private suspend fun declineGenerateAssets(
        current: ApplySessionState,
        eval: ApplyEvaluationResult,
        now: Long,
    ): ApplySessionState {
        val updated = current.copy(
            evaluation = eval.copy(status = TrackerStatus.RANKED),
            currentStep = ApplyWorkflowStep.AWAITING_INPUT,
            messages = current.messages + listOf(
                newChatMessage(ApplyMessageSender.USER, ApplyMessageType.TEXT, "No — keep tracked as Ranked", now),
                newChatMessage(ApplyMessageSender.SYSTEM, ApplyMessageType.TEXT, "Tracked as RANKED (${eval.score}/100).", now + 1),
            ),
            updatedAt = now,
        )
        localDataSource.saveSession(updated)
        return updated
    }

    override fun observeArtifactJob(jobId: String): Flow<ApplyArtifactJobState?> = channelFlow {
        var pollJob: kotlinx.coroutines.Job? = null
        val remoteJob = launch {
            remoteDataSource.observeArtifactJob(jobId)
                .catch { /* ignore remote listener errors; pollJob continues */ }
                .collect { state ->
                    if (state != null) {
                        send(state)
                        if (state.status == "SUCCESS" || state.status == "FAILED") {
                            pollJob?.cancel()
                        }
                    }
                }
        }
        pollJob = launch {
            while (true) {
                val fetched = networkDataSource.getArtifactJob(jobId)
                if (fetched != null) {
                    send(fetched)
                    if (fetched.status == "SUCCESS" || fetched.status == "FAILED") {
                        remoteJob.cancel()
                        break
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
        remoteJob.join()
        pollJob.join()
    }

    override suspend fun applyArtifactJobUpdate(
        sessionId: String,
        jobState: ApplyArtifactJobState,
    ): ApplySessionState {
        val current = localDataSource.getSession(sessionId) ?: localDataSource.getLatestSession()
            ?: error("Apply session not found: $sessionId")
        val eval = current.evaluation ?: return current
        val now = Clock.System.now().toEpochMilliseconds()
        val isSuccess = jobState.status == "SUCCESS"
        val spec = CanonicalArtifactHelper.forJob(current.company, current.role, eval.applicationId)
        val normalizedJob = if (isSuccess) {
            jobState.copy(
                applicationId = jobState.applicationId.ifBlank { eval.applicationId },
                cvArtifactId = jobState.cvArtifactId ?: spec.cvArtifactId,
                cvPdfUrl = jobState.cvPdfUrl ?: spec.cvPath,
                coverLetterArtifactId = jobState.coverLetterArtifactId ?: spec.coverArtifactId,
                coverLetterPdfUrl = jobState.coverLetterPdfUrl ?: spec.coverPath,
                outcomeArtifactId = jobState.outcomeArtifactId ?: spec.outcomeArtifactId,
                outcomeUrl = jobState.outcomeUrl ?: spec.outcomePath,
            )
        } else {
            jobState
        }
        if (isSuccess) {
            remoteDataSource.syncApplicationStatus(
                applicationId = eval.applicationId,
                company = current.company,
                role = current.role,
                status = TrackerStatus.DRAFTED,
                fitRating = eval.score,
                jobUrl = current.postingUrl.ifBlank { null },
                note = "Tailored CV and cover letter drafted (${normalizedJob.jobId})",
                evaluation = eval,
                artifactJob = normalizedJob,
            )
        }
        val updatedEval = eval.copy(status = if (isSuccess) TrackerStatus.DRAFTED else eval.status)
        val updated = current.copy(
            evaluation = updatedEval,
            artifactJob = normalizedJob,
            currentStep = if (isSuccess) ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION else current.currentStep,
            messages = buildUpdatedArtifactMessages(current.messages, normalizedJob, updatedEval, isSuccess, now),
            updatedAt = now,
        )
        localDataSource.saveSession(updated)
        return updated
    }

    private fun buildUpdatedArtifactMessages(
        messages: List<ApplyChatMessage>,
        jobState: ApplyArtifactJobState,
        updatedEval: ApplyEvaluationResult,
        isSuccess: Boolean,
        now: Long,
    ): List<ApplyChatMessage> {
        val mapped = messages.map { msg ->
            when (msg.messageType) {
                ApplyMessageType.ARTIFACTS_CARD -> msg.copy(
                    text = if (isSuccess) "Tailored artifacts ready" else msg.text,
                    artifactJob = jobState,
                )
                ApplyMessageType.EVALUATION_CARD -> msg.copy(evaluation = updatedEval)
                else -> msg
            }
        }
        val hasPrompt = mapped.any {
            it.messageType == ApplyMessageType.YES_NO_PROMPT &&
                it.promptStep == ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION
        }
        return if (isSuccess && !hasPrompt) {
            mapped + ApplyChatMessage(
                id = Uuid7.generate().toString(),
                sender = ApplyMessageSender.ASSISTANT,
                messageType = ApplyMessageType.YES_NO_PROMPT,
                text = "Did you submit the application?",
                promptStep = ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION,
                timestamp = now,
            )
        } else {
            mapped
        }
    }

    override suspend fun confirmSubmission(
        sessionId: String,
        submitted: Boolean,
    ): ApplySessionState {
        val current = localDataSource.getSession(sessionId) ?: localDataSource.getLatestSession()
            ?: error("Apply session not found: $sessionId")
        val eval = current.evaluation ?: return current
        val now = Clock.System.now().toEpochMilliseconds()

        val targetStatus = if (submitted) TrackerStatus.SUBMITTED else TrackerStatus.DRAFTED
        remoteDataSource.syncApplicationStatus(
            applicationId = eval.applicationId,
            company = current.company,
            role = current.role,
            status = targetStatus,
            fitRating = eval.score,
            jobUrl = current.postingUrl.ifBlank { null },
            note = if (submitted) "Application submitted by user" else "Draft prepared; awaiting user submission",
        )

        val updatedEval = eval.copy(status = targetStatus)
        val updatedMessages = current.messages + listOf(
            ApplyChatMessage(
                id = Uuid7.generate().toString(),
                sender = ApplyMessageSender.USER,
                messageType = ApplyMessageType.TEXT,
                text = if (submitted) "Yes — submitted!" else "Not yet",
                timestamp = now,
            ),
            ApplyChatMessage(
                id = Uuid7.generate().toString(),
                sender = ApplyMessageSender.SYSTEM,
                messageType = ApplyMessageType.TEXT,
                text = if (submitted) {
                    "Application status updated to SUBMITTED."
                } else {
                    "Application status saved as DRAFTED."
                },
                timestamp = now + 1,
            ),
        )
        val updated = current.copy(
            evaluation = updatedEval,
            currentStep = if (submitted) ApplyWorkflowStep.SUBMITTED else ApplyWorkflowStep.DRAFTED,
            messages = updatedMessages,
            updatedAt = now,
        )
        localDataSource.saveSession(updated)
        return updated
    }

    override suspend fun resetSession() {
        localDataSource.clearSessions()
    }
}
