package com.tzh.sme.ui.contact

import android.os.Build
import androidx.lifecycle.ViewModel
import com.tzh.sme.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ContactUiState(
    val bugReportText: String = "",
    val maxCharLimit: Int = 500,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val isSubmitEnabled: Boolean = false
)

@HiltViewModel
class ContactViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState = _uiState.asStateFlow()

    fun onBugReportChanged(text: String) {
        if (text.length <= _uiState.value.maxCharLimit) {
            _uiState.update { 
                it.copy(
                    bugReportText = text,
                    isSubmitEnabled = text.isNotBlank()
                ) 
            }
        }
    }

    fun submitBugReport() {
        // Logic to send to backend or Firebase
        _uiState.update { it.copy(bugReportText = "", isSubmitEnabled = false) }
    }
}
