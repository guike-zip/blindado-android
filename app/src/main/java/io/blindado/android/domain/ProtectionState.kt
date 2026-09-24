package io.blindado.android.domain

/**
 * Estado observável da proteção — sempre derivado do estado real do sistema
 * (Constituição, Princípio IV), nunca um valor otimista ou em cache.
 */
sealed interface ProtectionState {
    data object NaoConfigurado : ProtectionState
    data object Blindado : ProtectionState
    data class Erro(val motivo: ErrorReason) : ProtectionState
}

enum class ErrorReason {
    /** Usuário negou o diálogo de permissão de VPN do sistema. */
    PERMISSAO_NEGADA,

    /** Estava Blindado; a permissão foi revogada por fora do app. */
    PERMISSAO_REVOGADA,

    /**
     * Causa real não identificável pela API do Android. NUNCA usado como substituto de
     * investigar a causa real (FR-005) — sempre carrega a exceção/mensagem original do
     * sistema para diagnóstico (ver [io.blindado.android.protection.ProtectionException]).
     */
    FALHA_DESCONHECIDA,
}
