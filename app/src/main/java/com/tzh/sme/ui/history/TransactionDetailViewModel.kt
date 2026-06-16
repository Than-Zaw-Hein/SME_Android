package com.tzh.sme.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tzh.sme.domain.usecase.history.HistoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val historyUseCases: HistoryUseCases,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState(isLoading = true))
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    private val _sideEffect = Channel<TransactionDetailUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        sendIntent(TransactionDetailUiIntent.LoadDetail)
    }

    fun sendIntent(intent: TransactionDetailUiIntent) {
        when (intent) {
            TransactionDetailUiIntent.LoadDetail -> loadTransactionDetail()
        }
    }

    private fun loadTransactionDetail() {
        historyUseCases.getTransactionById(transactionId)
            .onEach { result ->
                if (result != null) {
                    _uiState.update { it.copy(transaction = result.first, items = result.second, isLoading = false, error = null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Transaction not found") }
                }
            }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "An unknown error occurred") } }
            .launchIn(viewModelScope)
    }
}
