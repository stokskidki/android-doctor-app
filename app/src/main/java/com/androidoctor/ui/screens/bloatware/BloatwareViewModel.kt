package com.androidoctor.ui.screens.bloatware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.parser.BloatwareCatalogLoader
import com.androidoctor.data.parser.BloatwareCatalogResult
import com.androidoctor.data.shell.ShellExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BloatwareState(
    val loading: Boolean = true,
    val catalog: BloatwareCatalogResult? = null,
    val disabled: Set<String> = emptySet(),
    val lastMessage: String? = null,
    val shizukuAvailable: Boolean = false,
)

@HiltViewModel
class BloatwareViewModel @Inject constructor(
    private val loader: BloatwareCatalogLoader,
    private val shell: ShellExecutor,
) : ViewModel() {

    private val _state = MutableStateFlow(BloatwareState())
    val state: StateFlow<BloatwareState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val catalog = loader.load()
            _state.value = BloatwareState(
                loading = false,
                catalog = catalog,
                shizukuAvailable = shell.isShizukuAvailable,
            )
        }
    }

    fun disable(pkg: String) {
        viewModelScope.launch {
            val result = runCatching { shell.exec("pm disable-user --user 0 $pkg") }.getOrNull()
            if (result != null && result.exitCode == 0) {
                _state.value = _state.value.copy(
                    disabled = _state.value.disabled + pkg,
                    lastMessage = "Disabled $pkg",
                )
            } else {
                _state.value = _state.value.copy(
                    lastMessage = "Failed: requires Shizuku",
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(lastMessage = null)
    }
}
