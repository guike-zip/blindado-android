package io.blindado.android.protection

import android.content.Context
import android.content.Intent
import android.net.VpnService
import io.blindado.android.domain.DnsProvider
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.domain.ProtectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Implementação real de [ProtectionManaging], controlando [BlindadoVpnService] via Intents e
 * observando seu estado via os `StateFlow` companion do serviço (FR-004, FR-009).
 */
class RealProtectionManaging(private val context: Context) : ProtectionManaging {

    override fun vpnPermissionIntent(): Intent? = VpnService.prepare(context)

    override suspend fun currentState(): ProtectionState {
        val running = BlindadoVpnService.isRunning.value
        val error = BlindadoVpnService.lastError.value
        return when {
            running -> ProtectionState.Blindado
            error != null -> ProtectionState.Erro(error)
            else -> ProtectionState.NaoConfigurado
        }
    }

    override suspend fun install(profile: ProtectionProfile) {
        if (vpnPermissionIntent() != null) {
            throw ProtectionException(ErrorReason.PERMISSAO_NEGADA)
        }

        val endpoint = resolveEndpoint(profile)
            ?: throw ProtectionException(ErrorReason.FALHA_DESCONHECIDA)

        val intent = Intent(context, BlindadoVpnService::class.java).apply {
            action = BlindadoVpnService.ACTION_CONNECT
            putExtra(BlindadoVpnService.EXTRA_DOH_ENDPOINT, endpoint)
        }
        context.startForegroundService(intent)

        // O establish() da VpnService é síncrono dentro do serviço, mas a comunicação aqui é
        // por Intent (assíncrona) — aguarda um instante e confirma via o estado real observável,
        // nunca assume sucesso otimisticamente (Princípio IV).
        awaitState { it is ProtectionState.Blindado || it is ProtectionState.Erro }
        val finalState = currentState()
        if (finalState is ProtectionState.Erro) {
            throw ProtectionException(finalState.motivo)
        }
    }

    override suspend fun remove() {
        val intent = Intent(context, BlindadoVpnService::class.java).apply {
            action = BlindadoVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
        awaitState { it !is ProtectionState.Blindado }
    }

    override suspend fun updateProfile(profile: ProtectionProfile) {
        check(currentState() is ProtectionState.Blindado) {
            "updateProfile só é válido quando o estado atual é Blindado"
        }
        // Reaplica sem exigir nova permissão (FR-009) — reconecta a VpnService internamente,
        // já que Android não permite alterar addDnsServer() de uma interface TUN já estabelecida.
        install(profile)
    }

    override val stateChanges: Flow<ProtectionState> =
        combine(BlindadoVpnService.isRunning, BlindadoVpnService.lastError) { running, error ->
            when {
                running -> ProtectionState.Blindado
                error != null -> ProtectionState.Erro(error)
                else -> ProtectionState.NaoConfigurado
            }
        }.distinctUntilChanged()

    private suspend fun awaitState(predicate: (ProtectionState) -> Boolean) {
        var attempts = 0
        while (attempts < 50 && !predicate(currentState())) {
            delay(100)
            attempts++
        }
    }

    private fun resolveEndpoint(profile: ProtectionProfile): String? {
        if (profile.providerId == DnsProvider.CUSTOM_PROVIDER_ID) {
            return profile.customDohUrl
        }
        return ProviderCatalog.byId(profile.providerId)?.dohEndpoint
    }
}
