package dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplySessionState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository.ApplyAssistantRepository
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ConfirmApplicationSubmissionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.EvaluateJobPostingUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.GenerateApplicationArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveArtifactJobUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.RefineJobEvaluationWithAiUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ResetApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.SyncDraftedArtifactsUseCase
import kotlinx.coroutines.flow.Flow

internal class ObserveApplySessionUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : ObserveApplySessionUseCase {
    override fun invoke(): Flow<ApplySessionState?> = repository.observeLatestSession()
}

internal class EvaluateJobPostingUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : EvaluateJobPostingUseCase {
    override suspend fun invoke(params: EvaluateJobPostingUseCase.Params): ApplySessionState =
        repository.evaluateDeterministic(
            url = params.url,
            text = params.text,
            company = params.company,
            role = params.role,
        )
}

internal class RefineJobEvaluationWithAiUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : RefineJobEvaluationWithAiUseCase {
    override suspend fun invoke(sessionId: String): ApplySessionState =
        repository.refineWithAi(sessionId)
}

internal class GenerateApplicationArtifactsUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : GenerateApplicationArtifactsUseCase {
    override suspend fun invoke(params: GenerateApplicationArtifactsUseCase.Params): ApplySessionState =
        repository.respondToGenerateAssets(
            sessionId = params.sessionId,
            generate = params.generate,
        )
}

internal class ObserveArtifactJobUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : ObserveArtifactJobUseCase {
    override fun invoke(jobId: String): Flow<ApplyArtifactJobState?> =
        repository.observeArtifactJob(jobId)
}

internal class SyncDraftedArtifactsUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : SyncDraftedArtifactsUseCase {
    override suspend fun invoke(params: SyncDraftedArtifactsUseCase.Params): ApplySessionState =
        repository.applyArtifactJobUpdate(
            sessionId = params.sessionId,
            jobState = params.jobState,
        )
}

internal class ConfirmApplicationSubmissionUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : ConfirmApplicationSubmissionUseCase {
    override suspend fun invoke(params: ConfirmApplicationSubmissionUseCase.Params): ApplySessionState =
        repository.confirmSubmission(
            sessionId = params.sessionId,
            submitted = params.submitted,
        )
}

internal class ResetApplySessionUseCaseImpl(
    private val repository: ApplyAssistantRepository,
) : ResetApplySessionUseCase {
    override suspend fun invoke() {
        repository.resetSession()
    }
}
