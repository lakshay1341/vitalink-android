package com.lakshay.vitalink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.vitalink.data.Encounter
import com.lakshay.vitalink.data.LatestVital
import com.lakshay.vitalink.data.News2Result
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One dashboard row: an active encounter plus its latest score and vitals (null until loaded). */
data class PatientRow(
    val encounter: Encounter,
    val news2: News2Result?,
    val vitals: List<LatestVital>?,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Content(val rows: List<PatientRow>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: VitaLinkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        load()
    }

    /** (Re)load the ward: active encounters, then their NEWS2 + latest vitals concurrently. */
    fun load() {
        _state.value = DashboardUiState.Loading
        viewModelScope.launch {
            repo.encounters().fold(
                onSuccess = { list ->
                    val active = list.filter { it.status in ACTIVE }
                    val rows = coroutineScope {
                        active.map { enc ->
                            async {
                                PatientRow(
                                    enc,
                                    repo.news2(enc.id).getOrNull(),
                                    repo.latestVitals(enc.id).getOrNull(),
                                )
                            }
                        }.awaitAll()
                    }
                    _state.value = DashboardUiState.Content(rows)
                },
                onFailure = { _state.value = DashboardUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    companion object {
        private val ACTIVE = setOf("PRE_ADMIT", "ADMITTED", "DISCHARGE_PENDING")
    }
}
