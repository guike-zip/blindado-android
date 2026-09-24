package io.blindado.android.domain

/** Resultado de uma execução da tela de Teste (User Story 3). */
data class TestResult(
    val items: List<DomainCheck>,
    val overall: OverallResult,
)

data class DomainCheck(
    val domain: String,
    val category: String,
    val status: DomainStatus,
)

enum class DomainStatus { BLOQUEADO, ACESSIVEL, VERIFICANDO }

/**
 * [OverallResult.INDETERMINADO] é obrigatório (nunca [PROTEGIDO]/[DESPROTEGIDO]) quando não há
 * conectividade de rede real disponível para testar (FR-012).
 */
enum class OverallResult { PROTEGIDO, DESPROTEGIDO, INDETERMINADO }
