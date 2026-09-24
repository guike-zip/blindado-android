# Fase 1 — Modelo de dados: Blindado Android

Entidades de domínio derivadas de `spec.md` → Key Entities. Tipos em Kotlin idiomático
(`sealed interface`/`enum class`/`data class`), sem anotação de nenhum framework de
persistência específico no próprio modelo de domínio (DataStore mapeia para/de estes tipos na
camada `data/`, não o contrário — Princípio VII).

## ProtectionState

Estado observável da proteção, sempre derivado do estado real do sistema (Princípio IV — nunca
cacheado de forma otimista).

```kotlin
sealed interface ProtectionState {
    data object NaoConfigurado : ProtectionState
    data object Blindado : ProtectionState
    data class Erro(val motivo: ErrorReason) : ProtectionState
}

enum class ErrorReason {
    PERMISSAO_NEGADA,       // usuário negou o diálogo de permissão de VPN
    PERMISSAO_REVOGADA,     // estava Blindado, permissão foi revogada por fora do app
    FALHA_DESCONHECIDA,     // erro real da API sem causa mais específica identificável —
                             // NUNCA usado como substituto de investigar a causa real (FR-005)
}
```

**Transições de estado** (State machine):

```text
NaoConfigurado --[usuário concede permissão de VPN]--> Blindado
NaoConfigurado --[usuário nega permissão]--> Erro(PERMISSAO_NEGADA)
Erro(PERMISSAO_NEGADA) --[usuário tenta de novo e concede]--> Blindado
Blindado --[usuário remove proteção pelo app]--> NaoConfigurado
Blindado --[permissão revogada/assumida por outro app, detectado ao retomar o app]-->
    Erro(PERMISSAO_REVOGADA)
Erro(PERMISSAO_REVOGADA) --[usuário reativa]--> Blindado
```

Não existe um estado "Instalado mas desativado" equivalente ao do app iOS — no Android a
ativação é atômica (permissão concedida = VPN conectada imediatamente), então esse estado
intermediário do iOS não tem equivalente real aqui (documentado também em `spec.md` §
Assumptions).

## ProtectionLevel

```kotlin
enum class ProtectionLevel {
    PADRAO,        // bloqueia anúncios/rastreadores
    FAMILIA,       // também bloqueia conteúdo adulto
    PERSONALIZADO, // usuário define o servidor DoH
}
```

## DnsProvider

```kotlin
data class DnsProvider(
    val id: String,               // ex.: "adguard", "control-d", "custom"
    val displayName: String,
    val dohEndpoint: String,      // URL HTTPS do endpoint DoH (RFC 8484, POST)
)
```

Os provedores reconhecidos (AdGuard DNS, Control D) têm endpoints diferentes por
`ProtectionLevel` (ver `research.md` #6) — a task de implementação define a estrutura exata de
mapeamento nível→lista de provedores suportados.

## ProtectionProfile

A escolha persistida do usuário (DataStore Preferences).

```kotlin
data class ProtectionProfile(
    val level: ProtectionLevel,
    val providerId: String,        // referencia um DnsProvider.id suportado pelo level,
                                     // ou "custom" quando level == PERSONALIZADO
    val customDohUrl: String? = null, // só preenchido quando providerId == "custom"
)
```

**Regra de validação**: quando `level == PERSONALIZADO`, `customDohUrl` DEVE ser uma URL HTTPS
válida e alcançável (FR-007) — validado antes de persistir, nunca depois.

**Valor padrão** (primeira instalação, antes de qualquer escolha do usuário): `ProtectionLevel.PADRAO`
com o primeiro provedor padrão suportado — mesma convenção do app iOS (nunca deixar o usuário
sem um nível/provedor implícito ao ativar pela primeira vez).

## TestResult

Resultado de uma execução da tela de Teste (User Story 3).

```kotlin
data class TestResult(
    val items: List<DomainCheck>,
    val overall: OverallResult,
)

data class DomainCheck(
    val domain: String,
    val category: String,   // ex.: "Anúncios/rastreadores", "Comum"
    val status: DomainStatus,
)

enum class DomainStatus { BLOQUEADO, ACESSIVEL, VERIFICANDO }

enum class OverallResult { PROTEGIDO, DESPROTEGIDO, INDETERMINADO }
```

**Regra** (FR-012): `OverallResult.INDETERMINADO` sempre que não há conectividade de rede real
disponível para testar — nunca inferido como PROTEGIDO nem DESPROTEGIDO nesse caso.
