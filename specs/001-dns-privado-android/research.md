# Fase 0 — Pesquisa técnica: Blindado Android

## #1 — minSdk / targetSdk

**Decision**: `minSdk = 26` (Android 8.0 Oreo), `targetSdk`/`compileSdk` na API estável mais
recente disponível no ambiente de build no momento da implementação (não fixar um número aqui
para não ficar desatualizado — a task de setup do Gradle deve usar o valor mais recente
disponível no SDK Manager instalado).

**Rationale**: `VpnService` existe desde a API 14, muito abaixo de qualquer piso razoável hoje.
O requisito real que empurra o piso para cima é `startForeground()` com canal de notificação
(`NotificationChannel`), obrigatório desde a API 26 — abaixo disso a notificação persistente do
serviço de proteção (FR-015) não pode ser criada da forma moderna. 26 cobre a esmagadora maioria
dos aparelhos Android em uso hoje sem excluir uma fatia de usuários desproporcional ao ganho,
diferente do app irmão de iOS (que mira só a versão mais recente do iOS por causa de uma API de
UI específica — Liquid Glass — que não existe em versões antigas). Aqui não existe uma API de UI
equivalente forçando um piso tão alto; manter o piso mais baixo e razoável maximiza o alcance do
app sem custo técnico real.

**Alternatives considered**: minSdk 21 (Lollipop) — rejeitado por não ter canal de notificação
moderno, exigindo um caminho de código legado só para isso, sem ganho relevante de alcance de
usuários em 2026. minSdk 34 (mesma filosofia "só o mais moderno" do iOS) — rejeitado porque não
há nenhuma API exclusiva de versões recentes que a v1 do app realmente precise, ao contrário do
caso do iOS.

## #2 — Tipo de foreground service (Android 14+)

**Decision**: usar `android:foregroundServiceType="specialUse"` no `AndroidManifest.xml`, com o
metadata `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` descrevendo o uso ("local VPN DNS
filtering service") — **a ser confirmado contra a documentação oficial do Android e testado em
dispositivo físico durante a implementação**, não assumido como definitivo aqui.

**Rationale**: a lista de tipos de foreground service do Android (`connectedDevice`, `dataSync`,
`location`, `mediaPlayback`, `specialUse`, `systemExempted`, etc., conforme a versão da
plataforma) não inclui um tipo dedicado literalmente chamado "vpn" — `VpnService` historicamente
recebe tratamento especial do subsistema de VPN do próprio Android e nem sempre passou pelas
mesmas regras de `foregroundServiceType` que serviços comuns, mas isso muda entre versões de
API. Em vez de arriscar uma alegação não verificada sobre o comportamento exato (mesmo erro que
já causou um bug real no app irmão de iOS — um "conflito" inventado em vez do erro real da API),
esta decisão fica marcada explicitamente como pendente de confirmação em código real rodando em
um Android atual antes de ser tratada como certa. A task de implementação da `BlindadoVpnService`
DEVE verificar o comportamento real (não só ler documentação) em pelo menos um dispositivo físico
com a versão de Android mais recente disponível, e atualizar este documento com o resultado.

**Alternatives considered**: nenhum tipo declarado (deixar o Android inferir) — arriscado em
versões recentes que podem rejeitar o `startForeground()` sem tipo declarado; `systemExempted` —
geralmente reservado a apps de sistema/pré-instalados, provavelmente não aplicável a um app de
terceiros na Play Store.

## #3 — Como capturar só tráfego DNS sem inspecionar o resto (bateria/latência)

**Decision**: a `VpnService.Builder` NÃO deve adicionar uma rota geral (`0.0.0.0/0`). Em vez
disso, o app se registra como o **próprio resolvedor DNS do sistema** via `Builder.addAddress()`
(endereço da interface TUN local, ex.: `10.0.0.2/32`) + `Builder.addDnsServer()` apontando para
esse mesmo endereço local, e adiciona rota (`addRoute()`) só para esse endereço — não para a
internet em geral. Esse é o padrão já usado por apps reais de filtragem de DNS local no Android
(ex.: DNS66, Intra, PersonalDNSFilter, código aberto e auditável), e significa que o sistema
operacional só entrega à interface TUN do Blindado os pacotes efetivamente destinados à
resolução DNS — todo o resto do tráfego dos outros apps nunca entra na VPN, sem precisar de
nenhum código de "passar direto" escrito pelo Blindado.

**Rationale**: evita reimplementar um roteador de pacotes genérico (fora de escopo, risco de bug
grave de rede, e Princípio VII de simplicidade) e resolve a preocupação de bateria/latência do
plano — o app só processa o volume de tráfego que já pretende processar (consultas DNS),
nunca payloads de app nenhum.

**Alternatives considered**: capturar `0.0.0.0/0` e reimplementar NAT/forwarding para todo o
tráfego (abordagem de VPNs de tráfego completo, ex. NetGuard) — rejeitada por complexidade e
risco desnecessários para um produto que é explicitamente "não é uma VPN de anonimização"
(spec, User Story 4 / Assumptions).

## #4 — Formato da requisição DoH

**Decision**: DoH via **POST** (RFC 8484), `Content-Type: application/dns-message`, corpo
binário = a consulta DNS em wire format (sem base64url), usando `HttpsURLConnection` nativo.

**Rationale**: o formato GET do RFC 8484 exige codificar a consulta em base64url na query
string — mais código sem necessidade real. POST com corpo binário é suportado por todos os
provedores DoH relevantes (incluindo os dois usados no app irmão de iOS) e é mais simples de
implementar com `HttpsURLConnection` puro (sem biblioteca de terceiros — Princípio II):
`setDoOutput(true)`, escrever os bytes do pacote DNS direto no `OutputStream`, ler os bytes da
resposta do `InputStream`.

**Alternatives considered**: GET com base64url — rejeitado por complexidade adicional sem
benefício real para este caso de uso (não há necessidade de cache HTTP por URL).

## #5 — Lista de bloqueio (domínios de anúncios/rastreadores/conteúdo adulto)

**Decision**: lista estática embutida no APK (`app/src/main/res/raw/blocklist.txt`, formato
simples de um domínio por linha), carregada em memória (`Set<String>` ou trie de sufixos de
domínio para matching eficiente, ex. `ads.example.com` casa contra uma entrada `example.com` se
o app decidir bloquear por domínio-pai — a task de implementação decide a granularidade exata)
na inicialização da `BlindadoVpnService`. Sem download remoto de lista dinâmica nesta v1
(consistente com a spec, que não pede atualização automática).

**Rationale**: mantém a promessa de zero infraestrutura própria e zero chamada de rede além do
DoH e do teste de proteção (Princípio I) — uma lista baixada de um servidor remoto próprio do
Blindado violaria isso; uma lista de terceiros teria que ser auditada com o mesmo cuidado do
Princípio II antes de ser considerada.

**Alternatives considered**: baixar lista de um provedor de blocklist de terceiros em runtime —
rejeitado nesta v1 por (a) violar a superfície mínima de rede do Princípio I sem uma decisão de
produto explícita para isso, e (b) adicionar uma dependência de disponibilidade externa fora do
controle do Blindado. Fica como possível v2, não decidida aqui.

## #6 — Provedores DoH suportados (Padrão/Família)

**Decision**: os mesmos dois provedores usados no app irmão de iOS — AdGuard DNS e Control D —
cada um com endpoints diferentes para o nível Padrão (bloqueio de anúncios/rastreadores) e
Família (também bloqueia conteúdo adulto).

**CONFIRMADO (2026-09-24)** contra documentação oficial via busca web, não mais uma suposição:

- AdGuard DNS Padrão: `https://dns.adguard-dns.com/dns-query`
- AdGuard DNS Família: `https://family.adguard-dns.com/dns-query`
- Control D Padrão: `https://freedns.controld.com/p2` ("Block Malware + Ads")
- Control D Família: `https://freedns.controld.com/family` ("Block Malware + Ads + Social +
  Adult Content + Drugs")

Estes são exatamente os valores já implementados em `ProviderCatalog.kt` — a implementação não
precisou de nenhuma correção, mas a pendência sinalizada aqui (research.md original) e em
`ProviderCatalog.kt`/`tasks.md` T029 está resolvida e não deve mais ser tratada como suposição
não verificada.

**Rationale**: consistência de produto com o app irmão; ambos os provedores já são conhecidos
por suportar DoH padrão RFC 8484 compatível com a decisão #4.

**Fontes**: [AdGuard DNS Knowledge Base — Known DNS Providers](https://adguard-dns.io/kb/general/dns-providers/),
[Control D — Free DNS Resolvers](https://docs.controld.com/docs/free-dns).

## #7 — ACHADO CRÍTICO (2026-09-24): resolução DNS falha 100% no emulador — bloqueia T042/T046

**O que foi observado**: ao preparar screenshots para a Play Store no emulador (AVD `movase_test`,
Android 14), a tela "Testar" reportou **todos** os 4 domínios de teste como "Bloqueado" —
incluindo `example.com`, que não está na blocklist e deveria aparecer como "Acessível". Investigação
com logging temporário (revertido, nunca commitado) confirmou:

- Nenhum pacote IPv4/UDP:53 chega ao `input.read()` da `BlindadoVpnService` — só ruído IPv6
  link-local (Neighbor Discovery, `fe80::...`) não relacionado a DNS.
- `InetAddress.getByName()` falha instantaneamente (< 100ms) para os 4 domínios, tanto a partir do
  próprio processo do Blindado quanto de um processo totalmente diferente (`adb shell`, UID 2000) —
  descarta a hipótese de auto-exclusão do app dono da VPN.
- `ping 10.0.0.2` (o próprio endereço que `Builder.addAddress()` atribui à interface TUN) responde
  com RTT ~0.06–0.2ms — rápido demais para um round-trip real via o código Kotlin do app (que nem
  implementa ICMP). Isso sugere que o próprio kernel/emulador responde localmente a tráfego
  destinado ao endereço da interface, sem nunca entregá-lo ao file descriptor da TUN para o app
  processar — ou seja, o mecanismo `addAddress("10.0.0.2") + addDnsServer("10.0.0.2")` (o mesmo
  endereço para os dois) pode não estar sendo roteado para o processo userspace da forma esperada
  **neste emulador especificamente**.

**Por que a hipótese mais provável é "limitação do emulador", não bug real do app**: esse padrão
(mesmo endereço para a interface e para o DNS) é exatamente o documentado na decisão #3 acima e é o
usado por apps de código aberto reais e amplamente usados em produção (DNS66, Intra,
PersonalDNSFilter) — se o padrão não funcionasse de forma alguma em Android real, esses apps não
funcionariam. A Constituição (Princípio VI) já registra que `VpnService` "não funciona de forma
confiável em emulador" — este achado é a primeira evidência concreta e específica desse limite,
não apenas a advertência genérica que já existia.

**Por que isso NÃO deve ser tratado como resolvido sem dispositivo físico**: a hipótese acima é
plausível, mas não foi provada — continua sendo possível (embora menos provável) que haja um bug
real no roteamento configurado por `BlindadoVpnService.connect()`. T042 e T046 já exigiam
dispositivo físico por outros motivos; este achado eleva a prioridade e o escopo de T042
especificamente: a primeira coisa a verificar em dispositivo físico real é se a tela "Testar"
diferencia corretamente domínios bloqueados de acessíveis (não só se a VPN "conecta" visualmente).
Enquanto isso não for confirmado em hardware real, a funcionalidade central do app (bloqueio de
domínios) deve ser tratada como **não verificada**, apesar da UI/fluxo de permissão funcionarem.

**Ação tomada nesta sessão**: nenhuma mudança de código foi feita (o logging de diagnóstico foi
revertido) — não há evidência suficiente para justificar uma mudança na arquitetura de
`Builder.addAddress()`/`addDnsServer()` sem antes confirmar em hardware real se o problema
realmente existe fora do emulador. Os screenshots da Play Store da tela "Testar" foram
propositalmente **não capturados** nesta sessão para evitar publicar um resultado que pode estar
incorreto.
