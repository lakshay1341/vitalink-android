package com.lakshay.vitalink.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val server: String = "http://10.0.2.2:8080",
    val username: String = "admin",
    val password: String = "admin",
    val showPassword: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: VitaLinkRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onServer(v: String) = _state.update { it.copy(server = v) }
    fun onUsername(v: String) = _state.update { it.copy(username = v) }
    fun onPassword(v: String) = _state.update { it.copy(password = v) }
    fun toggleShow() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun submit() {
        val s = _state.value
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            repo.login(s.server, s.username, s.password).fold(
                onSuccess = { _state.update { it.copy(busy = false, success = true) } },
                onFailure = { e -> _state.update { it.copy(busy = false, error = "Sign-in failed: ${e.message}") } },
            )
        }
    }
}
