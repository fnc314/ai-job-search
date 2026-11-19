package dev.fnc314.aijobtracker.frontend.features.aiapply.domain.di

import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ConfirmApplicationSubmissionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.EvaluateJobPostingUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.GenerateApplicationArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ObserveArtifactJobUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.RefineJobEvaluationWithAiUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.ResetApplySessionUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.SyncDraftedArtifactsUseCase
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.ConfirmApplicationSubmissionUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.EvaluateJobPostingUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.GenerateApplicationArtifactsUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.ObserveApplySessionUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.ObserveArtifactJobUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.RefineJobEvaluationWithAiUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.ResetApplySessionUseCaseImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.usecase.impl.SyncDraftedArtifactsUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain use case bindings for the AI Apply assistant feature.
 */
public val aiApplyDomainModule: Module = module {
    factoryOf(::ObserveApplySessionUseCaseImpl) bind ObserveApplySessionUseCase::class
    factoryOf(::EvaluateJobPostingUseCaseImpl) bind EvaluateJobPostingUseCase::class
    factoryOf(::RefineJobEvaluationWithAiUseCaseImpl) bind RefineJobEvaluationWithAiUseCase::class
    factoryOf(::GenerateApplicationArtifactsUseCaseImpl) bind GenerateApplicationArtifactsUseCase::class
    factoryOf(::ObserveArtifactJobUseCaseImpl) bind ObserveArtifactJobUseCase::class
    factoryOf(::SyncDraftedArtifactsUseCaseImpl) bind SyncDraftedArtifactsUseCase::class
    factoryOf(::ConfirmApplicationSubmissionUseCaseImpl) bind ConfirmApplicationSubmissionUseCase::class
    factoryOf(::ResetApplySessionUseCaseImpl) bind ResetApplySessionUseCase::class
}
