@file:OptIn(ExperimentalMaterial3Api::class)

package dev.fnc314.aijobtracker.frontend.features.aiapply.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.aijobtracker.frontend.core.artifacts.model.ArtifactsRepository
import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformFileOpener
import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformFileSaver
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.widgets.ErrorBannerWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.StatusBadge
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyArtifactJobState
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyChatMessage
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyEvaluationResult
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyInputMode
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageSender
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyMessageType
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.model.ApplyWorkflowStep
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_download
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_evaluate
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_new_session
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_no
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_open_application
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_review
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_action_yes
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_artifacts_cover_title
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_artifacts_cv_title
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_artifacts_outcome_title
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_artifacts_section_title
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_badge_ai_calibrated
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_badge_ai_refining
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_badge_deterministic
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_deal_breakers_header
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_download_error
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_download_saved
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_drafting_in_progress
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_gaps_header
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_company_label
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_description_label
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_description_placeholder
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_mode_description
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_mode_url
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_role_label
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_url_label
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_input_url_placeholder
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_score_label
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_strengths_header
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_welcome_subtitle
import dev.fnc314.aijobtracker.frontend.features_ai_apply_ui.generated.resources.apply_welcome_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

private const val SCORE_ANIMATION_DURATION_MS = 700
private const val DESCRIPTION_MIN_LINES = 6
private const val DESCRIPTION_MAX_LINES = 12

/**
 * Single-view Apply Assistant screen hosted directly inside the `Edits` (`Pencil`) destination.
 *
 * Implements the interactive `/apply` workflow:
 * 1. Dual input selector for Job Posting URL vs. Markdown Job Description.
 * 2. Instant Tier 1 deterministic fit evaluation (`RankScorer`, $0 cost) creating `RANKED` status.
 * 3. Asynchronous Tier 2 AI semantic calibration smoothly animating the fit score counter.
 * 4. Interactive asset drafting (`DRAFTING` -> `DRAFTED`) and submission confirmation (`SUBMITTED`).
 *
 * @param onOpenApplication Callback accepting an application ID to open its detail view.
 * @param onPreviewArtifact Callback accepting `(applicationId, artifactId)` to review an artifact in-app.
 * @param modifier Composable layout modifier.
 * @param applyViewModel Backing [ApplyAssistantViewModel] provided via Koin.
 */
@Composable
public fun AiApplyScreen(
    onOpenApplication: (String) -> Unit = {},
    onPreviewArtifact: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    applyViewModel: ApplyAssistantViewModel = koinViewModel(),
) {
    val state by applyViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val messages = state.session?.messages.orEmpty()
    val koin = getKoin()
    val artifactsRepository = remember(koin) { koin.getOrNull<ArtifactsRepository>() }
    val fileOpener = remember { PlatformFileOpener() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (state.isEvaluating || state.isSubmittingPrompt) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.errorMessage?.let { msg ->
            ErrorBannerWidget(
                message = msg,
                onRetry = { applyViewModel.onIntent(ApplyAssistantUiIntent.ClearError) },
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
            )
        }

        if (state.session == null) {
            ApplyInputSection(
                state = state,
                onIntent = applyViewModel::onIntent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                items(items = messages, key = { it.id }) { message ->
                    ApplyMessageItem(
                        message = message,
                        activeStep = state.session?.currentStep,
                        isAiRefining = state.isAiRefining,
                        isActionBusy = state.isSubmittingPrompt,
                        onOpenApplication = onOpenApplication,
                        onPreviewArtifact = onPreviewArtifact,
                        artifactsRepository = artifactsRepository,
                        fileOpener = fileOpener,
                        onRespondGenerate = { generate ->
                            applyViewModel.onIntent(ApplyAssistantUiIntent.RespondGenerateAssets(generate))
                        },
                        onRespondSubmitted = { submitted ->
                            applyViewModel.onIntent(ApplyAssistantUiIntent.RespondSubmitted(submitted))
                        },
                    )
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.medium),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = { applyViewModel.onIntent(ApplyAssistantUiIntent.ResetSession) },
                ) {
                    Text(stringResource(Res.string.apply_action_new_session))
                }
            }
        }
    }
}

@Composable
private fun ApplyInputSection(
    state: ApplyAssistantUiState,
    onIntent: (ApplyAssistantUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.apply_welcome_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.apply_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            FilterChip(
                selected = state.inputMode == ApplyInputMode.DESCRIPTION,
                onClick = { onIntent(ApplyAssistantUiIntent.SelectInputMode(ApplyInputMode.DESCRIPTION)) },
                label = { Text(stringResource(Res.string.apply_input_mode_description)) },
            )
            FilterChip(
                selected = state.inputMode == ApplyInputMode.URL,
                onClick = { onIntent(ApplyAssistantUiIntent.SelectInputMode(ApplyInputMode.URL)) },
                label = { Text(stringResource(Res.string.apply_input_mode_url)) },
            )
        }

        if (state.inputMode == ApplyInputMode.URL) {
            OutlinedTextField(
                value = state.urlInput,
                onValueChange = { onIntent(ApplyAssistantUiIntent.ChangeUrl(it)) },
                label = { Text(stringResource(Res.string.apply_input_url_label)) },
                placeholder = { Text(stringResource(Res.string.apply_input_url_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = state.descriptionInput,
                onValueChange = { onIntent(ApplyAssistantUiIntent.ChangeDescription(it)) },
                label = { Text(stringResource(Res.string.apply_input_description_label)) },
                placeholder = { Text(stringResource(Res.string.apply_input_description_placeholder)) },
                minLines = DESCRIPTION_MIN_LINES,
                maxLines = DESCRIPTION_MAX_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            OutlinedTextField(
                value = state.companyInput,
                onValueChange = { onIntent(ApplyAssistantUiIntent.ChangeCompany(it)) },
                label = { Text(stringResource(Res.string.apply_input_company_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.roleInput,
                onValueChange = { onIntent(ApplyAssistantUiIntent.ChangeRole(it)) },
                label = { Text(stringResource(Res.string.apply_input_role_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = { onIntent(ApplyAssistantUiIntent.SubmitEvaluation) },
            enabled = !state.isEvaluating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.apply_action_evaluate))
        }
    }
}

@Composable
private fun ApplyMessageItem(
    message: ApplyChatMessage,
    activeStep: ApplyWorkflowStep?,
    isAiRefining: Boolean,
    isActionBusy: Boolean,
    onOpenApplication: (String) -> Unit,
    onPreviewArtifact: (String, String) -> Unit,
    artifactsRepository: ArtifactsRepository?,
    fileOpener: PlatformFileOpener,
    onRespondGenerate: (Boolean) -> Unit,
    onRespondSubmitted: (Boolean) -> Unit,
) {
    when (message.messageType) {
        ApplyMessageType.TEXT -> {
            val isUser = message.sender == ApplyMessageSender.USER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                    )
                }
            }
        }

        ApplyMessageType.EVALUATION_CARD -> {
            val evaluation = message.evaluation ?: return
            EvaluationResultCard(
                evaluation = evaluation,
                isAiRefining = isAiRefining,
                onOpenApplication = onOpenApplication,
            )
        }

        ApplyMessageType.YES_NO_PROMPT -> {
            val isCurrentPrompt = activeStep == message.promptStep
            YesNoPromptCard(
                promptText = message.text,
                enabled = isCurrentPrompt && !isActionBusy,
                onYes = {
                    when (message.promptStep) {
                        ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION -> onRespondGenerate(true)
                        ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION -> onRespondSubmitted(true)
                        else -> Unit
                    }
                },
                onNo = {
                    when (message.promptStep) {
                        ApplyWorkflowStep.AWAITING_GENERATE_CONFIRMATION -> onRespondGenerate(false)
                        ApplyWorkflowStep.AWAITING_SUBMISSION_CONFIRMATION -> onRespondSubmitted(false)
                        else -> Unit
                    }
                },
            )
        }

        ApplyMessageType.ARTIFACTS_CARD -> {
            val job = message.artifactJob ?: return
            ArtifactsResultCard(
                job = job,
                onPreviewArtifact = onPreviewArtifact,
                onOpenApplication = onOpenApplication,
                artifactsRepository = artifactsRepository,
                fileOpener = fileOpener,
            )
        }
    }
}

@Composable
private fun EvaluationResultCard(
    evaluation: ApplyEvaluationResult,
    isAiRefining: Boolean,
    onOpenApplication: (String) -> Unit,
) {
    val animatedScore by animateIntAsState(
        targetValue = evaluation.score,
        animationSpec = tween(durationMillis = SCORE_ANIMATION_DURATION_MS),
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = evaluation.company,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = evaluation.role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(status = evaluation.status)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Text(
                    text = stringResource(Res.string.apply_score_label, animatedScore),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = when {
                                evaluation.isAiEvaluated -> stringResource(Res.string.apply_badge_ai_calibrated)
                                isAiRefining -> stringResource(Res.string.apply_badge_ai_refining)
                                else -> stringResource(Res.string.apply_badge_deterministic)
                            }
                        )
                    },
                )
            }

            if (evaluation.aiRationale.isNotBlank()) {
                Text(
                    text = evaluation.aiRationale,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (evaluation.strengths.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.apply_strengths_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = evaluation.strengths.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (evaluation.gaps.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.apply_gaps_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = evaluation.gaps.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (evaluation.dealBreakers.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.apply_deal_breakers_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = evaluation.dealBreakers.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (evaluation.applicationId.isNotBlank()) {
                TextButton(onClick = { onOpenApplication(evaluation.applicationId) }) {
                    Text(evaluation.applicationId)
                }
            }
        }
    }
}

@Composable
private fun YesNoPromptCard(
    promptText: String,
    enabled: Boolean,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = promptText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Button(
                    onClick = onYes,
                    enabled = enabled,
                ) {
                    Text(stringResource(Res.string.apply_action_yes))
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                OutlinedButton(
                    onClick = onNo,
                    enabled = enabled,
                ) {
                    Text(stringResource(Res.string.apply_action_no))
                }
            }
        }
    }
}

private data class ArtifactCardItem(
    val title: String,
    val path: String,
    val artifactId: String?,
)

@Composable
private fun ArtifactsResultCard(
    job: ApplyArtifactJobState,
    onPreviewArtifact: (String, String) -> Unit,
    onOpenApplication: (String) -> Unit,
    artifactsRepository: ArtifactsRepository?,
    fileOpener: PlatformFileOpener,
) {
    val scope = rememberCoroutineScope()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            if (job.status == "IN_PROGRESS" || job.status == "PENDING") {
                Text(
                    text = stringResource(Res.string.apply_drafting_in_progress),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    text = stringResource(Res.string.apply_artifacts_section_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                val items = buildList {
                    job.cvPdfUrl?.takeIf { it.isNotBlank() }?.let { path ->
                        add(
                            ArtifactCardItem(
                                title = stringResource(Res.string.apply_artifacts_cv_title),
                                path = path,
                                artifactId = job.cvArtifactId,
                            ),
                        )
                    }
                    job.coverLetterPdfUrl?.takeIf { it.isNotBlank() }?.let { path ->
                        add(
                            ArtifactCardItem(
                                title = stringResource(Res.string.apply_artifacts_cover_title),
                                path = path,
                                artifactId = job.coverLetterArtifactId,
                            ),
                        )
                    }
                    job.outcomeUrl?.takeIf { it.isNotBlank() }?.let { path ->
                        add(
                            ArtifactCardItem(
                                title = stringResource(Res.string.apply_artifacts_outcome_title),
                                path = path,
                                artifactId = job.outcomeArtifactId,
                            ),
                        )
                    }
                }

                items.forEach { item ->
                    val filename = item.path.substringAfterLast('/')
                    val savedLabel = stringResource(Res.string.apply_download_saved, filename)
                    val errorLabel = stringResource(Res.string.apply_download_error, filename)
                    ArtifactActionRow(
                        item = item,
                        canReview = job.applicationId.isNotBlank() && !item.artifactId.isNullOrBlank(),
                        onReview = {
                            val artId = item.artifactId ?: return@ArtifactActionRow
                            onPreviewArtifact(job.applicationId, artId)
                        },
                        onDownload = {
                            scope.launch {
                                feedbackMessage = downloadOrOpenArtifact(
                                    applicationId = job.applicationId,
                                    item = item,
                                    artifactsRepository = artifactsRepository,
                                    fileOpener = fileOpener,
                                    savedLabel = savedLabel,
                                    errorLabel = errorLabel,
                                )
                            }
                        },
                    )
                }

                feedbackMessage?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (job.applicationId.isNotBlank()) {
                    TextButton(onClick = { onOpenApplication(job.applicationId) }) {
                        Text(stringResource(Res.string.apply_action_open_application))
                    }
                }

                job.error?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactActionRow(
    item: ArtifactCardItem,
    canReview: Boolean,
    onReview: () -> Unit,
    onDownload: () -> Unit,
) {
    val filename = item.path.substringAfterLast('/')
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = filename,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = item.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                if (canReview) {
                    FilledTonalButton(onClick = onReview) {
                        Text(stringResource(Res.string.apply_action_review))
                    }
                }
                OutlinedButton(onClick = onDownload) {
                    Text(stringResource(Res.string.apply_action_download))
                }
            }
        }
    }
}

private suspend fun downloadOrOpenArtifact(
    applicationId: String,
    item: ArtifactCardItem,
    artifactsRepository: ArtifactsRepository?,
    fileOpener: PlatformFileOpener,
    savedLabel: String,
    errorLabel: String,
): String {
    if (item.path.startsWith("http://") || item.path.startsWith("https://")) {
        fileOpener.openUrl(item.path)
        return savedLabel
    }
    val artifactId = item.artifactId
    val artifact = if (applicationId.isNotBlank() && !artifactId.isNullOrBlank()) {
        runCatching { artifactsRepository?.getArtifact(applicationId, artifactId) }.getOrNull()
    } else {
        null
    }
    val remoteUrl = artifact?.externalUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    if (remoteUrl != null) {
        fileOpener.openUrl(remoteUrl)
        return savedLabel
    }
    val inlineContent = artifact?.content?.takeIf { it.isNotBlank() }
    if (inlineContent != null) {
        val result = PlatformFileSaver.saveText(inlineContent, artifact.filename)
        return if (result.isSuccess) savedLabel else errorLabel
    }
    fileOpener.openFile(item.path)
    return errorLabel
}
