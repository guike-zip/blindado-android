# Feature Specification: DNS Privado e Bloqueio em Todo o Sistema (Blindado Android)

**Feature Branch**: `001-dns-privado-android`

**Created**: 2026-09-24

**Status**: Draft

**Input**: User description: "Blindado para Android é um app nativo Kotlin + Jetpack Compose que protege a navegação do usuário usando DNS criptografado (DoH) em todo o sistema — Wi-Fi e dados móveis — via uma VpnService 100% local, e bloqueia anúncios/rastreadores conhecidos em qualquer app instalado, não só num navegador. Público: pessoas leigas que querem menos rastreadores e uma internet mais limpa sem pagar extra nem configurar nada técnico. Projeto irmão do Blindado iOS/macOS (specs/001-dns-privado-safari no repositório irmão), adaptado às diferenças reais da plataforma Android."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Blindar o aparelho (Priority: P1)

O usuário abre o app pela primeira vez e vê um indicador visual grande (escudo) mostrando o
estado real da proteção. Ao tocar em "Blindar meu Android", o app dispara o diálogo nativo de
permissão de VPN do próprio sistema Android; ao aceitar, a proteção conecta imediatamente, sem
o usuário precisar sair do app ou visitar uma tela de Ajustes separada (diferente do app irmão
de iOS, que exige uma confirmação manual fora do app). A partir do momento em que a proteção
está ativa, o bloqueio de domínios de anúncios/rastreadores já vale para qualquer aplicativo
instalado no aparelho, não apenas para um navegador específico — não existe um passo separado de
"habilitar num navegador". O usuário pode remover a proteção a qualquer momento pelo próprio
app, e o app detecta e reflete honestamente quando a proteção para de estar ativa por um motivo
fora do seu controle (outro app assumiu a permissão de VPN, o usuário revogou a permissão nos
Ajustes do sistema, o serviço foi encerrado pelo sistema).

**Why this priority**: É o valor central do produto — sem esta história o app não protege nada.
Precisa funcionar sozinha para o app ter propósito, e sozinha já cobre o diferencial de bloqueio
em todo o sistema (não só um navegador).

**Independent Test**: Pode ser testada integralmente instalando o app em um aparelho Android
físico, tocando em "Blindar meu Android", concedendo a permissão de VPN no diálogo do sistema, e
confirmando que o escudo muda para "Blindado" sem sair do app — e que a remoção pelo app reverte
o estado, e que revogar a permissão de VPN pelos Ajustes do sistema (fora do app) é detectado ao
reabrir o app.

**Acceptance Scenarios**:

1. **Given** o app nunca foi configurado, **When** o usuário abre o app, **Then** o escudo mostra
   o estado "Não configurado" e o botão principal oferece "Blindar meu Android".
2. **Given** o usuário tocou em "Blindar meu Android", **When** ele concede a permissão de VPN no
   diálogo nativo do Android, **Then** a proteção conecta imediatamente e o escudo muda para
   "Blindado" sem nenhuma navegação para fora do app.
3. **Given** o usuário tocou em "Blindar meu Android", **When** ele nega a permissão de VPN no
   diálogo nativo, **Then** o app permanece no estado "Não configurado" e explica, sem culpar um
   suposto conflito não verificado, que a permissão é necessária e oferece tentar de novo.
4. **Given** a proteção está ativa ("Blindado"), **When** o usuário escolhe remover a proteção
   pelo app, **Then** a VpnService é desconectada e o escudo volta ao estado "Não configurado".
5. **Given** a proteção está ativa ("Blindado"), **When** a permissão de VPN é revogada por fora
   do app (outro app de VPN assume, ou o usuário revoga nos Ajustes do sistema), **Then** ao
   reabrir ou retomar o app o escudo reflete o estado real ("Não configurado" ou um estado de
   erro claramente distinto), nunca continuando a mostrar "Blindado" de forma otimista.

---

### User Story 2 - Escolher o nível de proteção (Priority: P1)

O usuário escolhe entre três níveis de proteção: "Padrão" (bloqueia anúncios e rastreadores),
"Família" (também bloqueia conteúdo adulto) e "Personalizado" (o usuário informa o endereço de um
servidor DoH próprio). No modo Personalizado, o endereço informado é validado antes de ser salvo.
A escolha do usuário é lembrada, e trocar de nível reaplica a proteção com a nova configuração
sem exigir nova concessão de permissão de VPN. Os níveis "Padrão" e "Família" já vêm com um
provedor DoH padrão escolhido, apoiados por mais de um provedor de DNS criptografado reconhecido
(os mesmos dois provedores usados no app irmão de iOS, para manter a mesma proposta de não
depender de um único serviço), e o usuário pode opcionalmente ver e trocar qual provedor está em
uso dentro do nível escolhido.

**Why this priority**: Sem escolha de nível, o produto não atende às diferentes necessidades do
público (ex.: famílias) nem usuários avançados que já têm um provedor DNS de confiança. É P1
porque está diretamente ligada à ativação inicial.

**Independent Test**: Pode ser testada com a proteção já ativa, trocando entre os três níveis e
confirmando que a nova escolha permanece após fechar e reabrir o app, e que um endereço de
servidor inválido no modo Personalizado é rejeitado com uma mensagem clara antes de ser salvo.

**Acceptance Scenarios**:

1. **Given** a proteção está ativa no nível "Padrão", **When** o usuário seleciona "Família",
   **Then** a proteção é reconfigurada para o nível "Família" sem exigir nova concessão de
   permissão de VPN.
2. **Given** o usuário seleciona "Personalizado", **When** ele informa um endereço de servidor DoH
   válido, **Then** o app valida o endereço, salva a escolha e aplica a configuração.
3. **Given** o usuário seleciona "Personalizado", **When** ele informa um endereço inválido ou
   inacessível, **Then** o app exibe um erro claro e não salva a escolha.
4. **Given** o usuário já escolheu um nível de proteção, **When** ele reabre o app mais tarde,
   **Then** o nível escolhido continua selecionado.
5. **Given** o usuário está no nível "Padrão" ou "Família", **When** ele abre os detalhes do
   nível, **Then** o app mostra qual provedor de DNS está em uso e permite trocar para outro
   provedor suportado sem sair do nível escolhido.

---

### User Story 3 - Testar a proteção (Priority: P2)

O usuário acessa uma tela de teste que verifica, item a item, uma lista de domínios conhecidos de
anúncios/rastreadores e um domínio comum, mostrando se cada um foi bloqueado ou passou, além de
um resultado geral claro (Protegido / Desprotegido / Indeterminado).

**Why this priority**: Dá confiança de que a proteção realmente funciona em qualquer app, mas o
app já entrega valor sem ela (a proteção já está ativa via DNS do sistema independente do teste
existir).

**Independent Test**: Pode ser testada isoladamente com a proteção ativa e desativada,
confirmando que o resultado do teste muda de acordo com o estado real da proteção.

**Acceptance Scenarios**:

1. **Given** a proteção está ativa no nível "Padrão", **When** o usuário roda o teste, **Then**
   os domínios de anúncios/rastreadores da lista aparecem como bloqueados e o domínio comum
   aparece como acessível, com um resultado geral "Protegido".
2. **Given** a proteção está desativada, **When** o usuário roda o teste, **Then** todos os
   domínios da lista aparecem como acessíveis e o resultado geral indica "Desprotegido".
3. **Given** o aparelho está sem nenhuma conexão de rede, **When** o usuário roda o teste,
   **Then** o resultado geral indica "Indeterminado" — nunca "Protegido" nem "Desprotegido".
4. **Given** o teste está em andamento, **When** o usuário aguarda a conclusão, **Then** cada
   domínio testado mostra seu resultado individual assim que verificado, sem travar a tela.

---

### User Story 4 - Transparência (Priority: P3)

O usuário acessa uma tela de privacidade que explica, em linguagem simples, o que o app faz e não
faz: não é uma VPN de anonimização (não esconde IP nem localização, é só um túnel local para
DNS), não coleta nem transmite dados do usuário a um servidor do Blindado, e nomeia o provedor de
DNS criptografado atualmente em uso. A tela linka para a política de privacidade completa.

**Why this priority**: Reforça a confiança do usuário e cumpre a exigência de divulgação
transparente de apps de VPN nas políticas do Google Play, mas não bloqueia o valor central do
produto (a proteção já funciona sem essa tela existir).

**Independent Test**: Pode ser testada isoladamente conferindo que o texto exibido bate com o
comportamento real do app (nenhuma chamada de rede além das descritas) e que o link da política
de privacidade abre o conteúdo completo.

**Acceptance Scenarios**:

1. **Given** o usuário abre a tela de Transparência, **When** a proteção está ativa, **Then** o
   texto nomeia o provedor de DNS atualmente em uso e reafirma que nenhum dado é coletado.
2. **Given** o usuário está na tela de Transparência, **When** ele toca no link da política de
   privacidade, **Then** o conteúdo completo da política é aberto.

---

### Edge Cases

- O que acontece quando o usuário nega a permissão de VPN no diálogo do sistema? O app permanece
  em "Não configurado" e oferece tentar de novo, sem inventar um diagnóstico de "conflito" não
  verificado pela API real (lição aprendida no app irmão de iOS: uma versão anterior mascarava
  qualquer falha real como "conflito de DNS/VPN" sem checar a causa de verdade).
- O que acontece se o usuário já tem outro app de VPN ativo quando toca em "Blindar meu Android"?
  O Android só permite uma `VpnService` ativa por vez — o diálogo nativo do sistema já avisa que
  ativar o Blindado desconecta a VPN anterior; o app não precisa (nem deve) reimplementar essa
  checagem, só refletir o resultado real.
- O que acontece se o sistema encerrar o processo do app em segundo plano por gerenciamento de
  bateria? A proteção deve continuar ativa (é um serviço em primeiro plano com notificação
  persistente, não um processo comum), e o app deve reconciliar o estado exibido com o real ao
  ser reaberto.
- O que acontece se o endereço DoH personalizado ficar inacessível depois de já salvo (não só na
  hora de salvar)? O erro real deve ser exibido, nunca uma mensagem genérica de "conflito".
- O que acontece durante a troca de nível de proteção (Padrão → Família, etc.) se a nova
  configuração falhar ao aplicar? O nível anterior deve continuar ativo até a troca ser
  confirmada com sucesso — nunca deixar o usuário num estado intermediário não sinalizado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema DEVE exibir o estado real da proteção (Não configurado / Blindado / erro
  detectado) sempre sincronizado com o estado real da `VpnService` e da permissão de VPN do
  sistema, nunca um valor otimista ou em cache.
- **FR-002**: O sistema DEVE permitir ativar a proteção inteiramente dentro do app, via o diálogo
  nativo de permissão de VPN do Android, sem exigir navegação para uma tela de Ajustes do sistema
  separada.
- **FR-003**: O sistema DEVE permitir remover a proteção a qualquer momento pelo próprio app.
- **FR-004**: O sistema DEVE detectar quando a permissão de VPN foi revogada ou assumida por
  outro app fora do Blindado, e refletir isso no estado exibido na próxima vez que o app for
  aberto ou retomado.
- **FR-005**: O sistema DEVE, ao reportar uma falha de ativação, usar a causa real reportada pela
  API do Android — NUNCA atribuir uma causa não verificada (ex.: um "conflito" genérico) quando a
  causa real não foi confirmada programaticamente.
- **FR-006**: O sistema DEVE oferecer três níveis de proteção (Padrão, Família, Personalizado),
  cada um mapeado a um provedor DoH; Padrão e Família DEVEM funcionar sem nenhuma configuração
  adicional do usuário.
- **FR-007**: O sistema DEVE validar o endereço de servidor DoH informado no nível Personalizado
  antes de salvá-lo, rejeitando endereços inválidos ou inacessíveis com uma mensagem clara.
- **FR-008**: O sistema DEVE persistir o nível de proteção e o provedor escolhidos entre
  reaberturas do app.
- **FR-009**: O sistema DEVE reaplicar a proteção automaticamente ao trocar de nível ou provedor,
  sem exigir nova concessão de permissão de VPN.
- **FR-010**: O bloqueio de domínios de anúncios/rastreadores conhecidos DEVE se aplicar a
  qualquer aplicativo instalado no aparelho assim que a proteção estiver ativa, não apenas a um
  navegador específico.
- **FR-011**: O sistema DEVE fornecer uma tela de teste que verifica, item a item, uma lista fixa
  de domínios de anúncios/rastreadores mais um domínio comum, reportando bloqueado/acessível por
  item e um resultado geral (Protegido/Desprotegido/Indeterminado).
- **FR-012**: O resultado geral do teste DEVE ser "Indeterminado" (nunca Protegido nem
  Desprotegido) quando não há conexão de rede real disponível para testar.
- **FR-013**: O sistema DEVE fornecer uma tela de Transparência que nomeia o provedor de DNS
  atual, descreve exatamente quais dados o app acessa (nenhum coletado/transmitido além das
  consultas DNS e do teste de proteção), e linka para a política de privacidade completa.
- **FR-014**: O sistema NÃO DEVE coletar, transmitir ou armazenar qualquer dado do usuário em
  servidor próprio do Blindado — toda a resolução DNS acontece localmente dentro da `VpnService`.
- **FR-015**: O sistema DEVE manter a proteção ativa como um serviço em primeiro plano com
  notificação persistente, sobrevivendo ao gerenciamento de bateria do sistema.

### Key Entities

- **ProtectionState**: estado observável da proteção — Não configurado, Blindado, ou um estado de
  erro distinto (ex.: permissão negada, permissão revogada externamente). Nunca um valor
  em cache/otimista.
- **ProtectionLevel**: Padrão, Família ou Personalizado — determina qual `DnsProvider` (ou URL
  customizada) é usado.
- **DnsProvider**: um provedor de DNS criptografado suportado (hostname DoH + descrição), dos
  dois provedores reconhecidos usados também no app irmão de iOS, ou um endereço personalizado
  informado pelo usuário no nível Personalizado.
- **ProtectionProfile**: a combinação persistida de nível + provedor (ou URL personalizada)
  escolhidos pelo usuário, sobrevivendo a reaberturas do app.
- **TestResult**: o resultado de uma execução da tela de Teste — por domínio (bloqueado/
  acessível) e o resultado geral (Protegido/Desprotegido/Indeterminado).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um usuário consegue ativar a proteção em menos de 30 segundos a partir da primeira
  abertura do app, sem sair do app em nenhum momento do fluxo.
- **SC-002**: Uma vez ativa, a proteção bloqueia domínios de anúncios/rastreadores conhecidos
  para 100% dos aplicativos instalados testados, não apenas para um navegador.
- **SC-003**: Quando a permissão de VPN é revogada por fora do app, o app reflete o estado real
  (não mais "Blindado") na primeira vez que é reaberto ou retomado após a revogação, em 100% dos
  casos testados — zero casos de estado otimista incorreto.
- **SC-004**: Trocar de nível de proteção reaplica a nova configuração sem exigir nenhuma ação
  fora do app (sem nova concessão de permissão) em 100% das trocas.
- **SC-005**: O resultado da tela de Teste corresponde ao estado real da proteção (ativa/
  inativa/sem rede) em 100% das execuções.

## Assumptions

- **Sem tablet Android/Chromebook nesta primeira versão**: o app é otimizado só para telefone
  Android nesta v1, mesma decisão de escopo do app irmão de iOS (que exclui iPad).
- **Navegação simplificada para três seções (Início, Testar, Ajustes), não quatro**: o app irmão
  de iOS tem uma aba "Safari" separada porque o bloqueador de conteúdo do Safari precisa de um
  passo de ativação manual próprio, distinto da ativação do DNS. No Android, como o bloqueio já
  vale para todo o sistema assim que a proteção está ativa (FR-010), não existe esse passo
  separado — por isso não há uma aba de navegador dedicada; a mensagem de "funciona em qualquer
  app" fica na própria tela de Início (User Story 1) e na tela de Transparência.
- **Mesmos dois provedores DoH reconhecidos do app iOS** (AdGuard DNS e Control D) para os níveis
  Padrão e Família, mantendo a mesma proposta de não depender de um único serviço — assumindo que
  ambos oferecem endpoint DoH compatível para uso direto por um cliente Android sem SDK
  proprietário.
- **Modelo de negócio herdado do app irmão por padrão**: pago, download único, sem compras
  internas (Princípio IX da constituição do projeto) — sinalizado como suposição a confirmar
  explicitamente com o usuário antes de configurar preço na Google Play Console, não uma decisão
  definitiva desta spec.
- **Fora de escopo nesta v1** (mesmo recorte do app irmão de iOS, adaptado): ocultar IP ou
  localização real do usuário (não é uma VPN de privacidade de tráfego, só de DNS), bloqueio de
  conteúdo embutido em vídeo/anúncio que não dependa de um domínio bloqueável, contas de usuário,
  compras dentro do app, estatísticas de navegação, widgets, suporte a Wear OS/Android TV.
