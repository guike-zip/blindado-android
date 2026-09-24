# Contrato: `ProtectionManaging`

A interface por trás da qual fica toda a lógica real de `VpnService` (Princípio V da
constituição — testável, com implementação fake injetável). Todos os ViewModels dependem só
desta interface, nunca de `BlindadoVpnService` diretamente.

```kotlin
interface ProtectionManaging {
    /** Estado atual real da proteção — nunca cacheado, sempre consultado ao vivo. */
    suspend fun currentState(): ProtectionState

    /**
     * Solicita a permissão de VPN do sistema (se necessário) e, se concedida, conecta a
     * VpnService com o perfil informado. Suspende até a conexão ser confirmada ou falhar.
     *
     * @throws ProtectionException com um [ErrorReason] real — nunca um motivo inventado
     *   (FR-005). Se a causa real não puder ser determinada pela API do Android,
     *   [ErrorReason.FALHA_DESCONHECIDA] é o único caso aceitável de motivo genérico, e mesmo
     *   esse deve carregar a mensagem/exceção original do sistema para diagnóstico.
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
```

## Implementações esperadas

- **`BlindadoVpnService` + um adaptador `ProtectionManaging` real**: implementação de produção,
  gerencia o ciclo de vida real da `android.net.VpnService` (ver `research.md` #2 e #3).
- **`FakeProtectionManaging`**: implementação em memória para testes unitários e `@Preview` do
  Compose — permite simular qualquer `ProtectionState` (incluindo `Erro`) sem VPN real, sem
  dispositivo físico, sem permissão do sistema.

## Contrato de teste (o que toda implementação DEVE satisfazer)

1. `currentState()` chamado logo após `install()` bem-sucedido DEVE retornar
   `ProtectionState.Blindado`.
2. `currentState()` chamado logo após `remove()` DEVE retornar `ProtectionState.NaoConfigurado`.
3. `updateProfile()` chamado enquanto o estado é `Blindado` NÃO DEVE emitir nenhum estado
   diferente de `Blindado` em `stateChanges` (a troca de nível/provedor é transparente para o
   usuário — FR-009).
4. `updateProfile()` chamado enquanto o estado NÃO é `Blindado` é um erro de uso — a
   implementação real deve rejeitar (exceção ou no-op documentado); a spec não define
   comportamento de UI para esse caso porque nenhuma tela deve permitir chamá-lo fora de ordem.
5. Uma implementação fake usada em testes DEVE permitir forçar `stateChanges` a emitir
   `Erro(PERMISSAO_REVOGADA)` a qualquer momento, para testar a User Story 1 / Acceptance
   Scenario 5 sem depender de hardware real.
