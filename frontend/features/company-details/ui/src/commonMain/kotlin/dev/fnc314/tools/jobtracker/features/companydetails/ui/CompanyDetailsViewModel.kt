package dev.fnc314.tools.jobtracker.features.companydetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fnc314.tools.jobtracker.features.companydetails.domain.LoadCompanyDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

public class CompanyDetailsViewModel(
    private val loadCompanyDetailsUseCase: LoadCompanyDetailsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompanyDetailsUiState>(CompanyDetailsUiState.Loading)
    public val uiState: StateFlow<CompanyDetailsUiState> = _uiState.asStateFlow()

    private var currentCompanyId: String? = null

    public fun loadCompany(companyId: String, forceRefresh: Boolean = false) {
        currentCompanyId = companyId
        viewModelScope.launch {
            if (forceRefresh) {
                val current = _uiState.value
                if (current is CompanyDetailsUiState.Content) {
                    _uiState.value = current.copy(isRefreshing = true)
                }
            } else {
                _uiState.value = CompanyDetailsUiState.Loading
            }

            runCatching {
                loadCompanyDetailsUseCase(companyId)
            }.onSuccess { details ->
                if (details != null) {
                    val uiModel = toCompanyDetailsUiModel(
                        company = details.company,
                        applications = details.applications,
                    )
                    _uiState.value = CompanyDetailsUiState.Content(
                        details = uiModel,
                        isRefreshing = false,
                    )
                } else {
                    _uiState.value = CompanyDetailsUiState.Error("Company not found")
                }
            }.onFailure { error ->
                _uiState.value = CompanyDetailsUiState.Error(
                    message = error.message ?: "Failed to load company details"
                )
            }
        }
    }

    public fun onIntent(intent: CompanyDetailsUiIntent) {
        when (intent) {
            is CompanyDetailsUiIntent.Refresh -> {
                currentCompanyId?.let { loadCompany(it, forceRefresh = true) }
            }
            is CompanyDetailsUiIntent.SelectApplication -> {
                // Handled in screen callback
            }
        }
    }
}
