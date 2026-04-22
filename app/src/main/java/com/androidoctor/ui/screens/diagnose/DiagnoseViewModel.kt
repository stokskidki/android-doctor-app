package com.androidoctor.ui.screens.diagnose

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.domain.model.DiagnosisResult
import com.androidoctor.domain.usecase.DiagnoseUseCase
import com.androidoctor.ui.share.ReportCardRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnoseUiState(
    val isLoading: Boolean = false,
    val currentStep: String = "",
    val result: DiagnosisResult? = null,
    val error: String? = null,
    val sharing: Boolean = false,
    val shareUri: Uri? = null,
    val shareError: String? = null,
)

@HiltViewModel
class DiagnoseViewModel @Inject constructor(
    private val diagnoseUseCase: DiagnoseUseCase,
    private val reportCardRenderer: ReportCardRenderer,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnoseUiState())
    val state: StateFlow<DiagnoseUiState> = _state.asStateFlow()

    fun runDiagnosis() {
        viewModelScope.launch {
            _state.value = DiagnoseUiState(isLoading = true, currentStep = "Running diagnostics...")
            try {
                val result = diagnoseUseCase()
                _state.value = DiagnoseUiState(result = result)
            } catch (e: Exception) {
                _state.value = DiagnoseUiState(error = e.message ?: "Diagnosis failed")
            }
        }
    }

    fun share() {
        val result = _state.value.result ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(sharing = true, shareError = null)
            when (val r = reportCardRenderer.render(result)) {
                is com.androidoctor.ui.share.ReportCardRenderer.RenderResult.Success ->
                    _state.value = _state.value.copy(sharing = false, shareUri = r.uri)
                is com.androidoctor.ui.share.ReportCardRenderer.RenderResult.Failure ->
                    _state.value = _state.value.copy(sharing = false, shareError = r.reason)
            }
        }
    }

    fun consumeShareUri() {
        _state.value = _state.value.copy(shareUri = null)
    }

    fun consumeShareError() {
        _state.value = _state.value.copy(shareError = null)
    }
}
