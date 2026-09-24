# Implementation Plan: DNS Privado e Bloqueio em Todo o Sistema (Blindado Android)

**Branch**: `001-dns-privado-android` | **Date**: 2026-09-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-dns-privado-android/spec.md`

## Summary

App Android nativo (Kotlin + Jetpack Compose) que protege o aparelho com DNS criptografado
(DoH) em todo o sistema via uma `VpnService` local, e bloqueia domínios conhecidos de anúncios/
rastreadores/conteúdo adulto para qualquer app instalado, na própria camada de DNS dentro dessa
VPN local — sem servidor remoto do Blindado. Abordagem técnica: a `VpnService` cria uma
interface TUN local, intercepta apenas os pacotes destinados à porta 53 (DNS) dessa interface,
resolve cada consulta via DoH usando `HttpsURLConnection`, decide bloquear/permitir comparando o
domínio pedido contra uma lista estática embutida no app, e escreve de volta a resposta (real ou
sintética de bloqueio) na mesma interface TUN. Todo o restante do tráfego (não-DNS) é
roteado de volta ao sistema sem inspeção nem cópia adicional de payload, via uma rota de exceção
para os IPs dos provedores DoH mais um NAT simples de portas ainda a especificar no `research.md`.

## Technical Context

**Language/Version**: Kotlin 2.x (versão exata fixada no `libs.versions.toml` do projeto),
JVM target 17.

**Primary Dependencies**: Jetpack Compose BOM + Material 3, AndroidX Lifecycle/ViewModel-Compose,
AndroidX DataStore Preferences, Kotlin Coroutines (stdlib). `HttpsURLConnection` nativo do
Android para DoH — nenhuma dependência de terceiros para rede (Princípio II da constituição).

**Storage**: AndroidX DataStore Preferences (arquivo local, substitui SharedPreferences) para
persistir `ProtectionProfile` (nível + provedor/URL personalizada escolhidos). Nenhum banco de
dados relacional é necessário — não há dados estruturados além dessa preferência única e da
lista estática de domínios bloqueados, que é um recurso embutido no APK (`res/raw` ou asset),
não gravável em runtime nesta v1.

**Testing**: JUnit 5 + `kotlinx-coroutines-test` para ViewModels e lógica de domínio (parsing de
pacote DNS, matching de domínio bloqueado, validação de URL DoH personalizada) — tudo rodável em
JVM local (`testImplementation`), sem emulador. Testes instrumentados (`androidTest`) só para o
que realmente exige um dispositivo real (verificação manual via `quickstart.md`, não suíte
automatizada, já que `VpnService` não é confiável em CI/emulador — Princípio V/VIII da
constituição).

**Target Platform**: Telefones Android, minSdk 26 (Android 8.0) / targetSdk e compileSdk na API
estável mais recente disponível no momento do build (documentado em `research.md` #1 junto com o
racional do minSdk).

**Project Type**: App móvel nativo, projeto único (não há backend nem componente web).

**Performance Goals**: Resolução DNS local com overhead perceptível abaixo de ~150ms adicionais
por consulta não cacheada frente a uma consulta DNS UDP direta (a maior parte da latência é
inerente ao round-trip HTTPS do DoH, não ao processamento local); tráfego não-DNS não deve ter
overhead de cópia perceptível (roteamento O(1) por pacote, sem parsing de payload não-DNS).

**Constraints**: Serviço de VPN em primeiro plano deve sobreviver ao Doze/App Standby do sistema
(notificação persistente obrigatória, FR-015); nenhuma dependência de rede de terceiros além do
provedor DoH escolhido pelo usuário e do teste de domínios da User Story 3 (Princípio I); app
deve funcionar 100% offline exceto pela funcionalidade de DNS em si (não há tela que exija
conexão para ser navegável).

**Scale/Scope**: 4 telas principais (Início, Nível de Proteção, Testar, Ajustes/Transparência),
~5 entidades de domínio, um único usuário local por instalação (sem multi-conta, sem sync).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Verificação | Status |
|---|---|---|
| I. Privacidade Absoluta | Nenhum SDK de analytics/crash de terceiros no plano; único tráfego de rede é DoH do provedor escolhido + teste de domínios (US3) | PASS |
| II. Apenas Jetpack/AndroidX | Dependências listadas em Technical Context são só AndroidX/Compose + Kotlin stdlib; DoH via `HttpsURLConnection` nativo, sem OkHttp/Retrofit | PASS |
| III. Conformidade Google Play | Divulgação de dados (Data Safety) e alegação de bloqueio system-wide ficam para o research/quickstart de submissão (fora do escopo deste plano técnico, mas registrado como dependência futura) | PASS (com follow-up fora do código) |
| IV. Honestidade com o Usuário | `ProtectionState` deve ser derivado do estado real da `VpnService`/permissão a cada leitura, nunca cacheado — detalhado em `data-model.md` | PASS |
| V. Testabilidade | Interface `ProtectionManaging` isola toda a `VpnService` real atrás de um contrato com fake injetável — ver `contracts/` | PASS |
| VI. Acessibilidade | Compose exige `contentDescription`/semantics explícitos nas telas geradas nas tasks — critério de aceite, não de arquitetura; sem violação estrutural aqui | PASS |
| VII. Simplicidade | Uma única interface de domínio (`ProtectionManaging`) e ViewModels por tela, sem camada de repositório especulativa além do necessário para persistência (DataStore) | PASS |
| VIII. Entrega Independente | Estrutura de módulo único não impede entrega incremental por história de usuário — cada tela/ViewModel é isolável | PASS |
| IX. Modelo de Negócio | Não aplicável à arquitetura técnica (decisão de Play Console, não de código) | N/A |

Nenhuma violação a justificar — **Complexity Tracking** fica vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-dns-privado-android/
├── plan.md              # Este arquivo
├── research.md           # Fase 0
├── data-model.md         # Fase 1
├── quickstart.md         # Fase 1
├── contracts/             # Fase 1 — contrato da interface ProtectionManaging
└── tasks.md              # Fase 2 (/speckit-tasks, não gerado por este comando)
```

### Source Code (repository root)

Opção "Mobile app" de projeto único — módulo Gradle único `app/` (sem multi-módulo especulativo;
se o app crescer o suficiente para justificar separação, isso é uma decisão futura, não
antecipada agora — Princípio VII).

```text
app/
├── src/main/java/io/blindado/android/
│   ├── BlindadoApplication.kt
│   ├── MainActivity.kt
│   ├── domain/
│   │   ├── ProtectionState.kt
│   │   ├── ProtectionLevel.kt
│   │   ├── DnsProvider.kt
│   │   ├── ProtectionProfile.kt
│   │   └── TestResult.kt
│   ├── protection/
│   │   ├── ProtectionManaging.kt        # interface (Princípio V)
│   │   ├── BlindadoVpnService.kt        # implementação real (VpnService)
│   │   ├── DohResolver.kt               # cliente HttpsURLConnection para DoH
│   │   ├── DnsPacketCodec.kt            # parsing/encoding mínimo de pacote DNS
│   │   └── BlockList.kt                 # carrega/consulta a lista estática embutida
│   ├── data/
│   │   └── ProtectionProfileStore.kt    # DataStore Preferences
│   ├── ui/
│   │   ├── home/            # HomeScreen + HomeViewModel (US1)
│   │   ├── level/           # ProtectionLevelScreen + ViewModel (US2)
│   │   ├── test/            # TestScreen + ViewModel (US3)
│   │   ├── settings/        # SettingsScreen/TransparencyScreen + ViewModel (US4)
│   │   └── theme/           # Material 3 theme, tokens equivalentes ao design do iOS
│   └── fake/
│       └── FakeProtectionManaging.kt    # implementação fake p/ Previews e testes
├── src/main/res/raw/blocklist.txt        # lista estática de domínios bloqueados
├── src/test/java/io/blindado/android/    # testes unitários (JVM, sem device)
└── build.gradle.kts
```

**Structure Decision**: módulo Gradle único (`app/`) com pacotes por camada
(`domain`/`protection`/`data`/`ui`), refletindo o mesmo espírito MVVM enxuto do app irmão de iOS
(`Blindado/Services`, `Blindado/ViewModels`, `Blindado/Views`) sem introduzir módulos Gradle
separados — não há hoje um consumidor real (ex.: um segundo app, uma lib publicável) que
justifique a complexidade de multi-módulo (Princípio VII).

## Complexity Tracking

*Vazio — nenhuma violação da Constitution Check acima precisa de justificativa.*
