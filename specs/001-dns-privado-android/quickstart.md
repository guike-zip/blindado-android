# Quickstart: Blindado Android

## 1. Pré-requisitos

- Android Studio (versão estável mais recente) com o SDK correspondente ao `compileSdk`/
  `targetSdk` decidido em `research.md` #1 instalado.
- Um dispositivo Android físico para qualquer validação envolvendo `VpnService` — **não confiar
  em emulador** para nada além de compilar e rodar testes unitários JVM (Princípio V/VIII da
  constituição; mesma lição do app irmão de iOS, onde bugs reais de sistema só apareceram em
  hardware físico).
- Conta Google Play Console (para validação de ficha/política, fora do escopo desta feature
  técnica).

## 2. Gerar/abrir o projeto

```bash
cd blindado-android
# Abrir app/build.gradle.kts no Android Studio, ou:
./gradlew assembleDebug
```

## 3. Permissões e capabilities

- `android.permission.INTERNET` — necessária para as requisições DoH.
- `android.permission.ACCESS_NETWORK_STATE` — para detectar ausência de rede (FR-012, User
  Story 3, Acceptance Scenario 3).
- `android.permission.FOREGROUND_SERVICE` + o tipo declarado em `research.md` #2.
- Declaração de serviço `android:permission="android.permission.BIND_VPN_SERVICE"` com
  `intent-filter` para `android.net.VpnService` no Manifest, para a `BlindadoVpnService`.

## 4. Estrutura de código

Ver `plan.md` → Project Structure para o layout completo de pacotes.

## 5. Validação por história de usuário (dispositivo físico)

Rodar cada validação isoladamente, na ordem de prioridade (Constitution Princípio VIII).

### US1 — Blindar o aparelho (P1)

1. Instalar o app em um Android físico.
2. Abrir o app → confirmar escudo em "Não configurado".
3. Tocar "Blindar meu Android" → confirmar que o diálogo NATIVO de permissão de VPN do Android
   aparece, sem nenhuma navegação para fora do app antes disso.
4. Conceder a permissão → confirmar que o escudo muda para "Blindado" imediatamente, sem
   precisar reabrir o app nem visitar Ajustes.
5. Negar a permissão (em uma segunda tentativa, ou reinstalando) → confirmar que o app mostra um
   erro real e específico (não um "conflito" genérico) e oferece tentar de novo.
6. Com a proteção ativa, ir em Ajustes do sistema Android → Rede → VPN, e revogar a permissão do
   Blindado por lá (fora do app) → voltar ao app → confirmar que o escudo reflete o estado real
   (não continua mostrando "Blindado").
7. Remover a proteção pelo próprio app → confirmar que o escudo volta a "Não configurado" e que
   o ícone de VPN desaparece da barra de status do Android.
8. **Aceita** quando os estados batem com `ProtectionState` (`data-model.md`) em cada passo.

### US2 — Escolher o nível de proteção (P1)

1. Com a proteção ativa, trocar entre Padrão/Família/Personalizado.
2. Em Personalizado, testar uma URL inválida → confirmar rejeição com mensagem clara, sem
   salvar.
3. Testar uma URL DoH válida e alcançável → confirmar salvamento e reaplicação sem exigir nova
   concessão de permissão de VPN.
4. Fechar e reabrir o app → confirmar que o nível escolhido persiste.

### US3 — Testar a proteção (P2)

1. Com a proteção ativa, rodar o teste → confirmar itens de anúncio/rastreador como "bloqueado"
   e o domínio comum como "acessível"; resultado geral "Protegido".
2. Desativar a proteção → rodar o teste novamente → confirmar todos os itens como "acessível" e
   resultado geral "Desprotegido".
3. Ativar o modo avião → rodar o teste → confirmar resultado "Indeterminado" (nunca "Protegido"
   nem "Desprotegido").

### US4 — Transparência (P3)

1. Abrir Ajustes/Transparência → confirmar que o provedor de DNS atual é nomeado corretamente.
2. Tocar no link da política de privacidade → confirmar abertura do conteúdo completo.

## 6. Testes automatizados

```bash
./gradlew testDebugUnitTest
```

Deve cobrir: todos os ViewModels (com `FakeProtectionManaging`), o parsing/matching de domínio
bloqueado (`DnsPacketCodec`, `BlockList`), e a validação de URL DoH personalizada — tudo sem
dispositivo físico nem emulador.

## 7. Deploy (fora do escopo desta feature — placeholder para uma spec futura)

Assinatura de release, ficha da Google Play Console (incluindo o formulário de Data Safety —
Princípio III), e submissão ficam para quando o app estiver funcionalmente completo, seguindo o
mesmo padrão de disciplina usado no app irmão de iOS (fastlane + revisão manual antes de
qualquer submissão).
