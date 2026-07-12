package com.lakshay.vitalink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.vitalink.data.AlertRule
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ThresholdsUiState {
    data object Loading : ThresholdsUiState
    data class Content(
        val rules: List<AlertRule>,
        val savingId: Long? = null,
        val message: String? = null,
    ) : ThresholdsUiState

    data class Error(val message: String) : ThresholdsUiState
}

@HiltViewModel
class AlarmThresholdsViewModel @Inject constructor(
    private val repo: VitaLinkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ThresholdsUiState>(ThresholdsUiState.Loading)
    val state: StateFlow<ThresholdsUiState> = _state.asStateFlow()

    private var encounterId: Long = -1
    private var started = false

    fun start(encounterId: Long) {
        if (started) return
        started = true
        this.encounterId = encounterId
        load()
    }

    fun load() {
        _state.value = ThresholdsUiState.Loading
        viewModelScope.launch {
            repo.alertRules(encounterId).fold(
                onSuccess = { _state.value = ThresholdsUiState.Content(it) },
                onFailure = { _state.value = ThresholdsUiState.Error(it.message ?: "Failed to load rules") },
            )
        }
    }

    /** PUT one edited rule, then patch it into the in-memory list on success. */
    fun save(rule: AlertRule) {
        val cur = _state.value as? ThresholdsUiState.Content ?: return
        _state.value = cur.copy(savingId = rule.id, message = null)
        viewModelScope.launch {
            repo.updateAlertRule(rule).fold(
                onSuccess = { updated ->
                    val c = _state.value as? ThresholdsUiState.Content ?: cur
                    _state.value = c.copy(
                        rules = c.rules.map { if (it.id == updated.id) updated else it },
                        savingId = null,
                        message = "Saved ${updated.vitalType}",
                    )
                },
                onFailure = { e ->
                    val c = _state.value as? ThresholdsUiState.Content ?: cur
                    _state.value = c.copy(savingId = null, message = "Save failed: ${e.message}")
                },
            )
        }
    }
}
