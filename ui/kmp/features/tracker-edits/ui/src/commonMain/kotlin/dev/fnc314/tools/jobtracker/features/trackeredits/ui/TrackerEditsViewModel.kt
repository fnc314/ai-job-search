package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class TrackerEditsViewModel(
    private val loadPendingEdits: LoadPendingEditsUseCase,
    private val approveEdit: ApproveEditUseCase,
    private val rejectEdit: RejectEditUseCase,
    private val expireStaleEdits: ExpireStaleEditsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackerEditsUiState>(TrackerEditsUiState.Loading)
    public val uiState: StateFlow<TrackerEditsUiState> = _uiState.asStateFlow()

    init {
        loadEdits()
    }

    public fun onIntent(intent: TrackerEditsUiIntent): Unit {
        when (intent) {
            is TrackerEditsUiIntent.Refresh -> loadEdits()
            is TrackerEditsUiIntent.ApproveEdit -> handleApprove(intent.id, intent.note)
            is TrackerEditsUiIntent.RejectEdit -> handleReject(intent.id, intent.note)
            is TrackerEditsUiIntent.ExpireStaleEdits -> handleExpireStaleEdits()
            is TrackerEditsUiIntent.ClickBack -> handleClickBack()
        }
    }

    private fun loadEdits(): Unit {
        viewModelScope.launch {
            _uiState.value = TrackerEditsUiState.Loading
            try {
                val edits = loadPendingEdits(LoadPendingEditsUseCase.Params(forceRefresh = true))
                val uiModels = edits.map { it.toUiModel() }.toPersistentList()
                _uiState.value = TrackerEditsUiState.Content(edits = uiModels, isActionInProgress = false)
            } catch (e: Exception) {
                _uiState.value = TrackerEditsUiState.Error(message = e.message ?: "Failed to load edits")
            }
        }
    }

    private fun handleApprove(id: String, note: String?): Unit {
        val currentContent = _uiState.value as? TrackerEditsUiState.Content
        viewModelScope.launch {
            if (currentContent != null) {
                _uiState.value = currentContent.copy(isActionInProgress = true)
            }
            try {
                approveEdit(ApproveEditUseCase.Params(id = id, note = note))
                val updatedEdits = currentContent?.edits?.filter { it.id != id }?.toPersistentList()
                    ?: emptyList<TrackerEditItemUiModel>().toPersistentList()
                _uiState.value = TrackerEditsUiState.Content(edits = updatedEdits, isActionInProgress = false)
            } catch (e: Exception) {
                _uiState.value = TrackerEditsUiState.Error(message = e.message ?: "Failed to approve edit")
            }
        }
    }

    private fun handleReject(id: String, note: String?): Unit {
        val currentContent = _uiState.value as? TrackerEditsUiState.Content
        viewModelScope.launch {
            if (currentContent != null) {
                _uiState.value = currentContent.copy(isActionInProgress = true)
            }
            try {
                rejectEdit(RejectEditUseCase.Params(id = id, reason = note ?: "Rejected by user"))
                val updatedEdits = currentContent?.edits?.filter { it.id != id }?.toPersistentList()
                    ?: emptyList<TrackerEditItemUiModel>().toPersistentList()
                _uiState.value = TrackerEditsUiState.Content(edits = updatedEdits, isActionInProgress = false)
            } catch (e: Exception) {
                _uiState.value = TrackerEditsUiState.Error(message = e.message ?: "Failed to reject edit")
            }
        }
    }

    private fun handleExpireStaleEdits(): Unit {
        val currentContent = _uiState.value as? TrackerEditsUiState.Content
        viewModelScope.launch {
            if (currentContent != null) {
                _uiState.value = currentContent.copy(isActionInProgress = true)
            }
            try {
                expireStaleEdits(ExpireStaleEditsUseCase.Params())
                val edits = loadPendingEdits(LoadPendingEditsUseCase.Params(forceRefresh = true))
                val uiModels = edits.map { it.toUiModel() }.toPersistentList()
                _uiState.value = TrackerEditsUiState.Content(edits = uiModels, isActionInProgress = false)
            } catch (e: Exception) {
                _uiState.value = TrackerEditsUiState.Error(message = e.message ?: "Failed to expire stale edits")
            }
        }
    }

    private fun handleClickBack(): Unit {
        _uiState.value = TrackerEditsUiState.NavigateBack
    }
}
