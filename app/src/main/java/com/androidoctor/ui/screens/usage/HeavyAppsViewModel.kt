package com.androidoctor.ui.screens.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.parser.HeavyApp
import com.androidoctor.data.parser.UsageStatsParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HeavyAppsState(
    val loading: Boolean = true,
    val hasPermission: Boolean = false,
    val apps: List<HeavyApp> = emptyList(),
)

@HiltViewModel
class HeavyAppsViewModel @Inject constructor(
    private val parser: UsageStatsParser,
) : ViewModel() {

    private val _state = MutableStateFlow(HeavyAppsState())
    val state: StateFlow<HeavyAppsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hasPerm = parser.hasUsageAccess()
            if (!hasPerm) {
                _state.value = HeavyAppsState(loading = false, hasPermission = false)
                return@launch
            }
            val apps = parser.topHeavyApps()
            _state.value = HeavyAppsState(loading = false, hasPermission = true, apps = apps)
        }
    }
}
