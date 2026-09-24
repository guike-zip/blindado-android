package io.blindado.android.protection

import android.content.Intent
import io.blindado.android.domain.ErrorReason
import io.blindado.android.domain.ProtectionProfile
import io.blindado.android.domain.ProtectionState
import kotlinx.coroutines.flow.Flow

/**
 * Interface por trás da qual fica toda a lógica real de VpnService (Constituição, Princípio V
 * — testável, com implementação fake injetável). Todos os ViewModels dependem só desta
 * interface, nunca de `BlindadoVpnService` diretamente.
 *
 * Ver `specs/001-dns-privado-android/contracts/protection-managing.md` para o contrato completo,
 * incluindo as 5 regras de teste que toda implementação DEVE satisfazer.
 */
interface ProtectionManaging {
    /** Estado atual real da proteção — nunca cacheado, sempre consultado ao vivo. */
    suspend fun currentState(): ProtectionState

    /**
     * Retorna o [Intent] do diálogo nativo de permissão de VPN do Android
     * (`VpnService.prepare()`) se ainda não concedida, ou `null` se já concedida — quem chama
     * (a camada de UI, que tem um `Activity`) deve lançar esse Intent via
     * `ActivityResultContracts.StartActivityForResult` antes de chamar [install]. Implementações
     * fake sempre retornam `null` (nunca precisam de permissão real).
     */
    fun vpnPermissionIntent(): Intent?

    /**
     * Solicita a permissão de VPN do sistema (se necessário) e, se concedida, conecta a
     * VpnService com o perfil informado. Suspende até a conexão ser confirmada ou falhar.
     *
     * @throws ProtectionException com um [ErrorReason] real — nunca um motivo inventado
     *   (FR-005).
     */
    suspend fun install(profile: ProtectionProfile)

    /** Desconecta a VpnService, se estiver ativa. Idempotente. */
    suspend fun remove()

    /**
     * Reaplica a proteção com um novo [ProtectionProfile] sem exigir nova concessão de
     * permissão de VPN (FR-009) — só válido quando o estado atual já é [ProtectionState.Blindado].
     */
    suspend fun updateProfile(profile: ProtectionProfile)

    /**
     * Fluxo observável de mudanças de estado, para a UI reagir a mudanças que aconteçam fora
     * de uma ação direta do usuário no app (ex.: permissão revogada externamente — FR-004).
     */
    val stateChanges: Flow<ProtectionState>
}

class ProtectionException(val reason: ErrorReason, cause: Throwable? = null) : Exception(cause)
