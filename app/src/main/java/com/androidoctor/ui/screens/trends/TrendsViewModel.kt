package com.androidoctor.ui.screens.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidoctor.data.local.dao.ScoreSnapshotDao
import com.androidoctor.data.local.entity.ScoreSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class TrendRange(val days: Int, val label: String) {
    WEEK(7, "7d"), MONTH(30, "30d"), QUARTER(90, "90d")
}

data class TrendsState(
    val range: TrendRange = TrendRange.WEEK,
    val snapshots: List<ScoreSnapshotEntity> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val dao: ScoreSnapshotDao,
) : ViewModel() {

    private val _state = MutableStateFlow(TrendsState())
    val state: StateFlow<TrendsState> = _state.asStateFlow()

    init {
        load(TrendRange.WEEK)
    }

    fun selectRange(range: TrendRange) {
        load(range)
    }

    private fun load(range: TrendRange) {
        viewModelScope.launch {
            _state.value = _state.value.copy(range = range, loading = true)
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(range.days.toLong())
            val data = runCatching { dao.since(cutoff) }.getOrDefault(emptyList())
            _state.value = TrendsState(range = range, snapshots = data, loading = false)
        }
    }
}
