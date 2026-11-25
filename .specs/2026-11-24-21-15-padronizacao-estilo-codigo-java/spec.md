# Especificação — estilo Java, Beans e VOs

Solicitação: `2026-11-24-21-15-padronizacao-estilo-codigo-java`  
Criado em: 24/11/2026 21:15 — America/Sao_Paulo (UTC−03:00)  
Estado: **implementação aplicada — validação integrada pendente**.

## Objetivo e motivação

Melhorar a leitura humana, a inspeção por breakpoint e a compreensão das responsabilidades dos objetos em todo o código Java do Hub. A execução autorizada deve preservar o mesmo comportamento funcional com nomes de transporte explícitos e montagem organizada.

## Escopo

### Incluído na execução aprovada

- Java de produção e testes do backend, com revisão de todas as camadas existentes.
- Nomes de tipos que sejam efetivamente Beans ou VOs, inclusive tipos aninhados e referências.
- Campos privados, getters/setters explícitos em Beans, preparação por etapas e revisão dos construtores longos.
- Atualização de imports, parâmetros, retornos, métodos de acesso, factories, referências de método e testes afetados.
- Separação de objetos novos em variáveis antes de retornos, composição e chamadas em que sua preparação esteja escondida.
- Blocos e espaçamento claros; comentários de intenção nos pontos de regra não óbvia.
- Verificação de compatibilidade JSON, Bean Validation, OpenAPI, dados criptografados, JPA e consumidores.
- Documentação permanente das convenções em AGENTS/READMEs, preservando as alterações locais existentes.

### Excluído

- A entrega inicial era restrita ao planejamento; a autorização posterior liberou somente a execução nele descrita.
- Mudanças de regras, parâmetros de negócio, validação, status, autorização ou fluxo de autenticação.
- Alterações de schema, banco, migrations, credenciais, endpoints ou integrações financeiras.
- Renomear serviços, controllers, repositórios, entidades, enums, anotações ou pacotes por mera aplicação de sufixos.
- Criar novos subsistemas de DTOs, camadas paralelas, geração de código ou dependências.
- Alterar o estilo Kotlin/TypeScript/Vue ou consultar projetos externos ao Hub.
- Executar chamadas reais à Stone, Infobip ou outros provedores.

## Requisitos verificáveis

### RF-01 — Identificação dos tipos

Classificar cada candidato nominal do inventário por responsabilidade. Aplicar `Bean` a contratos de transporte e `VO` a valores de domínio/contexto aprovados. Classes `Contratos...` são agrupadoras de tipos, e não Beans de dados. Interfaces de anotação e enums não são DTOs por estarem na pasta `dto`.

### RF-02 — Acesso e preparação de Beans

Usar campos privados, construtor sem parâmetros, getters/setters e variáveis locais para a montagem. Usar `getX`/`setX` como convenção uniforme, inclusive para indicadores, preservando seus nomes JSON explicitamente quando necessário. Não criar getters de informações derivadas que acrescentem propriedades ao contrato acidentalmente. Preservar `boolean` versus `Boolean`, `null`, limites e anotações dos tipos.

### RF-03 — Composição legível

Nenhum `new` deve ser devolvido diretamente por `return`. Objetos novos devem ser montados separadamente antes de serem passados a outro construtor. Separar também composições importantes em `save(...)`, `body(...)`, factories, ternários e lambdas. Manter a avaliação preguiçosa, a ordem de execução e as saídas antecipadas; a regra não autoriza reordenar efeitos ou eliminar tratamentos de erro.

### RF-04 — Exceções justificadas

Construtores obrigatórios de frameworks, identidade/segurança e valores imutáveis ficam sujeitos às exceções nominais descritas no plano. Setters em VOs não serão introduzidos antes da aprovação de uma solução que preserve invariantes, hashing e concorrência. Serviços continuam com injeção por construtor. Nenhum VO ou exceção será escolhido só pelo tamanho da classe.

### RF-05 — Comportamento preservado

Nenhum campo JSON pode mudar de nome, hierarquia, tipo, formatação, inclusão/omissão ou regra de acesso por causa de um rename Java. Preservar validações, mensagens, códigos HTTP, padrões, datas, arredondamentos, nomes de enum, ordenação observável e autorização. Preservar os nomes dos schemas OpenAPI; se isso não for possível sem mudança pública, interromper a parte afetada e pedir nova aprovação.

### RF-06 — Compatibilidade com dados antigos

Continuar lendo as estruturas JSON já cifradas em `credenciamento_empresarial.dados_protegidos`, snapshots de tentativas e diagnósticos, sem migration ou regravação em massa. Manter a modalidade legada, os valores cujo período não está definido, os campos ignorados e os defaults existentes. Nenhuma limpeza de dados é parte desta tarefa.

### RF-07 — Segurança

Preservar `@JsonIgnore`, `@JsonIgnoreType`, `WRITE_ONLY`, respostas `no-store`, criptografia e `toString` que não revele credenciais. Manter a exposição administrativa de credenciais exatamente conforme o contrato existente. Não acrescentar segredos a getters expostos em APIs comuns, erros, logs ou exemplos. A existência de logs autorizados em outro trabalho não permite expandi-los nesta refatoração.

### RF-08 — Testes e sequência

Cada lote precisa compilar com todos os consumidores atualizados e passar pelos testes previstos para sua responsabilidade. Antes de converter records, caracterizar seu JSON, igualdade, invariantes e validação. Não desabilitar testes nem enfraquecer assertions para aceitar uma diferença criada pela migração.

## Critérios de aceitação da implementação

| ID | Evidência exigida |
| --- | --- |
| CA-01 | Cada candidato possui classificação e nome aprovados, inclusive tipos aninhados. |
| CA-02 | Beans usam getters/setters e montagem por etapas; acessores compactos antigos não permanecem como API duplicada sem exceção documentada. |
| CA-03 | Ausência de `return new` e de composição `new` aninhada no escopo final, com inspeção semântica de lambdas e delegações. |
| CA-04 | Construtores especiais remanescentes estão na lista de exceções aprovada, com justificativa e teste. |
| CA-05 | Testes de contrato demonstram JSON e mensagens/validações equivalentes para entradas válidas, ausentes, nulas e inválidas. |
| CA-06 | Formatos Pagar.me, dados legados, `DEV_MODE` e configuração de serviços mantêm o comportamento atual. |
| CA-07 | Autenticação, autorização, hashing, concorrência, histórico e prevenção de duplicidade passam por regressão. |
| CA-08 | Nomes/mapeamentos JPA, schema, SQL de estrutura e migrations permanecem intactos. |
| CA-09 | Frontend e Android continuam compatíveis; qualquer divergência exige revisão do planejamento. |
| CA-10 | Build, testes e revisão humana registrados por lote; pendências não são declaradas concluídas. |

## Critérios desta entrega de planejamento

Entregar o documento principal com o identificador da pasta, `spec.md`, `plan.md` e `tasks.md`; inventário global com contagens e exemplos; classificações propostas e exceções visíveis; tarefas sem marcações de conclusão; nenhum código de aplicação alterado. Aguardar autorização explícita da versão apresentada.


## Situação após a execução autorizada

Foram aplicados os 83 Beans de produção, quatro fixtures Bean, sete VOs/exceções, acessores e montagem por etapas. JSON, validações e schemas dos 83 contratos foram comparados com a captura anterior. Os testes complementares cobrem legado cifrado, inferência da modalidade, defaults, arrays, igualdade, configuração Spring e bootstrap.

O backend passou em `clean verify`: 72 testes executados sem falhas, 43 PostgreSQL desativados. Frontend passou em typecheck/build, com três falhas de testes em código não alterado que espera um campo de login comentado. Android compilou; a tarefa unitária usou resultado `UP-TO-DATE`. O PostgreSQL temporário foi bloqueado pelo ambiente em `initdb`.

CA-07 e CA-09 continuam parcialmente verificados: faltam regressões transacionais com PostgreSQL e uma suíte frontend integralmente aprovada. CA-10 exige manter essas pendências visíveis. Não há novas migrations, alteração de dados ou chamada real a provedores. Os detalhes e arquivos estão no documento principal; as tarefas parciais permanecem desmarcadas em `tasks.md`.
