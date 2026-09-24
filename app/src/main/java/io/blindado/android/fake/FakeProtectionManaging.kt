package io.blindado.android.fake

import android.content.Intent
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.domain.ProtectionState
import io.blindado.android.protection.ProtectionException
import io.blindado.android.protection.ProtectionManaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Implementação em memória de [ProtectionManaging] para testes unitários e `@Preview` do
 * Compose — sem VPN real, sem dispositivo físico, sem permissão do sistema.
 *
 * Satisfaz as 5 regras do contrato de teste em
 * `specs/001-dns-privado-android/contracts/protection-managing.md`.
 */
class FakeProtectionManaging(
    initialState: ProtectionState = ProtectionState.NaoConfigurado,
) : ProtectionManaging {

    private val _stateChanges = MutableStateFlow(initialState)
    override val stateChanges: StateFlow<ProtectionState> = _stateChanges

    /** Quando não nulo, [install] lança esta exceção em vez de conectar. */
    var installFailure: ProtectionException? = null

    var currentProfile: ProtectionProfile? = null
        private set

    override suspend fun currentState(): ProtectionState = _stateChanges.value

    /** Fakes nunca precisam de permissão real — sempre já "concedida". */
    override fun vpnPermissionIntent(): Intent? = null

    override suspend fun install(profile: ProtectionProfile) {
        installFailure?.let { throw it }
        currentProfile = profile
        _stateChanges.value = ProtectionState.Blindado
    }

    override suspend fun remove() {
        currentProfile = null
        _stateChanges.value = ProtectionState.NaoConfigurado
    }

    override suspend fun updateProfile(profile: ProtectionProfile) {
        check(_stateChanges.value is ProtectionState.Blindado) {
            "updateProfile só é válido quando o estado atual é Blindado"
        }
        currentProfile = profile
        // Regra #3 do contrato: a troca não deve emitir nenhum estado diferente de Blindado.
    }

    /** Permite forçar qualquer estado a qualquer momento — usado para testar revogação externa. */
    fun forceState(state: ProtectionState) {
        _stateChanges.value = state
    }
}
