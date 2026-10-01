# CLAUDE.md

> **Aposentado em 2026-10-01.** O app Android (io.blindado.android) agora vive em guike-zip/blindado, pasta `android/`. Este repositório fica só como histórico da linha 1.0.x — não implemente nada aqui.

<!-- ORQUESTRACAO:BEGIN v1 -->
## Orquestração de specs

As specs deste projeto são escritas e acompanhadas por uma **sessão orquestradora**; esta sessão
**implementa**. O protocolo completo está em `cerebro/orquestracao/README.md` (fora deste repositório).

- **Ao começar:** liste as specs com `**Handoff**: pronta-para-implementar` ou `em-andamento`
  (`grep -l '^\*\*Handoff\*\*:' specs/*/spec.md`) e leia a spec **inteira**, principalmente a seção
  "Para a sessão do projeto" (o que fazer, o que NÃO fazer, como verificar).
- **Não implemente** spec em `proposta` ou `bloqueada`: espere a decisão do dono.
- **Código de referência** (arquivo `.patch` na pasta da spec) é ponto de partida já testado, não ordem de
  aplicar às cegas: confira contra o código atual.
- **Se a spec discordar do código**, pare e avise o dono; não mude o escopo sozinho.
- **Ao terminar:** troque o `Handoff` para `feita`, registre o hash do commit na spec e avise o dono.
  Deploy e publicação continuam só com o dono.
<!-- ORQUESTRACAO:END -->
