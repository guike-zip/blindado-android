<!--
Sync Impact Report
Version change: (none) → 1.0.0
Modified principles: n/a (ratificação inicial)
Added sections:
  - Core Principles I–IX (todas novas)
  - Padrões de Qualidade e Segurança
  - Fluxo de Desenvolvimento
  - Governance
Removed sections: n/a
Deferred TODOs:
  - Princípio IX (modelo de negócio) herda a suposição do projeto irmão iOS (pago, download
    único, sem IAP) sem confirmação explícita do usuário para o Android — revisar antes de
    configurar preço na Play Console.
Templates requiring follow-up: specs/ ainda não existe neste repo — nenhuma feature spec para
  sincronizar ainda. A primeira spec (/speckit-specify) deve referenciar os Princípios II, III e
  V ao decidir a arquitetura da VpnService.
-->

# Blindado (Android) Constitution

## Core Principles

### I. Privacidade Absoluta
O aplicativo NÃO DEVE coletar dados do usuário, NÃO DEVE integrar SDKs de analytics ou
rastreamento, e NÃO DEVE operar servidor próprio. A `VpnService` roda inteiramente local no
aparelho — nenhum tráfego do usuário passa por infraestrutura do Blindado. Nenhuma chamada de
rede é permitida além de: (a) o teste de eficácia da proteção contra uma lista fixa de domínios,
e (b) as consultas DNS enviadas ao provedor DoH escolhido pelo usuário. SDKs de publicidade,
crash reporting de terceiros (Firebase Crashlytics, Sentry, etc.) ou qualquer telemetria estão
proibidos.
Rationale: privacidade é a proposta de valor central do produto; qualquer coleta de dados mina a
confiança do usuário e contradiz o propósito do app — especialmente relevante num app que pede
permissão de VPN, a permissão mais sensível que o Android expõe.

### II. Apenas Bibliotecas Jetpack/AndroidX Oficiais
O projeto DEVE usar exclusivamente o Android SDK e bibliotecas Jetpack/AndroidX oficiais do
Google (Compose, Lifecycle, DataStore, Navigation, etc.). Para a lógica de rede/DNS sensível à
privacidade (resolução DoH dentro da `VpnService`), DEVE usar `HttpsURLConnection` nativo do
Android em vez de adicionar OkHttp, Retrofit ou qualquer cliente HTTP de terceiros como
dependência. Qualquer exceção a esta regra EXIGE justificativa técnica explícita registrada no
plano de implementação da feature.
Rationale: reduz a superfície de ataque, elimina risco de supply-chain, simplifica a auditoria de
privacidade e mantém o app leve — mesmo racional do Princípio II do projeto iOS irmão, adaptado
ao ecossistema Android/Kotlin.

### III. Conformidade com a Política do Google Play Acima de Tudo
O app se enquadra na categoria de política do Google Play para apps que usam `VpnService`
(Device and Network Abuse / User Data policies) — a ficha da loja DEVE incluir divulgação
proeminente do que a VPN local faz com os dados do usuário (não coleta, não retransmite tráfego
para servidor próprio), e o formulário de segurança de dados (Data Safety) DEVE refletir
exatamente o comportamento real do app antes de qualquer envio. Diferente do app irmão de iOS
(restrito ao Safari Content Blocker), o Blindado para Android PODE alegar bloqueio de anúncios e
rastreadores em todo o sistema — para qualquer app instalado, não só um navegador — porque isso é
uma capacidade tecnicamente real da `VpnService` (bloqueio de domínios conhecidos na camada de
DNS, dentro da própria VPN local). Mesmo assim, nenhum texto de interface ou material de
marketing PODE fazer alegação exagerada, enganosa, ou que sugira capacidades que o app não tem
(ex.: inspeção de conteúdo HTTPS, bloqueio de anúncios embutidos em vídeo que não dependem de um
domínio bloqueável).
Rationale: um app rejeitado, suspenso ou removido da Play Store não serve a ninguém; apps de VPN
recebem escrutínio adicional da Google, e divulgação honesta desde o início evita ambos os riscos
de rejeição por política e de erosão de confiança do usuário.

### IV. Honestidade com o Usuário
O estado de proteção exibido na interface DEVE sempre corresponder ao estado real do sistema (a
`VpnService` está de fato conectada e ativa, o filtro de domínios está de fato carregado). O app
NÃO DEVE exibir um estado "ativo" de forma otimista ou em cache quando o estado real é
desconhecido, ainda não confirmado pelo sistema, ou diferente. Qualquer discrepância DEVE ser
resolvida a favor de mostrar o estado real assim que detectável — incluindo o caso em que o
Android revoga a permissão de VPN silenciosamente (outro app de VPN foi ativado, o usuário
revogou a permissão nos Ajustes do sistema, etc.).
Rationale: usuários leigos confiam no indicador visual para decisões de privacidade; um indicador
falso é pior do que nenhuma proteção, e o Android permite que a VPN seja desativada por fora do
app com mais facilidade do que o iOS permite com um perfil de DNS.

### V. Testabilidade
Todo acesso a API de sistema sensível (`VpnService`, resolução DoH, `ConnectivityManager`, etc.)
DEVE ficar atrás de uma interface Kotlin com uma implementação fake/mock injetável. ViewModels
DEVEM ter cobertura de testes unitários (JUnit + coroutines test). As `@Preview` do Compose DEVEM
funcionar sem necessidade de dispositivo físico, permissão de VPN concedida, ou emulador com
Google Play Services.
Rationale: `VpnService` não funciona de forma confiável em emulador (e não funciona nada em
alguns emuladores sem Google Play Services); sem essa barreira arquitetural o desenvolvimento e a
integração contínua ficam reféns de hardware físico, mesmo racional do Princípio V do iOS.

### VI. Acessibilidade Obrigatória
Toda tela DEVE suportar TalkBack com rótulos descritivos (`contentDescription`,
`semantics {}` quando necessário), DEVE respeitar o fator de escala de fonte do sistema (sem
tamanhos de texto fixos em `dp` para conteúdo textual — usar `sp` e a tipografia do Material 3),
e DEVE funcionar corretamente em tema claro e em tema escuro (`dynamicColor`/tema Material 3
próprio do Blindado, não hardcoded).
Rationale: acessibilidade é um requisito de qualidade não negociável e faz parte dos critérios de
qualidade do Google Play; mesmo racional do Princípio VI do iOS, adaptado às ferramentas do
Android.

### VII. Simplicidade
A arquitetura DEVE seguir MVVM enxuto com fluxo de dados unidirecional (Compose `State`/
`StateFlow` subindo do ViewModel, eventos descendo via lambdas). Abstrações especulativas
(camadas, interfaces ou generalizações sem um consumidor atual) NÃO SÃO PERMITIDAS. pt-BR é o
idioma principal da interface; toda string voltada ao usuário DEVE ser preparada para
localização em inglês via `strings.xml` (`values/` e `values-en/`).
Rationale: um app pequeno e focado não precisa de complexidade arquitetural desnecessária; a
preparação para localização desde o início evita retrabalho — mesmo racional do Princípio VII do
iOS.

### VIII. Entrega Independente por História de Usuário
Cada história de usuário priorizada (P1, P2, P3...) DEVE ser entregável, testável e demonstrável
de forma independente em dispositivo físico Android real, sem depender da conclusão de histórias
de prioridade inferior.
Rationale: permite validar o fluxo crítico (P1 — ativar a VPN local e confirmar DNS
criptografado) cedo em hardware real antes de investir nas demais histórias, já que
comportamento de `VpnService` em emulador não é confiável o suficiente para servir de critério de
aceite.

### IX. App Pago de Download Único (Sem Compras Internas) — herdado, sujeito a revisão
Por padrão, o Blindado para Android segue o mesmo modelo do app irmão de iOS: distribuído como
aplicativo pago de download único, com preço definido na Google Play Console, sem compras dentro
do aplicativo, assinaturas, paywall, Google Play Billing ou qualquer mecanismo de bloqueio de
recursos. Todo usuário que baixa o app DEVE ter acesso completo e imediato a todos os níveis de
proteção e ao bloqueio de anúncios/rastreadores em todo o sistema — não existe versão "Pro" nem
recurso premium.
Rationale: consistência com o produto irmão e com o Princípio I (nenhuma infraestrutura própria
de cobrança ou validação de recibos). **Esta suposição foi herdada do projeto iOS e ainda não foi
confirmada explicitamente pelo usuário para o Android** — revisar antes de configurar preço na
Play Console (ver Sync Impact Report).

## Padrões de Qualidade e Segurança

O código Kotlin DEVE compilar sem warnings de lint suprimidos (`@Suppress`) em builds de
Release, exceto com justificativa em comentário. Qualquer permissão Android declarada no
Manifest (`BIND_VPN_SERVICE`, `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, etc.)
DEVE ser justificável em relação a uma história de usuário ativa. Como o app não possui backend
próprio, nenhum segredo, chave de API ou credencial DEVE ser embutido no binário ou no
repositório público. Uma revisão de conformidade com a política do Google Play (ver Princípio
III), incluindo o formulário de Data Safety, É OBRIGATÓRIA antes de qualquer envio.

## Fluxo de Desenvolvimento

Cada mudança DEVE referenciar a história de usuário priorizada que implementa. Testes unitários
DEVEM passar antes do merge. Qualquer mudança que afete o estado de proteção exibido ao usuário
(Princípio IV) DEVE ser validada em dispositivo físico Android antes de considerar a história de
usuário concluída, já que o comportamento real de `VpnService` (conexão, revogação de permissão,
troca de rede) não é confiável em emulador.

## Governance

Esta constituição tem precedência sobre qualquer outra prática ou convenção do projeto. Emendas
exigem: (1) documentação da mudança e sua motivação, (2) atualização deste arquivo com o número
de versão incrementado conforme versionamento semântico, e (3) revisão da data de "Last
Amended". Toda revisão de código e todo plano de implementação DEVEM verificar conformidade com
os princípios aqui descritos; complexidade que viole o Princípio VII (Simplicidade) DEVE ser
justificada explicitamente no plano ou rejeitada.

**Version**: 1.0.0 | **Ratified**: 2026-09-24 | **Last Amended**: 2026-09-24
