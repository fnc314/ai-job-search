package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEdit
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public data class TrackerEditsUiState(
    val edits: List<TrackerEdit> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val actionInProgress: Boolean = false,
)

public class TrackerEditsViewModel(
    private val loadPendingEdits: LoadPendingEditsUseCase,
    private val approveEdit: ApproveEditUseCase,
    private val rejectEdit: RejectEditUseCase,
    private val expireStaleEdits: ExpireStaleEditsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerEditsUiState())
    public val uiState: StateFlow<TrackerEditsUiState> = _uiState.asStateFlow()

    init {
        loadEdits()
    }

    public fun loadEdits() {
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

    public fun approveEdit(id: String, note: String? = null) {
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

    public fun rejectEdit(id: String, reason: String) {
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

    public fun expireStaleEdits() {
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
