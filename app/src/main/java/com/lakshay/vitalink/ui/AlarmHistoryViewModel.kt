package com.lakshay.vitalink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.vitalink.data.Alert
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(val alerts: List<Alert>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

@HiltViewModel
class AlarmHistoryViewModel @Inject constructor(
    private val repo: VitaLinkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var encounterId: Long = -1
    private var started = false

    fun start(encounterId: Long) {
        if (started) return
        started = true
        this.encounterId = encounterId
        load()
    }

    /** All alerts (active + resolved) for the encounter, newest first — the API sorts by triggeredAt desc. */
    fun load() {
        _state.value = HistoryUiState.Loading
        viewModelScope.launch {
            repo.alerts(encounterId, size = 100).fold(
                onSuccess = { _state.value = HistoryUiState.Content(it) },
                onFailure = { _state.value = HistoryUiState.Error(it.message ?: "Failed to load history") },
            )
        }
    }
}
