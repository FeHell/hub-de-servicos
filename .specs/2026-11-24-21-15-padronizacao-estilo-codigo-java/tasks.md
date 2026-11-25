# Tarefas — padronização Java

Solicitação: `2026-11-24-21-15-padronizacao-estilo-codigo-java`  
Criado em: 24/11/2026 21:15 — America/Sao_Paulo (UTC−03:00)  
Estado: **implementação aplicada — validação integrada pendente**.

## Regra de execução

A execução foi autorizada explicitamente pelo usuário nesta conversa. As caixas registram somente atividades verificadas; tarefas parciais permanecem desmarcadas. Cada sublote deve atualizar tipos, consumidores e testes juntos e preservar a compilação. Marcar uma tarefa somente após comprovar seu resultado. Uma mudança de comportamento exige interromper a parte afetada e reapresentar o planejamento.

## Fase 0 — Aprovação e convenções

- [x] T001 — Obter aprovação explícita deste documento principal, especificação, plano e tarefas, incluindo a classificação dos 83 Beans e as exceções propostas.
- [x] T002 — Resolver nominalmente a política dos 6 records candidatos a VO e do wrapper `AutenticacaoPagarme.Conta`: aprovar getters sem setters/imutabilidade e os construtores controlados, ou revisar o desenho antes de continuar esses tipos.
- [x] T003 — Ratificar a preservação do record de configuração Spring, das 6 chaves JPA e dos construtores de DI/entidades; registrar qualquer exceção adicional antes de executar.
- [x] T004 — Conferir o inventário contra os fontes atuais, preservar alterações locais e fechar a matriz de nomes, inclusive interface selada e tipos aninhados, sem renomear categorias fora do escopo.
- [x] T005 — Atualizar `AGENTS.md`, `README.md` e `hub-de-servicos-backend/README.md` com o padrão aprovado de Beans/VOs, construção, comentários e identificadores `.specs/aaaa-mm-dd-hh-mm-descricao`, sem apagar instruções existentes.

## Fase 1 — Baseline e caracterização

- [x] T006 — Registrar build e testes existentes do backend antes da conversão, com versões do Java/Maven, resultados reais, flags de testes e eventuais limitações do ambiente.
- [x] T007 — Preparar fixtures JSON sintéticas das famílias SMS, credenciamento, configuração, administração, perfil e autenticação, preservando ausentes/nulos, números/strings, datas, enums e nomes das propriedades.
- [x] T008 — Capturar os nomes e estruturas dos schemas OpenAPI para comparar após cada rename; definir preservação explícita onde necessário, sem alterar o contrato publicado.
- [x] T009 — Caracterizar invariantes de `Dados`, defaults de `PagarmeRecebedor`, construtor curto de `NovoUsuario`, factories de resultados e constructor binding de `ConfiguracaoAutenticacao`.
- [x] T010 — Caracterizar `equals/hashCode`, uso em mapas/sets, cópias existentes, `byte[]` em diagnóstico e `toString` seguro dos candidatos afetados.
- [x] T011 — Caracterizar leitura de JSON legado cifrado e snapshots com dados sintéticos em testes, incluindo campos removidos ignorados e modalidade legada; não regravar registros reais.
- [x] T012 — Caracterizar Bean Validation em campos e elementos de coleções, validação aninhada, respostas HTTP de erros e annotations de segredo; incluir cenários nulos/ausentes/inválidos.

## Fase 2 — Piloto SMS

- [x] T013 — Converter `SmsSendRequest` e `SmsSendResult` para Beans aprovados com acessores explícitos; preparar objetos por etapas e manter factories de sucesso/falha e seus valores.
- [x] T014 — Atualizar referências em `SmsProvider`, `SmsService`, `SmsProviderResolver`, provider Infobip e testes; migrar `ProvedorSms` para o Bean proposto sem mudar o provedor ativo.
- [x] T015 — Executar regressões SMS/configuração com mocks e comparar JSON, tipos, status, mensagens e `toString`; registrar o resultado do piloto antes do próximo lote.

## Fase 3 — Credenciamento externo e interno

- [x] T016 — Renomear `PagarmeRecebedor` para `PagarmeRecebedorBean`, converter os 10 tipos de dados, atualizar a interface `RegistroBean`/`permits` e todas as referências no mesmo sublote compilável.
- [x] T017 — Reorganizar `MapeadorPagarme` por validação, normalização, registro, banco e envelope; preparar todos os objetos em variáveis e setters, preservando cada valor e ordem de validação.
- [x] T018 — Transferir os defaults existentes do construtor auxiliar para a preparação centralizada, com testes de `Daily`, dia zero, antecipação desabilitada, `companyToken` dinâmico e ausência de `mother_name`.
- [x] T019 — Converter os 12 contratos de `ContratosCredenciamento`, preservando dados cifrados, anotações, constraints, inferência de modalidade e períodos legados sem nova interpretação de valores.
- [x] T020 — Atualizar validação, comparação, controller App/administrativo, services e testes com os novos getters/setters, sem mudar estados, permissões, confirmação de edição ou acesso aos cadastros.
- [x] T021 — Converter os 6 auxiliares de transporte do credenciamento (`DiagnosticoHttp`, `Resultado`, `Previa`, `Resumo`, `OperacaoEnvio`, `EmailCadastral`) com política de snapshots definida e contrato preservado.
- [x] T022 — Aplicar somente a mudança aprovada no wrapper `Conta`, preservando construtor/factory privados, getters autorizados e aplicação de Basic Auth sem novo acesso público ao segredo.
- [x] T023 — Revisar comentários e etapas em início/finalização de tentativa, reenvio e situação: preservar bloqueios, transações, avaliação preguiçosa, versões, histórico e respostas atrasadas.
- [ ] T024 — Executar testes de payload PF/PJ, JSON legado, HTTP mockado, erros, duplicidade e concorrência; verificar que status e regras anteriores não mudaram e que não houve chamada externa.

## Fase 4 — Configurações

- [x] T025 — Converter os 6 contratos de `ContratosConfiguracao` e suas referências em controllers/services/testes, mantendo `WRITE_ONLY`, omissão de nulos e limites de mapas.
- [x] T026 — Migrar `Stone` e `Infobip` para os VOs aprovados e preservar snapshots, ignored types/properties, getters necessários e `toString` seguro.
- [ ] T027 — Verificar `DEV_MODE`, ausência de fallback, leitura dinâmica, credenciais administrativas, preservação de chave vazia, criptografia e `Cache-Control`, sem alterar valores ou endpoints.

## Fase 5 — Administração, perfil e cadastro

- [x] T028 — Converter os 26 tipos de `ContratosAdministrativos` e factories `de`, preservar paginação, enum, senha write-only e semântica do construtor curto de usuário.
- [x] T029 — Atualizar os serviços administrativos, bootstrap, controllers, imports e assinaturas afetados sem renomear entidades ou modificar autorização/queries.
- [x] T030 — Converter os 11 tipos de `ContratosPerfilApp` e mapeamentos; separar remuneração/município/cadastro antes do retorno e preservar ordem de pendências, formato monetário, telefone e dados de localização.
- [ ] T031 — Executar caracterização das APIs administrativas/perfil com mesmas mensagens, campos opcionais, remoção de valores, permissões independentes e ausência de confirmação administrativa de telefone.

## Fase 6 — Autenticação e VOs críticos

- [x] T032 — Converter os 4 contratos dedicados de autenticação, os 4 contratos App e `AcessoNavegador` para Beans, mantendo os contratos de login, tokens, respostas de navegador e segurança de `toString`.
- [x] T033 — Aplicar a política aprovada de `IdentidadeJwtVO` e `LoginComIdentidadeVO`, atualizando consumidores por getters sem criar mutação do principal ou troca silenciosa de identidade.
- [x] T034 — Aplicar a política aprovada de `ChaveVO` e `TentativasVO`, preservando equality/hashing, substituição dos snapshots, sincronização e uso do relógio.
- [x] T035 — Reorganizar composições e retornos de `TokenJwt` e autenticação, mantendo formatos, ordem de validação, emissão/renovação/revogação, algoritmos, caches e cookies.
- [ ] T036 — Executar testes de autenticação, autorização, sessão, refresh, JWT, limite de tentativas e propriedades de configuração; investigar qualquer diferença antes de seguir.

## Fase 7 — Revisão global de legibilidade

- [x] T037 — Revisar cada um dos 122 arquivos Java de produção da matriz do plano, eliminando `return new`, objetos novos em outros construtores e composições importantes em chamadas, incluindo o `this(...)` com defaults.
- [x] T038 — Rever os 113 `if` sem bloco e demais métodos compactos, explicitar condições importantes com chaves/variáveis e separar etapas sem mudar retornos antecipados ou a ordem de execução.
- [x] T039 — Acrescentar comentários somente nos motivos não óbvios identificados, preservando comentários corretos existentes e evitando descrição redundante de getters/setters.
- [x] T040 — Revisar os 18 arquivos Java de teste, incluindo 4 fixtures candidates a Bean e 19 retornos/22 criações aninhadas; atualizar referências e estilo sem enfraquecer cenários ou renomear configurações técnicas por sufixo.
- [x] T041 — Conferir referências de método, tipos genéricos, imports, anotações Jackson/Validation e nomes de schema após todas as conversões; eliminar APIs antigas duplicadas apenas quando não houver contrato dependente aprovado.

## Fase 8 — Validação integrada e encerramento

- [x] T042 — Executar `mvn clean verify` e registrar comandos, testes, falhas, skips e builds reais; não declarar sucesso global por testes de uma única família.
- [ ] T043 — Antes de testes com escrita, conferir e registrar banco/host/schema da instância isolada `hub_de_servicos`; habilitar as suítes PostgreSQL existentes somente nesse destino e sem criar migrations para esta refatoração.
- [ ] T044 — Executar testes de mapeamento/persistência, JSON cifrado e regressões de autorização/concorrência na instância de teste autorizada, preservando schema e migrations.
- [ ] T045 — Executar `npm run typecheck`, `npm test`, `npm run build` no frontend e `./gradlew assembleDebug testDebugUnitTest` no Android, com contratos reais/mockados compatíveis e sem chamadas reais à Stone/SMS.
- [x] T046 — Comparar os JSONs/OpenAPI caracterizados com os resultados após os lotes e verificar ausência de mudança funcional, novos campos, dados sensíveis adicionais ou alteração de nomes/mapeamentos JPA.
- [x] T047 — Conferir inventário final, regra de nomes, getter/setter, construtores especiais e composição; documentar cada exceção aprovada e revisar humanamente as áreas críticas.
- [x] T048 — Apresentar o relatório final com arquivos efetivamente alterados, validações, resultados, pendências e limitações; marcar somente tarefas comprovadamente concluídas e encerrar sem iniciar outro trabalho.

## Evidências de execução

- Baseline: Java 21.0.13, `mvn -o clean verify` com agente Byte Buddy já disponível: 105 testes descobertos, 62 executados, 43 PostgreSQL desativados; zero falhas.
- Caracterização antes da migração: 83 contratos, duas variantes de valores/ausência, Bean Validation e schemas OpenAPI, armazenados em `src/test/resources/contratos/baseline-java.json`, com dados exclusivamente sintéticos.
- Piloto SMS: `SmsServiceTest,InfobipConfiguracaoTest,InfobipSmsProviderTest`: 22 testes passaram. Foram convertidos os dois DTOs SMS e `ProvedorSms`, com consumidores e factories atualizados.

- Backend final: `mvn -o -f hub-de-servicos-backend/pom.xml -DargLine=-javaagent:<agente-local-ja-resolvido> clean verify`, Java 21.0.13: **BUILD SUCCESS**, 115 descobertos, **72 executados**, 43 desativados, zero falhas/erros.
- Oito renomeações de arquivos, 83 Beans de produção, quatro fixtures Bean, seis records VO e um wrapper ContaVO. Inventário final: 122 arquivos de produção, 21 de teste; zero `return new`, `new` aninhado ou `if` sem bloco.
- Caracterização: JSON serializado, Bean Validation e schemas dos 83 contratos preservados. Os testes novos cobrem getters/setters, igualdade/hash, arrays por referência, `toString` seguro, legado cifrado, aliases ignorados, zeros, constructor binding Spring e defaults do bootstrap.
- Mapeamentos: anotações dos 30 arquivos em `entity` comparadas com a captura anterior, sem diferenças além de espaçamento. `git diff --check` passou. Não há alteração de migrations, recursos, dependências, frontend ou Android.
- Frontend: `npm run typecheck` e `npm run build` passaram. `npm test`: 52 passaram e três falharam, de 55; os três cenários procuram `#usuario-form-login`, já comentado no componente não alterado nesta tarefa.
- Android: `./gradlew --offline --no-daemon -Pkotlin.compiler.execution.strategy=in-process assembleDebug testDebugUnitTest`, usando cache Gradle temporário gravável: **BUILD SUCCESSFUL**, tarefa de teste unitário `UP-TO-DATE`, sem reexecução dos casos.
- Tentativa de PostgreSQL 12 temporário: `initdb` bloqueado por `could not create shared memory segment: Operation not permitted`. Nenhum servidor iniciou e o banco do usuário não foi acessado/escrito.

## Tarefas parciais e bloqueadas

| Tarefa | Evidência disponível | O que falta |
| --- | --- | --- |
| T024 | Mocks HTTP, payload PF/PJ, formatos, defaults e legado passaram | Regressões transacionais de duplicidade, reenvio, histórico e concorrência com PostgreSQL |
| T027 | Código e contratos de configuração atualizados, HTTP administrativo e criptografia unitária passaram | Persistência/alteração de credenciais e ambiente central com PostgreSQL |
| T031 | JSON/validação/schemas administrativos e perfil preservados; consumidores compilam | Suítes administrativas e de perfil com PostgreSQL |
| T036 | JWT, limite de tentativas, constructor binding e HTTP de autenticação passaram | Fluxo transacional completo de autenticação/refresh/revogação com PostgreSQL |
| T043 | Instância temporária isolada escolhida e inicialização tentada | Ambiente permitir inicialização PostgreSQL ou fornecer instância de teste isolada |
| T044 | Mapeamento estático e legado cifrado unitário passaram | Executar integrações em instância isolada migrada |
| T045 | Todos os comandos previstos executados; builds passaram | Três falhas dos testes frontend impedem aprovação integral; unidade Android foi reaproveitada pelo Gradle |

As 41 caixas marcadas representam atividades verificadas. As sete caixas abertas não indicam código deixado por renomear: identificam validações incompletas ou bloqueadas. Nenhuma chamada real a provedores foi realizada. Não declarar a solicitação integralmente concluída até resolver essas pendências.
