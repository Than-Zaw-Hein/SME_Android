package com.tzh.sme.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState(isLoading = true))
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        loadTransactionDetail()
    }

    private fun loadTransactionDetail() {
        transactionRepository.getTransactionById(transactionId)
            .onEach { result ->
                if (result != null) {
                    _uiState.update {
                        it.copy(
                            transaction = result.first,
                            items = result.second,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Transaction not found") }
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unknown error occurred"
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
