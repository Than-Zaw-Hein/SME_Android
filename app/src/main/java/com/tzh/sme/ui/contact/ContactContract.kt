package com.tzh.sme.ui.contact

import android.os.Build
import com.tzh.sme.BuildConfig

data class ContactUiState(
    val bugReportText: String = "",
    val maxCharLimit: Int = 500,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val isSubmitEnabled: Boolean = false
)

sealed interface ContactUiIntent {
    data class BugReportChanged(val text: String) : ContactUiIntent
    object SubmitBugReport : ContactUiIntent
}

sealed interface ContactUiSideEffect {
    data class ShowToast(val message: String) : ContactUiSideEffect
}
