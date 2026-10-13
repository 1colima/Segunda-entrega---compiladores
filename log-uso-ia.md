# Log de uso de IA — Checkpoint 2

- Data: 09/10/2026.
- Ferramenta: ChatGPT / Codex.
- Base: projeto do Checkpoint 1 enviado na conversa.

## Assistência recebida

Foi solicitada a geração da gramática completa e, depois, das partes restantes
do CP2. A IA propôs a BNF, calculou FIRST/FOLLOW, verificou LL(1) e gerou
o parser recursivo descendente e os testes. Também explicou fatoração,
recursão à esquerda e recuperação de erros.

A sintaxe de funções com tipo de retorno e procedimentos sem tipo explícito
segue a opção A escolhida na conversa. As demais produções completas foram
propostas pela IA e ainda precisam ser revisadas pelo grupo.

Scanner.java e Token.java foram preservados sem alterações. O parser não
constrói AST. Os sete testes mínimos foram compilados e executados pelo
assistente; isso não substitui a revisão nem a execução pelo grupo.

## Revisão e versão final

O usuário informou que vai revisar o código e refazer a versão a ser entregue.
Essa revisão ainda não foi concluída nesta conversa. Atualizar este log
com as alterações efetivamente feitas pelo grupo, mantendo o registro da
assistência recebida.

O uso descrito ultrapassa as categorias permitidas no enunciado. O registro
é uma descrição verdadeira da assistência e não torna esse uso autorizado.

## Adaptação às aulas do professor

A IA adaptou Parser.java ao formato da Aula 6: métodos por não-terminal, sequências diretas nas produções únicas, if/else para FIRST e FOLLOW, caudas recursivas e sincronização em modo pânico. Scanner.java e Token.java foram preservados. Os sete testes foram executados após a adaptação. O grupo ainda deve revisar e refazer a versão destinada à entrega.
