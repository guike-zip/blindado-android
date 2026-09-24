# Respostas para o formulário "Segurança dos dados" da Play Console

A Play Console não aceita esse formulário via arquivo — é preenchido manualmente numa UI
com várias perguntas guiadas. Este documento existe só para você preencher rápido, com as
respostas já decididas com base no que o Blindado realmente faz (consistente com
`design/privacidade.html` e com a Constituição, Princípio I).

## Coleta e compartilhamento de dados

**O app coleta ou compartilha algum dos tipos de dados de usuário exigidos?** Não.

O Blindado não tem backend, não usa SDK de analytics/anúncios/crash-reporting de terceiros, não
pede login nem cadastro, e não faz upload de nenhum dado do usuário para um servidor do Blindado
(porque esse servidor não existe).

Marque **"Nenhum dado coletado"** — não é necessário declarar nenhuma categoria (localização,
informações pessoais, mensagens, fotos/vídeos, etc.) porque nenhuma delas se aplica.

## Segurança dos dados em trânsito

**Os dados são transmitidos com criptografia?** Sim (mas veja a ressalva abaixo).

As únicas chamadas de rede do app são:
1. Consultas DNS-over-HTTPS (DoH, RFC 8484) para o provedor escolhido pelo usuário (AdGuard DNS,
   Control D, ou um servidor DoH personalizado) — sempre HTTPS.
2. A checagem da lista fixa de domínios na tela "Testar" — mesma via DoH.

Não há nenhum outro tráfego de rede iniciado pelo app.

## Posso solicitar a exclusão dos meus dados?

**O app permite que os usuários solicitem a exclusão dos dados?** Não aplicável — não há dados
para excluir, porque nada é coletado ou armazenado fora do próprio aparelho do usuário (o
`ProtectionProfile` fica só localmente, via DataStore Preferences).

Se a Play Console exigir marcar uma opção mesmo assim, a resposta correta é "o app não coleta
nenhum dos tipos de dados listados", que normalmente dispensa a pergunta de exclusão.

## Categoria de app / declarações adicionais

- **Contém anúncios?** Não.
- **Compras no app?** Não — pagamento único, sem assinatura nem IAP (Constituição, Princípio IX —
  suposição herdada do app irmão de iOS, **ainda não confirmada explicitamente por você para o
  Android**; revise o preço/modelo antes de publicar).
- **Público-alvo / classificação indicativa:** app utilitário sem conteúdo sensível — mesma lógica
  usada no `age_rating_config.json` do app de iOS (tudo em NONE/false).
- **Declaração de app de VPN:** o Blindado usa a API `VpnService` do Android, o que aciona a
  categoria de política de apps de rede/VPN da Play Store. Ele NÃO é uma VPN de anonimização/
  tráfego completo — é usado apenas para registrar um resolvedor de DNS local (ver
  `design/privacidade.html`, seção "Sobre a permissão de VPN"). Ao preencher a declaração de uso
  de `VpnService` na Play Console, descreva exatamente isso: interceptação local de DNS apenas,
  sem servidor remoto do Blindado, sem inspeção nem retransmissão de tráfego que não seja DNS.

## Link da política de privacidade

https://guike-zip.github.io/blindado-android/design/privacidade.html
