@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalGridApi::class)

package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.EmptyStateWidget
import dev.fnc314.tools.jobtracker.core.widgets.ErrorBannerWidget
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import dev.fnc314.tools.jobtracker.core.widgets.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCard
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCardAction
import dev.fnc314.tools.jobtracker.core.widgets.TopLevelCardSurfaceVariant
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.Res
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_approve
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_approve_for_app
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_expire_stale
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_refresh
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_reject
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_action_reject_for_app
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_application_label
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_cd_view_application
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_changes_header
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_confidence_label
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_dialog_reject_cancel
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_dialog_reject_label
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_dialog_reject_placeholder
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_dialog_reject_prompt
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_dialog_reject_title
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_empty
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_reason_label
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_source_email_from
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_source_email_subject
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen displaying the inbox of pending candidate updates and proposed edits.
 *
 * Provides review controls to approve individual changes, reject proposals with optional feedback notes,
 * and purge stale edits.
 *
 * @param onNavigateBack Callback invoked when exiting the review screen.
 * @param onEditClick Callback invoked when an individual edit card is clicked.
 * @param settings Application configuration controlling formatting.
 * @param onSettingsChange Callback receiving settings adjustments.
 * @param viewModel [TrackerEditsViewModel] providing UI state and handling approvals/rejections.
 */
@Composable
public fun TrackerEditsScreen(
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    viewModel: TrackerEditsViewModel = koinViewModel(),
): Unit {
    val uiState: TrackerEditsUiState by viewModel.uiState.collectAsState()
    var rejectDialogItem: TrackerEditItemUiModel? by remember { mutableStateOf(null) }
    var rejectReason: String by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is TrackerEditsUiState.NavigateBack) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Actions row: Expire Stale Edits & Refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isActionInProgress = (uiState as? TrackerEditsUiState.Content)?.isActionInProgress == true
            val isLoading = uiState is TrackerEditsUiState.Loading

            TooltipIconButton(
                tooltip = stringResource(Res.string.edits_action_expire_stale),
                icon = Icons.Default.DateRange,
                onClick = { viewModel.onIntent(TrackerEditsUiIntent.ExpireStaleEdits) },
                enabled = !isActionInProgress && !isLoading,
            )
            TooltipIconButton(
                tooltip = stringResource(Res.string.edits_action_refresh),
                icon = Icons.Default.Refresh,
                onClick = { viewModel.onIntent(TrackerEditsUiIntent.Refresh) },
                enabled = !isLoading && !isActionInProgress,
            )
        }

        when (val state = uiState) {
            is TrackerEditsUiState.Loading -> {
                LoadingWidget(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.medium),
                )
            }
            is TrackerEditsUiState.Error -> {
                ErrorBannerWidget(
                    message = state.message,
                    onRetry = { viewModel.onIntent(TrackerEditsUiIntent.Refresh) },
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                )
            }
            is TrackerEditsUiState.Content -> {
                if (state.edits.isEmpty()) {
                    EmptyStateWidget(
                        title = stringResource(Res.string.edits_empty),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.medium),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                    ) {
                        items(
                            items = state.edits,
                            key = { item -> item.id },
                        ) { item ->
                            EditCard(
                                item = item,
                                isActionInProgress = state.isActionInProgress,
                                onApprove = {
                                    viewModel.onIntent(item.approveIntent)
                                },
                                onReject = {
                                    rejectDialogItem = item
                                },
                                onViewApplication = { onEditClick(item.applicationId) },
                            )
                        }
                    }
                }
            }
            is TrackerEditsUiState.NavigateBack -> {
                // Navigation side effect dispatched in LaunchedEffect
            }
        }
    }

    // Reject confirmation dialog
    rejectDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = {
                rejectDialogItem = null
                rejectReason = ""
            },
            title = { Text(stringResource(Res.string.edits_dialog_reject_title)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    Text(stringResource(Res.string.edits_dialog_reject_prompt))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text(stringResource(Res.string.edits_dialog_reject_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.edits_dialog_reject_placeholder)) },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            viewModel.onIntent(item.rejectIntent.copy(note = rejectReason))
                            rejectDialogItem = null
                            rejectReason = ""
                        }
                    },
                    enabled = rejectReason.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.edits_action_reject))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        rejectDialogItem = null
                        rejectReason = ""
                    },
                ) {
                    Text(stringResource(Res.string.edits_dialog_reject_cancel))
                }
            },
        )
    }
}

/**
 * Top-level card presenting a pending tracker edit proposal built on [TopLevelCard].
 *
 * When no collapsible content is hidden, clicking the card navigates to the target application,
 * while the "Approve" and "Reject" buttons receive equal-width action treatment via [TopLevelCardAction].
 */
@Composable
internal fun EditCard(
    item: TrackerEditItemUiModel,
    isActionInProgress: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onViewApplication: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TopLevelCardSurfaceVariant = TopLevelCardSurfaceVariant.Elevated,
): Unit {
    val viewAppCd: String = stringResource(Res.string.edits_cd_view_application, item.applicationId)
    val approveLabel: String = stringResource(Res.string.edits_action_approve)
    val rejectLabel: String = stringResource(Res.string.edits_action_reject)
    val approveCd: String = stringResource(Res.string.edits_action_approve_for_app, item.applicationId)
    val rejectCd: String = stringResource(Res.string.edits_action_reject_for_app, item.applicationId)

    TopLevelCard(
        modifier = modifier,
        onClick = onViewApplication,
        contentDescription = viewAppCd,
        variant = variant,
        showActionsWhenNotCollapsible = true,
        title = {
            Text(
                text = item.kindDisplayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        },
        subtitle = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro)) {
                if (item.timestamp.isNotBlank()) {
                    Text(
                        text = item.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(Res.string.edits_application_label, item.applicationId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .semantics { contentDescription = viewAppCd }
                        .clickableArea(onClick = onViewApplication),
                )
            }
        },
        wideTrailingHeader = {
            Text(
                text = stringResource(Res.string.edits_confidence_label, item.confidenceDisplayName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        wideBody = {
            EditCardBodyContent(item = item)
        },
        actions = persistentListOf(
            TopLevelCardAction(
                label = approveLabel,
                onClick = onApprove,
                enabled = !isActionInProgress,
                contentDescription = approveCd,
                isPrimary = true,
            ),
            TopLevelCardAction(
                label = rejectLabel,
                onClick = onReject,
                enabled = !isActionInProgress,
                contentDescription = rejectCd,
                isPrimary = false,
            ),
        ),
    )
}

@Composable
private fun EditCardBodyContent(
    item: TrackerEditItemUiModel,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        if (item.changes.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.edits_changes_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.changes.forEach { change ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
                    ) {
                        Text(
                            text = change.field,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = change.changeDescription,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        change.reason?.let { reasonText ->
                            Text(
                                text = stringResource(Res.string.edits_reason_label, reasonText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else if (item.changeSummary.isNotBlank()) {
            Text(
                text = item.changeSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item.sourceEmail?.let { email ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
                ) {
                    Text(
                        text = stringResource(Res.string.edits_source_email_from, email.from),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = stringResource(Res.string.edits_source_email_subject, email.subject),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.clickableArea(onClick: () -> Unit): Modifier =
    this.then(
        clickable(
            onClick = onClick,
            onClickLabel = null,
            role = Role.Button,
        ),
    )
