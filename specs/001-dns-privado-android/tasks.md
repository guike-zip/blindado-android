---

description: "Task list for Blindado Android — DNS Privado e Bloqueio em Todo o Sistema"

---

# Tasks: DNS Privado e Bloqueio em Todo o Sistema (Blindado Android)

**Input**: Design documents from `/specs/001-dns-privado-android/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/protection-managing.md, quickstart.md

**Tests**: incluídas — a constituição do projeto (Princípio V) exige cobertura de teste unitário
para todo ViewModel e para lógica de domínio, não é opcional aqui.

**Organization**: Tasks agrupadas por história de usuário para permitir implementação e teste
independentes de cada uma.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa incompleta)
- **[Story]**: A qual história de usuário a tarefa pertence (US1, US2, US3, US4)
- Caminhos de arquivo exatos incluídos em cada descrição

## Path Conventions

Projeto único (Gradle module `app/`), pacote raiz `io.blindado.android` — ver `plan.md` →
Project Structure para o layout completo.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do projeto Gradle/Android Studio

- [X] T001 Criar o módulo Gradle `app/` com pacote raiz `io.blindado.android`, conforme o layout
      de `plan.md` → Project Structure
- [X] T002 Configurar `app/build.gradle.kts`: Compose BOM + Material 3, AndroidX
      Lifecycle-ViewModel-Compose, AndroidX DataStore Preferences, Kotlin Coroutines;
      `minSdk = 26`, `targetSdk`/`compileSdk` na API estável mais recente disponível
      (`research.md` #1) — sem OkHttp/Retrofit nem nenhuma dependência de rede de terceiros
      (Princípio II)
- [X] T003 [P] Configurar lint (Android Lint + `ktlint` ou equivalente) para builds de Release
      sem warnings suprimidos (Padrões de Qualidade e Segurança da constituição)
- [X] T004 [P] Criar `AndroidManifest.xml` base com as permissões
      `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`,
      `android.permission.FOREGROUND_SERVICE` (cada uma justificada por uma história de usuário
      ativa, per a constituição)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central que TODAS as histórias de usuário dependem

**⚠️ CRITICAL**: nenhuma história de usuário pode começar antes desta fase estar completa

- [X] T005 [P] Criar `ProtectionState` (sealed interface com `NaoConfigurado`/`Blindado`/
      `Erro(ErrorReason)`) e `ErrorReason` (`PERMISSAO_NEGADA`/`PERMISSAO_REVOGADA`/
      `FALHA_DESCONHECIDA`) em `app/src/main/java/io/blindado/android/domain/ProtectionState.kt`
      — exatamente como definido em `data-model.md` § ProtectionState, incluindo a state machine
      documentada lá
- [X] T006 [P] Criar `ProtectionLevel` (enum `PADRAO`/`FAMILIA`/`PERSONALIZADO`) em
      `app/src/main/java/io/blindado/android/domain/ProtectionLevel.kt`
- [X] T007 [P] Criar `DnsProvider` (data class `id`/`displayName`/`dohEndpoint`) em
      `app/src/main/java/io/blindado/android/domain/DnsProvider.kt`
- [X] T008 [P] Criar `ProtectionProfile` (data class `level`/`providerId`/`customDohUrl`) em
      `app/src/main/java/io/blindado/android/domain/ProtectionProfile.kt` com a regra de
      `data-model.md`: quando `level == PERSONALIZADO`, `customDohUrl` DEVE ser uma URL HTTPS
      válida e alcançável antes de persistir (FR-007); valor padrão de primeira instalação é
      `ProtectionLevel.PADRAO` com o primeiro provedor suportado
- [X] T009 [P] Criar `TestResult`/`DomainCheck`/`DomainStatus`/`OverallResult` em
      `app/src/main/java/io/blindado/android/domain/TestResult.kt` — `OverallResult.INDETERMINADO`
      é obrigatório (nunca `PROTEGIDO`/`DESPROTEGIDO`) quando não há conectividade real (FR-012)
- [X] T010 Definir a interface `ProtectionManaging` e `ProtectionException(reason: ErrorReason,
      cause: Throwable?)` em `app/src/main/java/io/blindado/android/protection/ProtectionManaging.kt`
      exatamente conforme `contracts/protection-managing.md` (depends on T005, T008)
- [X] T011 [P] Implementar `FakeProtectionManaging` (implementação em memória, permite forçar
      qualquer `ProtectionState` incluindo `Erro`) em
      `app/src/main/java/io/blindado/android/fake/FakeProtectionManaging.kt` — satisfaz as 5
      regras do contrato de teste em `contracts/protection-managing.md` (depends on T010)
- [X] T012 [P] Implementar `ProtectionProfileStore` (DataStore Preferences, persiste
      `ProtectionProfile`) em
      `app/src/main/java/io/blindado/android/data/ProtectionProfileStore.kt` (depends on T008)
- [X] T013 [P] Configurar o tema Compose (Material 3, claro/escuro, tipografia em `sp`) em
      `app/src/main/java/io/blindado/android/ui/theme/` (Princípio VI)
- [X] T014 Criar `BlindadoApplication.kt` e `MainActivity.kt` com o scaffold de navegação
      (bottom navigation Material 3 com três destinos: Início, Testar, Ajustes — três, não
      quatro, per `spec.md` § Assumptions) em
      `app/src/main/java/io/blindado/android/{BlindadoApplication.kt,MainActivity.kt}` (depends
      on T013)

**Checkpoint**: fundação pronta — a implementação das histórias de usuário pode começar

---

## Phase 3: User Story 1 - Blindar o aparelho (Priority: P1) 🎯 MVP

**Goal**: usuário ativa a proteção inteiramente dentro do app via o diálogo nativo de permissão
de VPN do Android, vê o estado real refletido honestamente (incluindo quando a permissão é
revogada por fora do app), e pode remover a proteção a qualquer momento.

**Independent Test**: instalar em Android físico, tocar "Blindar meu Android", conceder a
permissão no diálogo nativo, confirmar mudança para "Blindado" sem sair do app; revogar a
permissão pelos Ajustes do sistema e confirmar que o app reflete o estado real ao retomar;
remover a proteção pelo app e confirmar volta a "Não configurado" (roteiro completo em
`quickstart.md` § US1).

### Tests for User Story 1

- [X] T015 [P] [US1] Teste unitário de `HomeViewModel` cobrindo as 5 transições de
      `data-model.md` § ProtectionState (incluindo `Erro(PERMISSAO_REVOGADA)` forçado via
      `FakeProtectionManaging`) em
      `app/src/test/java/io/blindado/android/ui/home/HomeViewModelTest.kt`
- [X] T016 [P] [US1] Teste unitário confirmando que uma falha de `install()` sempre carrega um
      `ErrorReason` real (nunca um motivo inventado — FR-005) em
      `app/src/test/java/io/blindado/android/protection/ProtectionManagingContractTest.kt`

### Implementation for User Story 1

- [X] T017 [US1] Implementar `BlindadoVpnService` (extends `android.net.VpnService`) com
      `Builder.addAddress()`/`addDnsServer()`/`addRoute()` apontando só para o endereço virtual
      local (research.md #3 — sem rota `0.0.0.0/0`), e `startForeground()` com notificação
      persistente e o `foregroundServiceType` de `research.md` #2 (a ser confirmado em
      dispositivo físico — não commitar essa parte sem validar, ver T046) em
      `app/src/main/java/io/blindado/android/protection/BlindadoVpnService.kt` (depends on T010)
- [X] T018 [P] [US1] Implementar `DohResolver`: requisição POST RFC 8484 (`Content-Type:
      application/dns-message`, corpo binário wire-format) via `HttpsURLConnection` nativo em
      `app/src/main/java/io/blindado/android/protection/DohResolver.kt` (research.md #4)
- [X] T019 [P] [US1] Implementar `DnsPacketCodec` (parsing/encoding mínimo de pacote DNS UDP
      porta 53) em `app/src/main/java/io/blindado/android/protection/DnsPacketCodec.kt`
- [X] T020 [US1] Implementar `BlockList` (carrega `res/raw/blocklist.txt`, expõe checagem de
      domínio bloqueado por sufixo) em
      `app/src/main/java/io/blindado/android/protection/BlockList.kt` (research.md #5)
- [X] T021 [P] [US1] Adicionar `app/src/main/res/raw/blocklist.txt` com uma lista inicial
      curada de domínios conhecidos de anúncios/rastreadores (um domínio por linha)
- [X] T022 [US1] Implementar o adaptador `ProtectionManaging` real ao redor de
      `BlindadoVpnService` (`install`/`remove`/`updateProfile`/`currentState`/`stateChanges`)
      em `app/src/main/java/io/blindado/android/protection/RealProtectionManaging.kt` (depends
      on T017, T018, T019, T020)
- [X] T023 [US1] Registrar `BlindadoVpnService` no `AndroidManifest.xml` com
      `android:permission="android.permission.BIND_VPN_SERVICE"` e o `intent-filter` para
      `android.net.VpnService`, mais o `foregroundServiceType` e o metadata
      `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` de `research.md` #2 (depends on T017)
- [X] T024 [US1] Implementar `HomeViewModel` (expõe `ProtectionState`, ações `blindar()`/
      `remover()`) em `app/src/main/java/io/blindado/android/ui/home/HomeViewModel.kt` (depends
      on T010, T022)
- [X] T025 [US1] Implementar `HomeScreen` composable: indicador visual grande de estado, botão
      "Blindar meu Android", botão de remover proteção, mensagem de erro usando o `ErrorReason`
      real (nunca um diagnóstico inventado — FR-005), `contentDescription`/semantics para
      TalkBack em cada elemento (Princípio VI) em
      `app/src/main/java/io/blindado/android/ui/home/HomeScreen.kt` (depends on T024)
- [X] T026 [US1] Integrar o fluxo de permissão `VpnService.prepare()` +
      `ActivityResultContracts.StartActivityForResult` em `MainActivity.kt`/`HomeScreen.kt`,
      encaminhando o resultado (concedida/negada) para `HomeViewModel` (depends on T024, T025)
- [X] T027 [US1] Implementar a detecção de permissão revogada externamente (observar no
      `onResume`/ciclo de vida e emitir em `stateChanges` — FR-004) em `RealProtectionManaging`
      (depends on T022)

**Checkpoint**: neste ponto, a User Story 1 deve estar totalmente funcional e testável de forma
independente — rodar `quickstart.md` § US1 em dispositivo físico antes de prosseguir.

---

## Phase 4: User Story 2 - Escolher o nível de proteção (Priority: P1)

**Goal**: usuário escolhe entre Padrão/Família/Personalizado, com validação de URL DoH
personalizada e troca de provedor sem reconectar manualmente.

**Independent Test**: com a proteção ativa, trocar entre os três níveis e confirmar persistência
após reabrir o app; endereço inválido em Personalizado é rejeitado com mensagem clara antes de
salvar (roteiro completo em `quickstart.md` § US2).

### Tests for User Story 2

- [X] T028 [P] [US2] Teste unitário de `ProtectionLevelViewModel` cobrindo troca de nível,
      troca de provedor dentro do nível, e validação de URL personalizada (válida/inválida) em
      `app/src/test/java/io/blindado/android/ui/level/ProtectionLevelViewModelTest.kt`

### Implementation for User Story 2

- [X] T029 [US2] Definir o catálogo de provedores suportados por nível (AdGuard DNS e Control D,
      hostnames DoH confirmados contra a documentação oficial de cada provedor — não inventados
      — research.md #6) em
      `app/src/main/java/io/blindado/android/protection/ProviderCatalog.kt`
- [X] T030 [P] [US2] Implementar `DohUrlValidator` (valida HTTPS + alcançabilidade de uma URL
      DoH personalizada antes de salvar — FR-007) em
      `app/src/main/java/io/blindado/android/protection/DohUrlValidator.kt`
- [X] T031 [US2] Implementar `ProtectionLevelViewModel` em
      `app/src/main/java/io/blindado/android/ui/level/ProtectionLevelViewModel.kt` (depends on
      T012, T022, T029, T030)
- [X] T032 [US2] Implementar `ProtectionLevelScreen` composable (seletor de nível, seletor de
      provedor, campo de URL personalizada com erro inline) com semantics para TalkBack em
      `app/src/main/java/io/blindado/android/ui/level/ProtectionLevelScreen.kt` (depends on
      T031)

**Checkpoint**: User Stories 1 e 2 devem funcionar juntas de forma independente.

---

## Phase 5: User Story 3 - Testar a proteção (Priority: P2)

**Goal**: usuário roda um teste item a item contra domínios conhecidos e vê um resultado geral
honesto, incluindo o caso "sem rede" (nunca falsamente Protegido/Desprotegido).

**Independent Test**: com proteção ativa e desativada, resultado muda de acordo; em modo avião,
resultado é sempre "Indeterminado" (roteiro completo em `quickstart.md` § US3).

### Tests for User Story 3

- [X] T033 [P] [US3] Teste unitário de `TestViewModel`: `OverallResult.INDETERMINADO` sem rede
      (FR-012), `PROTEGIDO`/`DESPROTEGIDO` conforme `FakeProtectionManaging` em
      `app/src/test/java/io/blindado/android/ui/test/TestViewModelTest.kt`

### Implementation for User Story 3

- [X] T034 [P] [US3] Definir a lista fixa de domínios de teste (anúncios/rastreadores + um
      domínio comum) em `app/src/main/java/io/blindado/android/protection/TestDomains.kt`
- [X] T035 [P] [US3] Implementar `NetworkStatus` (checagem de conectividade real via
      `ConnectivityManager`) em `app/src/main/java/io/blindado/android/protection/NetworkStatus.kt`
- [X] T036 [US3] Implementar `TestViewModel` (roda a checagem item a item sem travar a tela,
      agrega `OverallResult`) em `app/src/main/java/io/blindado/android/ui/test/TestViewModel.kt`
      (depends on T009, T034, T035)
- [X] T037 [US3] Implementar `TestScreen` composable (lista de itens com status individual,
      banner de resultado geral) com semantics para TalkBack em
      `app/src/main/java/io/blindado/android/ui/test/TestScreen.kt` (depends on T036)

**Checkpoint**: User Stories 1, 2 e 3 devem funcionar juntas de forma independente.

---

## Phase 6: User Story 4 - Transparência (Priority: P3)

**Goal**: usuário entende, em linguagem simples, o que o app faz e não faz, com o provedor DNS
atual nomeado e link para a política de privacidade completa.

**Independent Test**: texto exibido bate com o comportamento real do app; link da política abre
o conteúdo completo (roteiro completo em `quickstart.md` § US4).

### Tests for User Story 4

- [X] T038 [P] [US4] Teste unitário de `SettingsViewModel`: nome do provedor atual exibido bate
      com o `ProtectionProfile` persistido em
      `app/src/test/java/io/blindado/android/ui/settings/SettingsViewModelTest.kt`

### Implementation for User Story 4

- [X] T039 [P] [US4] Redigir o texto da tela de Transparência (o que o app faz/não faz, mesmo
      recorte de `spec.md` § User Story 4) e o link para a política de privacidade completa
- [X] T040 [US4] Implementar `SettingsViewModel` (expõe provedor DNS atual + texto de
      privacidade) em `app/src/main/java/io/blindado/android/ui/settings/SettingsViewModel.kt`
      (depends on T012)
- [X] T041 [US4] Implementar `SettingsScreen` composable (texto de Transparência + link) com
      semantics para TalkBack em
      `app/src/main/java/io/blindado/android/ui/settings/SettingsScreen.kt` (depends on T040)

**Checkpoint**: todas as quatro histórias de usuário devem estar funcionais de forma
independente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: validações finais que afetam múltiplas histórias de usuário

**Nota (2026-09-24)**: T042/T046 foram parcialmente validados num **emulador** Android 14
(Pixel 7, `google_apis/arm64-v8a`), não num dispositivo físico — ainda contam como pendentes
para fins de release, mas os resultados foram reais e positivos, não simulados: US1 completo
(permissão → diálogo nativo do sistema → `establish()` bem-sucedido → ícone de chave 🔑 real na
barra de status → estado "Blindado" → "Remover proteção" → ícone some, estado volta a "Não
configurado"), navegação para "Nível de proteção" com seleção de nível/provedor funcionando
(US2), tela de Teste e Ajustes renderizando corretamente (US3/US4). Nenhum crash no logcat. O
`foregroundServiceType="specialUse"` de `research.md` #2 funcionou sem erro nesse emulador — um
dado a favor, mas emulador não é garantia de comportamento idêntico em hardware real de
fabricantes diferentes (Samsung, Xiaomi etc. costumam ter gerenciamento de bateria mais agressivo
que o AOSP/emulador) — T042/T046 continuam abertas até validação em aparelho físico.

- [ ] T042 Rodar a validação completa de `quickstart.md` (todas as 4 histórias) em pelo menos
      um dispositivo Android físico
- [X] T043 [P] Auditoria de acessibilidade TalkBack em todas as telas (rótulos descritivos,
      fator de escala de fonte do sistema respeitado — Princípio VI) — validado em emulador via
      `uiautomator dump` (árvore de acessibilidade real, não inspeção de código): escudo de
      estado tem `content-desc` correto ("Não configurado"/etc.), botão principal tem
      `content-desc="Blindar meu Android"`, textos de título/corpo são lidos normalmente pelo
      TalkBack via texto visível. Fator de escala testado em 1.3x via
      `adb shell settings put system font_scale 1.3` — texto escalou corretamente, sem cortar
      nem sobrepor (confirma uso de `sp`, não `dp`, no texto). Pendente: rodar com o serviço
      TalkBack de verdade ligado (não só a árvore de acessibilidade) em aparelho físico.
- [X] T044 [P] QA visual em tema claro e tema escuro em todas as telas — tema escuro testado em
      emulador via `adb shell cmd uimode night yes`: Início e Testar renderizaram corretamente
      (fundo escuro, texto claro, botão verde com texto escuro — contraste adequado). Tema claro
      já validado nas capturas anteriores desta sessão.
- [X] T045 Confirmar que o build de Release compila sem nenhum warning de lint suprimido
      (Padrões de Qualidade e Segurança da constituição) — `./gradlew lintDebug` rodou limpo
      (`BUILD SUCCESSFUL`, `abortOnError = true` no `app/build.gradle.kts` garante que qualquer
      erro real interromperia o build). Falta rodar especificamente contra `lintRelease`/
      `assembleRelease` quando houver uma configuração de assinatura de release (fora do escopo
      desta feature).
- [ ] T046 Confirmar em dispositivo físico o comportamento real do `foregroundServiceType`
      decidido em `research.md` #2 (pendência sinalizada explicitamente naquele documento) e
      atualizar `research.md` com o resultado observado

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — pode começar imediatamente
- **Foundational (Phase 2)**: depende da conclusão do Setup — BLOQUEIA todas as histórias de
  usuário
- **User Stories (Phase 3-6)**: todas dependem da conclusão da Fase Foundational
  - US1 e US2 são ambas P1 — US2 depende de `RealProtectionManaging` (T022, de US1) para
    `updateProfile()`, então na prática US1 deve ser concluída (ou pelo menos T010-T022) antes
    de US2 ser testável de ponta a ponta, mesmo que os arquivos de US2 possam ser escritos em
    paralelo
  - US3 depende só da Fase Foundational (usa `FakeProtectionManaging`/`RealProtectionManaging`
    já existente de US1 para refletir o estado real, mas sua própria lógica de teste de domínio
    é independente)
  - US4 depende só de `ProtectionProfileStore` (Fase Foundational) — a mais independente das
    quatro
- **Polish (Phase 7)**: depende de todas as histórias de usuário desejadas estarem completas

### Parallel Opportunities

- Todas as tasks [P] da Fase Setup podem rodar em paralelo
- Todas as tasks [P] da Fase Foundational podem rodar em paralelo (T005-T009, depois T011-T013
  em paralelo entre si)
- Dentro de cada história, os testes marcados [P] podem rodar em paralelo entre si
- US3 e US4 podem ser desenvolvidas em paralelo por pessoas diferentes assim que a Fase
  Foundational e T022 (de US1) estiverem prontas

---

## Parallel Example: User Story 1

```bash
# Depois da Fase Foundational, lançar os testes de US1 juntos:
Task: "Teste unitário de HomeViewModel em app/src/test/java/io/blindado/android/ui/home/HomeViewModelTest.kt"
Task: "Teste unitário do contrato ProtectionManaging (ErrorReason real) em app/src/test/java/io/blindado/android/protection/ProtectionManagingContractTest.kt"

# Lançar os componentes de protocolo/rede de US1 em paralelo (arquivos independentes):
Task: "Implementar DohResolver em app/src/main/java/io/blindado/android/protection/DohResolver.kt"
Task: "Implementar DnsPacketCodec em app/src/main/java/io/blindado/android/protection/DnsPacketCodec.kt"
Task: "Adicionar app/src/main/res/raw/blocklist.txt"
```

---

## Implementation Strategy

### MVP First (User Story 1 apenas)

1. Completar Fase 1: Setup
2. Completar Fase 2: Foundational (CRÍTICO — bloqueia todas as histórias)
3. Completar Fase 3: User Story 1
4. **PARAR e VALIDAR**: testar User Story 1 de forma independente em dispositivo físico
   (`quickstart.md` § US1)
5. Nesse ponto já existe um MVP demonstrável: ativar/desativar proteção com bloqueio
   system-wide, mesmo sem escolha de nível (fixo em Padrão) nem tela de teste

### Incremental Delivery

1. Setup + Foundational → fundação pronta
2. Adicionar US1 → testar independentemente → MVP demonstrável
3. Adicionar US2 → testar independentemente
4. Adicionar US3 → testar independentemente
5. Adicionar US4 → testar independentemente
6. Cada história adiciona valor sem quebrar as anteriores

---

## Notes

- [P] = arquivos diferentes, sem dependência entre si
- Verificar que os testes falham antes de implementar (quando aplicável)
- Fazer commit depois de cada task ou grupo lógico
- Parar em qualquer checkpoint para validar a história de forma independente
- T017/T023/T046 (tudo relacionado a `foregroundServiceType` e ao comportamento real da
  `VpnService`) carregam a maior incerteza técnica do plano (research.md #2) — tratar com
  verificação em dispositivo físico antes de considerar T023 realmente concluída, nunca assumir
  o valor documentado como definitivo sem checar
