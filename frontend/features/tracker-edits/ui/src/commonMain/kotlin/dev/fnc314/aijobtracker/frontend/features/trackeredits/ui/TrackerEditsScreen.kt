@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalGridApi::class)

package dev.fnc314.aijobtracker.frontend.features.trackeredits.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerEventType
import dev.fnc314.aijobtracker.frontend.core.data.model.TrackerStatus
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettings
import dev.fnc314.aijobtracker.frontend.core.designsystem.spacing
import dev.fnc314.aijobtracker.frontend.core.widgets.EmptyStateWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.ErrorBannerWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.LoadingWidget
import dev.fnc314.aijobtracker.frontend.core.widgets.TooltipIconButton
import dev.fnc314.aijobtracker.frontend.core.widgets.TopLevelCard
import dev.fnc314.aijobtracker.frontend.core.widgets.TopLevelCardAction
import dev.fnc314.aijobtracker.frontend.core.widgets.TopLevelCardSurfaceVariant
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_approve
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_approve_for_app
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_edit
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_expire_stale
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_refresh
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_reject
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_reject_for_app
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_action_undo
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_application_label
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_cd_view_application
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_changes_header
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_confidence_label
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_empty
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_inline_cancel
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_inline_event_type
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_inline_save
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_inline_target_status
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_reason_label
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_snackbar_approved
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_snackbar_rejected
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_source_email_from
import dev.fnc314.aijobtracker.frontend.features_tracker_edits_ui.generated.resources.edits_source_email_subject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private data class PendingSwipeAction(
    val item: TrackerEditItemUiModel,
    val isApprove: Boolean,
)

/**
 * Screen displaying the inbox of pending candidate updates and proposed edits with full-page inline editing,
 * swipe-to-action interactions, and accessible stacked Snackbars with UNDO.
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pendingActions = remember { mutableStateListOf<PendingSwipeAction>() }

    LaunchedEffect(uiState) {
        if (uiState is TrackerEditsUiState.NavigateBack) {
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    val visibleEdits = state.edits.filter { edit ->
                        pendingActions.none { it.item.id == edit.id }
                    }

                    if (visibleEdits.isEmpty()) {
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
                                items = visibleEdits,
                                key = { item -> item.id },
                            ) { item ->
                                SwipeableEditCard(
                                    item = item,
                                    swipeRightToApprove = settings.swipeRightToApprove,
                                    isActionInProgress = state.isActionInProgress,
                                    onSwipeAction = { isApprove ->
                                        val action = PendingSwipeAction(item, isApprove)
                                        pendingActions.add(action)

                                        val messageText = if (isApprove) {
                                            "Approved: ${item.applicationId}"
                                        } else {
                                            "Rejected: ${item.applicationId}"
                                        }

                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = messageText,
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Indefinite,
                                            )
                                            when (result) {
                                                SnackbarResult.ActionPerformed -> {
                                                    // Undo tapped: restore card
                                                    pendingActions.remove(action)
                                                }
                                                SnackbarResult.Dismissed -> {
                                                    // Dismissed: commit action
                                                    pendingActions.remove(action)
                                                    if (isApprove) {
                                                        viewModel.onIntent(item.approveIntent)
                                                    } else {
                                                        viewModel.onIntent(item.rejectIntent)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onApprove = { viewModel.onIntent(item.approveIntent) },
                                    onReject = { viewModel.onIntent(item.rejectIntent) },
                                    onSaveEdit = { eventType, statusTo, reason ->
                                        viewModel.onIntent(
                                            TrackerEditsUiIntent.EditProposedChange(
                                                proposalId = item.id,
                                                itemIndex = 0,
                                                newEventType = eventType,
                                                newStatusTo = statusTo,
                                                reason = reason,
                                            )
                                        )
                                    },
                                    onViewApplication = { onEditClick(item.applicationId) },
                                )
                            }
                        }
                    }
                }
                is TrackerEditsUiState.NavigateBack -> {
                    // Handled in LaunchedEffect
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(MaterialTheme.spacing.medium)
        )
    }
}

/**
 * Swipe-to-dismiss wrapped proposal card supporting configurable swipe actions and inline editing.
 */
@Composable
internal fun SwipeableEditCard(
    item: TrackerEditItemUiModel,
    swipeRightToApprove: Boolean,
    isActionInProgress: Boolean,
    onSwipeAction: (isApprove: Boolean) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSaveEdit: (TrackerEventType, TrackerStatus?, String?) -> Unit,
    onViewApplication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.targetValue) {
        when (dismissState.targetValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onSwipeAction(swipeRightToApprove)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onSwipeAction(!swipeRightToApprove)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = !isActionInProgress,
        enableDismissFromEndToStart = !isActionInProgress,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isApproveDirection = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                swipeRightToApprove
            } else {
                !swipeRightToApprove
            }

            val bgColor = if (isApproveDirection) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
            val iconTint = if (isApproveDirection) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
            val icon = if (isApproveDirection) Icons.Default.Check else Icons.Default.Close

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor, shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = MaterialTheme.spacing.large),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
        },
        content = {
            EditCard(
                item = item,
                isActionInProgress = isActionInProgress,
                onApprove = onApprove,
                onReject = onReject,
                onSaveEdit = onSaveEdit,
                onViewApplication = onViewApplication,
            )
        }
    )
}

/**
 * Top-level card presenting a pending tracker edit proposal with full-page inline editing form controls.
 */
@Composable
internal fun EditCard(
    item: TrackerEditItemUiModel,
    isActionInProgress: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSaveEdit: (TrackerEventType, TrackerStatus?, String?) -> Unit,
    onViewApplication: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TopLevelCardSurfaceVariant = TopLevelCardSurfaceVariant.Elevated,
): Unit {
    var isEditing by remember { mutableStateOf(false) }
    var selectedEventType by remember { mutableStateOf<TrackerEventType>(TrackerEventType.Known.ACKNOWLEDGE) }
    var selectedStatus by remember { mutableStateOf<TrackerStatus>(TrackerStatus.Known.UNSPECIFIED) }
    var editReason by remember { mutableStateOf("") }

    val viewAppCd: String = stringResource(Res.string.edits_cd_view_application, item.applicationId)
    val approveLabel: String = stringResource(Res.string.edits_action_approve)
    val rejectLabel: String = stringResource(Res.string.edits_action_reject)
    val editLabel: String = stringResource(Res.string.edits_action_edit)

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
                    modifier = Modifier.clickableArea(onClick = onViewApplication),
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                EditCardBodyContent(item = item)

                // Inline editing section (strictly full page, no dialogs)
                AnimatedVisibility(
                    visible = isEditing,
                    enter = fadeIn() + androidx.compose.animation.expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                        ) {
                            Text(
                                text = "Override Transition",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )

                            // Event Type selector
                            EventTypeDropdown(
                                selected = selectedEventType,
                                onSelect = { selectedEventType = it }
                            )

                            // Target Status selector
                            StatusDropdown(
                                selected = selectedStatus,
                                onSelect = { selectedStatus = it }
                            )

                            OutlinedTextField(
                                value = editReason,
                                onValueChange = { editReason = it },
                                label = { Text("Reason / Note") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. Recruiter confirmation, not rejection") },
                                minLines = 2,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = { isEditing = false },
                                    modifier = Modifier.padding(end = MaterialTheme.spacing.small)
                                ) {
                                    Text(stringResource(Res.string.edits_inline_cancel))
                                }
                                Button(
                                    onClick = {
                                        onSaveEdit(selectedEventType, selectedStatus, editReason.takeIf { it.isNotBlank() })
                                        isEditing = false
                                    }
                                ) {
                                    Text(stringResource(Res.string.edits_inline_save))
                                }
                            }
                        }
                    }
                }
            }
        },
        actions = persistentListOf(
            TopLevelCardAction(
                label = editLabel,
                onClick = { isEditing = !isEditing },
                enabled = !isActionInProgress,
                contentDescription = editLabel,
                isPrimary = false,
            ),
            TopLevelCardAction(
                label = approveLabel,
                onClick = onApprove,
                enabled = !isActionInProgress,
                contentDescription = approveLabel,
                isPrimary = true,
            ),
            TopLevelCardAction(
                label = rejectLabel,
                onClick = onReject,
                enabled = !isActionInProgress,
                contentDescription = rejectLabel,
                isPrimary = false,
            ),
        ),
    )
}

@Composable
private fun EventTypeDropdown(
    selected: TrackerEventType,
    onSelect: (TrackerEventType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val candidates = listOf(
        TrackerEventType.Known.ACKNOWLEDGE,
        TrackerEventType.Known.APPLY,
        TrackerEventType.Known.RECRUITER_SCREEN,
        TrackerEventType.Known.SCHEDULE_INTERVIEW,
        TrackerEventType.Known.COMPLETE_INTERVIEW,
        TrackerEventType.Known.OFFER,
        TrackerEventType.Known.REJECT,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.edits_inline_event_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            candidates.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName()) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusDropdown(
    selected: TrackerStatus,
    onSelect: (TrackerStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val candidates = listOf(
        TrackerStatus.Known.UNSPECIFIED,
        TrackerStatus.Known.SUBMITTED,
        TrackerStatus.Known.INTERVIEW,
        TrackerStatus.Known.OFFER_EXPECTED,
        TrackerStatus.Known.REJECTED,
        TrackerStatus.Known.NO_RESPONSE,
        TrackerStatus.Known.LEAD,
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.edits_inline_target_status)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            candidates.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.displayName()) },
                    onClick = {
                        onSelect(status)
                        expanded = false
                    }
                )
            }
        }
    }
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

private fun Modifier.clickableArea(onClick: () -> Unit): Modifier = this.clickable(
    role = Role.Button,
    onClick = onClick,
)
