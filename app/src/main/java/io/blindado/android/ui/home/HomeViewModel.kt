package io.blindado.android.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.blindado.android.data.ProtectionProfileStore
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionState
import io.blindado.android.protection.ProtectionException
import io.blindado.android.protection.ProtectionManaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel da Home (User Story 1 — Blindar o aparelho). Nunca cacheia estado otimista
 * (Constituição, Princípio IV) — sempre reflete o [ProtectionManaging.currentState] real.
 */
class HomeViewModel(
    private val protectionManaging: ProtectionManaging,
    private val profileStore: ProtectionProfileStore,
) : ViewModel() {

    private val _state = MutableStateFlow<ProtectionState>(ProtectionState.NaoConfigurado)
    val state: StateFlow<ProtectionState> = _state.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    /** Evento de efeito único: pede à UI (que tem um Activity) para lançar este Intent do sistema. */
    private val _permissionRequest = MutableStateFlow<Intent?>(null)
    val permissionRequest: StateFlow<Intent?> = _permissionRequest.asStateFlow()

    init {
        viewModelScope.launch {
            refreshState()
            protectionManaging.stateChanges.collect { _state.value = it }
        }
    }

    suspend fun refreshState() {
        _state.value = protectionManaging.currentState()
    }

    fun blindar() {
        val intent = protectionManaging.vpnPermissionIntent()
        if (intent != null) {
            _permissionRequest.value = intent
            return
        }
        installWithCurrentProfile()
    }

    /** Chamado pela UI depois que o usuário responde ao diálogo de permissão do sistema. */
    fun onPermissionResult(granted: Boolean) {
        _permissionRequest.value = null
        if (granted) {
            installWithCurrentProfile()
        } else {
            _state.value = ProtectionState.Erro(ErrorReason.PERMISSAO_NEGADA)
        }
    }

    private fun installWithCurrentProfile() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val profile = profileStore.profile.first()
                protectionManaging.install(profile)
            } catch (e: ProtectionException) {
                _state.value = ProtectionState.Erro(e.reason)
            } finally {
                refreshState()
                _isBusy.value = false
            }
        }
    }

    fun remover() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                protectionManaging.remove()
            } finally {
                refreshState()
                _isBusy.value = false
            }
        }
    }
}
