package com.tzh.sme.ui.contact

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<ContactUiSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun sendIntent(intent: ContactUiIntent) {
        when (intent) {
            is ContactUiIntent.BugReportChanged -> onBugReportChanged(intent.text)
            ContactUiIntent.SubmitBugReport -> submitBugReport()
        }
    }

    private fun onBugReportChanged(text: String) {
        if (text.length <= _uiState.value.maxCharLimit) {
            _uiState.update { it.copy(bugReportText = text, isSubmitEnabled = text.isNotBlank()) }
        }
    }

    private fun submitBugReport() {
        // Mock submission
        _uiState.update { it.copy(bugReportText = "", isSubmitEnabled = false) }
    }
}
