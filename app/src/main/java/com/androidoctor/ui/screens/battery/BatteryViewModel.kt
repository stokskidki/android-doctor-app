package com.androidoctor.ui.screens.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.parser.BatteryAttributionParser
import com.androidoctor.domain.model.BatteryAttribution
import com.androidoctor.domain.model.BatteryDiagnosis
import com.androidoctor.domain.repository.DiagnosisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatteryUiState(
    val isLoading: Boolean = false,
    val diagnosis: BatteryDiagnosis? = null,
    val attribution: BatteryAttribution? = null,
    val error: String? = null,
)

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val repository: DiagnosisRepository,
    private val attributionParser: BatteryAttributionParser,
) : ViewModel() {

    private val _state = MutableStateFlow(BatteryUiState())
    val state: StateFlow<BatteryUiState> = _state.asStateFlow()

    fun analyze() {
        viewModelScope.launch {
            _state.value = BatteryUiState(isLoading = true)
            try {
                val diag = repository.diagnoseBattery()
                _state.value = BatteryUiState(diagnosis = diag)
                val attribution = runCatching { attributionParser.attribute() }.getOrNull()
                _state.value = _state.value.copy(attribution = attribution)
            } catch (e: Exception) {
                _state.value = BatteryUiState(error = e.message)
            }
        }
    }
}
