package dev.fnc314.tools.jobtracker.ui.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.domain.usecase.RejectEditUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrackerEditsUiState(
    val edits: List<TrackerEdit> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val actionInProgress: Boolean = false,
)

class TrackerEditsViewModel(
    private val loadPendingEdits: LoadPendingEditsUseCase,
    private val approveEdit: ApproveEditUseCase,
    private val rejectEdit: RejectEditUseCase,
    private val expireStaleEdits: ExpireStaleEditsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerEditsUiState())
    val uiState: StateFlow<TrackerEditsUiState> = _uiState.asStateFlow()

    init {
        loadEdits()
    }

    fun loadEdits() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val edits = loadPendingEdits(LoadPendingEditsUseCase.Params(forceRefresh = true))
                _uiState.update { it.copy(edits = edits, loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message ?: "Failed to load edits") }
            }
        }
    }

    fun approveEdit(id: String, note: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                approveEdit(ApproveEditUseCase.Params(id = id, note = note))
                _uiState.update {
                    it.copy(
                        edits = it.edits.filter { edit -> edit.id != id },
                        actionInProgress = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionInProgress = false, error = e.message ?: "Failed to approve edit") }
            }
        }
    }

    fun rejectEdit(id: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                rejectEdit(RejectEditUseCase.Params(id = id, reason = reason))
                _uiState.update {
                    it.copy(
                        edits = it.edits.filter { edit -> edit.id != id },
                        actionInProgress = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(actionInProgress = false, error = e.message ?: "Failed to reject edit") }
            }
        }
    }

    fun expireStaleEdits() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true) }
            try {
                expireStaleEdits(ExpireStaleEditsUseCase.Params())
                loadEdits()
            } catch (e: Exception) {
                _uiState.update { it.copy(actionInProgress = false, error = e.message ?: "Failed to expire stale edits") }
            }
        }
    }
}
