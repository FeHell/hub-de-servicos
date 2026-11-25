# Plano técnico — diagnóstico, classificação e migração

Solicitação: `2026-11-24-21-15-padronizacao-estilo-codigo-java`  
Criado em: 24/11/2026 21:15 — America/Sao_Paulo (UTC−03:00)  
Estado: **implementação aplicada — validação integrada pendente**.

## 1. Arquitetura e método da análise

A raiz contém três módulos. O Java do Hub está integralmente em `hub-de-servicos-backend`, com Java 21, Spring Boot 3.2.4, Spring Security, Spring Data JPA/Hibernate, Jackson, Bean Validation, Liquibase, SpringDoc e Maven. O backend segue as subcamadas `dto`, `controller`, `service`, `repository` e `entity`, com divisões por funcionalidade sob `br.com.hubdeservicos`. Android é Kotlin nativo; painel é Vue/TypeScript.

Foram lidos o `AGENTS.md`, o README da raiz, os guias de backend/frontend/Android, o `pom.xml`, os contratos de transporte, exemplos de entidades/chaves, serviços de autenticação/configuração/credenciamento, controllers e testes existentes. Foi realizado inventário de todos os arquivos Java e de suas declarações, com busca de expressões compactas. Não houve consulta a bancos ou execução de testes/builds nesta etapa.

As contagens usam arquivos-fonte atuais, inclusive tipos locais/aninhados, e excluem código gerado/dependências. Buscas léxicas removem comentários e literais antes de contar `return new` e criações aninhadas. As linhas citadas são referências do levantamento e podem deslocar-se em uma implementação futura. O inventário sinaliza candidatos; revisão semântica e caracterização são etapas obrigatórias antes de converter cada tipo.

## 2. Convenções propostas e classificações

### Beans

Há 83 candidatos de produção: 75 records sob `dto` e 8 records de transporte em outras camadas. Propor converter para classes com nome terminado em `Bean`, campos privados, getters/setters explícitos e construção por etapas. Getters usam `getX` de forma uniforme; setters não devem executar novas normalizações ou validações ausentes no comportamento anterior. Factory/preparador continua responsável pelos padrões que já existiam.

A renomeação será semântica e localizada: por exemplo, `ContratosCredenciamento.PessoaFisica` → `ContratosCredenciamento.PessoaFisicaBean`, e `PagarmeRecebedor.Individual` → `PagarmeRecebedorBean.IndividualBean`. Não deslocar automaticamente cada tipo aninhado para um arquivo novo. Os cinco agrupadores `Contratos...` ficam com o nome atual. A interface selada `Registro` poderá receber `RegistroBean` por representar o contrato do payload; suas implementações e `permits` acompanham a mudança, sem introduzir mutação na interface.

### VOs e exceções de construção/mutabilidade

Seis records e o wrapper `AutenticacaoPagarme.Conta` precisam de decisão nominal. Propor nomes terminados em `VO` para os valores/contextos classificados na tabela B e manter criação controlada e somente getters. Preferência técnica: classe final imutável quando forem necessários acessores JavaBeans; alternativamente, manter record com getters explícitos como exceção aprovada. Não manter records em toda a camada DTO apenas por conveniência.

Esta proposta de getters sem setters para os VOs é uma exceção explícita ao pedido geral de mutabilidade e depende da aprovação do usuário. Se a exigência for setters para todos, interromper a conversão desses tipos até que seja definido um desenho que não altere a segurança, a igualdade e o uso em mapas. A análise não declara esse conflito resolvido.

Tipos de configuração Spring, chaves JPA e construtores de injeção de serviços têm regras próprias e ficam preservados conforme a tabela C. O acumulador `ValidacaoCredenciamento.Resultado` tem comportamento de avaliação e não é um Bean de transporte; não converter seus métodos `falta`, `invalido` e `confirmar` em setters indiscriminados.

## 3. Comportamentos que precisam ser caracterizados antes da conversão

### Jackson, validação e contratos

- Copiar corretamente as restrições dos componentes para campos/acessores, incluindo constraints de elementos de listas/mapas e `@Valid`; evitar duplicar validação em campo e getter sem necessidade.
- Preservar `@JsonFormat(shape=STRING)` nos valores internos de remuneração/renda/faturamento e números no payload externo. `LocalDate`, `Instant`, UUIDs e booleanos conservam tipos e formato.
- Preservar `@JsonIgnoreProperties({"nomeMae", "mother_name"})`, as anotações de senha `WRITE_ONLY`, a omissão de campos nulos e as classes `Stone` ignoradas por Jackson.
- A propriedade `metadata.companyToken` continua camelCase, enquanto o payload externo usa snake_case em outras estruturas. Não traduzir campos Java só para aproximar um exemplo em português.
- Capturar os schemas OpenAPI atuais antes de renomear classes; preservar identificadores de schema quando possível com o mecanismo já disponível de anotações SpringDoc. Não adicionar biblioteca.
- Não adicionar metadados de classe Java ao JSON cifrado ou a requisições externas. Não presumir que toda renomeação seja neutra sem comparar a serialização real.

### Construtores com comportamento

`ContratosCredenciamento.Dados` infere `CORPORATION` ao ler uma estrutura legada que só contém empresa/representante. Ao convertê-lo, preservar a finalização depois de todos os campos terem sido recebidos; não inferir a modalidade em cada setter, pois a ordem de desserialização pode mudar o resultado. Definir uma factory ou finalização centralizada nos mesmos limites de entrada/leitura e testar todas as permutações relevantes antes de remover o construtor compacto. Um construtor/Jackson creator especial pode ser mantido como exceção se for a única forma de preservar a leitura sem alterar o contrato; isso deve ser decidido no lote, antes da conversão.

`PagarmeRecebedor` aplica antecipação desabilitada, transferência diária habilitada com dia zero e metadata do usuário no construtor de conveniência. Mover a preparação desses objetos para o mapeador/factory existente, com variáveis e setters, preservando exatamente valores, casing e momento de validação. Não deixar um Bean de resposta sair parcialmente inicializado.

`ContratosAdministrativos.NovoUsuario` tem um construtor curto que aplica `ATIVO` e campos opcionais nulos. Preservar essa conveniência por uma factory com nome claro; não transformar status ausente no JSON em `ATIVO` automaticamente, pois isso pode ser diferente do construtor usado atualmente pelo Jackson.

`ConfiguracaoAutenticacao` copia o mapa, trata ausência e valida hashes no construtor compacto. Não trocar isso por um Bean com setters livres. `SmsSendResult` e `ClienteRecebedorPagarme.Resultado` possuem factories de sucesso/falha que devem continuar garantindo combinações de atributos compatíveis.

### Igualdade, hashing, coleções e concorrência

Records têm igualdade/hashing por componentes; uma migração não pode simplesmente cair na igualdade por identidade de `Object`. Definir implementação explícita fiel onde houver uso observável e testes antes de mudar o tipo. Não adicionar `@Data`: além de imprimir campos sensíveis, pode introduzir igualdade de arrays diferente da atual. Em `DiagnosticoHttp`, por exemplo, `byte[] corpo` exige cuidado com identidade versus conteúdo.

Preservar as cópias imutáveis que já existem (`Map.copyOf`, `List.copyOf`) e o comportamento de coleções que não eram copiadas. Não aproveitar esta tarefa para introduzir novas cópias defensivas ou normalizações sem justificar e aprovar a alteração de semântica. Objetos em hash maps, snapshots usados entre transações e dados de tentativas não podem ser alterados silenciosamente após sua publicação.

### Efeitos e transações

Separar expressões dentro da mesma região de execução. Não antecipar criação de UUIDs, geração de senha, chamadas a repositórios ou avaliação de exceções para fora de `orElseGet`/`orElseThrow`. Preservar anotações `@Transactional`, `REQUIRES_NEW`, bloqueios de linha/documento, validação de versão e proteção contra respostas atrasadas. Construtores longos de serviços são DI, e não objetos de transporte: continuar com injeção por construtor.

## 4. Exemplos reais e comentários úteis

| Local atual | Problema/observação | Direção aprovada apenas como proposta |
| --- | --- | --- |
| `MapeadorPagarme.preparar`, linha 40 | Retorno constrói recebedor e banco no mesmo comando; normalizações nos argumentos. | Titular, documento, tipo, dígitos, banco e envelope em variáveis; montagem por setters. |
| `PagarmeRecebedor`, linhas 16–20 | `this(...)` recebe três objetos recém-criados e defaults. | Preparar antecipação, metadata e transferência em passos separados no mapeador/factory. |
| `ContratosPerfilApp.Cadastro.de`, linhas 65–76 | Condições sem bloco, ternário de município e remuneração criada no retorno. | Preservar ordem de pendências; preparar município e remuneração antes do cadastro. |
| `AutenticacaoAppService.cadastrar`, linha 65 | `save(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(...)))`. | Preparar chave, vínculo e persistência em três passos, sem alterar transação nem entidade. |
| `InicializacaoAdministrativa`, linhas 88–90 | Vínculos e chaves aninhados no bootstrap. | Separar valores e objetos no mesmo ponto transacional; não executar bootstrap durante a refatoração. |
| `TokenJwt.carregarChaves`, linhas 177–179 | Chaves e specs são construídos nos argumentos de `KeyPair`. | Specs, chaves geradas e par separados; preservar ordem de leitura e exceções. |
| `ClienteRecebedorPagarme.capturarResposta`, linhas 168–172 | Corpo truncado, cópia de headers e flags são compostos no retorno. | Nomear corpo de diagnóstico, cabeçalhos e flags antes de criar Bean. |
| `RecebedorStoneService.iniciar/finalizar` | Operação, versão, história, documento e resposta podem ficar difíceis de acompanhar. | Etapas e comentários sobre lock contra duplicidade e resposta antiga; sem modificar condições. |
| `SituacaoCredenciamento.operacaoDeReenvio` | Predicados sobre versão, aprovação e modalidade em guard clauses compactas. | Blocos, variáveis booleanas de intenção e comentário do motivo de impedir novo POST. |
| `ConfiguracoesExternasService.lerStone` | Ordem de configuração e proteção de segredo fazem parte do resultado. | Nomear configurações de ambiente; explicar ausência de fallback, preservando exposição administrativa. |
| `ConfiguracaoAutenticacao` / `LimiteTentativasApp` | Imutabilidade e igualdade fazem parte do funcionamento. | Exceções explícitas; não substituir construtores por setters sem análise de invariantes. |

O método PF de `MapeadorPagarme` já contém a variável intermediária `aniversario` com formatação de data e espaçamento manual. Preservar esse trabalho do usuário e reorganizar somente no lote aprovado, sem reverter valores ou o formato `dd/MM/uuuu`.

Comentários devem responder “por quê”: por que não reenviar após timeout indeterminado, por que um documento fica bloqueado, por que um campo legado não é reinterpretado ou por que um segredo não é serializado. Não documentar cada setter com frases repetitivas.

## 5. Sequência técnica, arquivos e dependências

1. Após aprovação, consolidar convenções em `AGENTS.md` e nos READMEs; registrar exceções nominais e congelar a matriz de nomes.
2. Criar testes de caracterização e capturas JSON sintéticas antes de mexer nos tipos. Esses testes e seus arquivos novos serão definidos por família: `ContratosSmsCompatibilidadeTest`, `ContratosCredenciamentoCompatibilidadeTest`, `ContratosAdministrativosCompatibilidadeTest`, `ContratosPerfilAppCompatibilidadeTest`, `ContratosAutenticacaoCompatibilidadeTest`, `ContratosConfiguracaoCompatibilidadeTest` e testes de VOs, nos pacotes de teste correspondentes. São propostas, não arquivos já criados.
3. Piloto: converter `SmsSendRequest`/`SmsSendResult`, factories, `SmsService`, `SmsProvider`, resolver e provider Infobip conforme referências, com seus testes existentes. Revisar o DTO aninhado `ProvedorSms`.
4. Credenciamento externo: converter `PagarmeRecebedor` e tipos aninhados, `MapeadorPagarme` e usos nos clientes/services/testes. Preservar envelope e formatos, incluindo defaults.
5. Credenciamento interno: converter `ContratosCredenciamento`, leitura do legado, comparação, validação e consumidores; depois prévia, diagnóstico e operação de envio. Resolver a política de snapshots mutáveis antes de converter os auxiliares que cruzam transações.
6. Configurações: migrar os 6 contratos e referências; tratar snapshots `Stone`/`Infobip` como VOs se aprovados; preservar senha vazia, `DEV_MODE`, criptografia, endpoints e autorização.
7. Administração/perfil: migrar 26 contratos administrativos e 11 de perfil, factories `de`, mapeamento de páginas, controllers e testes. Nenhum rename de entidade ou coluna.
8. Autenticação por último: 4 DTOs dedicados, 4 contratos App, `AcessoNavegador` e VOs aprovados de identidade, resultado de login e limite de tentativas. Manter mecanismos de JWT, renovação, revogação e cookies.
9. Revisar os demais arquivos por legibilidade, imports e consumidores, com lotes pequenos e compiláveis. Revisar os 4 fixtures de teste sem renomear classes `...Test` ou configurações de teste indiscriminadamente.
10. Conferir JSON/OpenAPI e regressões globais; publicar o relatório do executado e das limitações reais.

Relação de dependência principal: aprovação → caracterização → tipo + consumidores + testes do mesmo lote → validação do lote → lote seguinte. O backend mantém o contrato JSON; frontend e Android não devem precisar de alterações funcionais. Se precisarem, a parte afetada sai da execução até revisão e aprovação do planejamento.

### Arquivos de produção potencialmente renomeados

- `dto/sms/SmsSendRequest.java` → `SmsSendRequestBean.java`.
- `dto/sms/SmsSendResult.java` → `SmsSendResultBean.java`.
- `dto/credenciamento/PagarmeRecebedor.java` → `PagarmeRecebedorBean.java`.
- `dto/autenticacao/LoginEntrada.java` → `LoginEntradaBean.java`.
- `dto/autenticacao/RenovacaoEntrada.java` → `RenovacaoEntradaBean.java`.
- `dto/autenticacao/TokensAutenticacao.java` → `TokensAutenticacaoBean.java`.
- `dto/autenticacao/UsuarioAutenticado.java` → `UsuarioAutenticadoBean.java`.
- `service/autenticacao/IdentidadeJwt.java` → `IdentidadeJwtVO.java`, somente se a exceção e a classificação forem aprovadas.

Os caminhos acima são relativos a `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/`. Não há remoção funcional ou exclusão de dados. A relação de todos os arquivos inspecionados aparece no anexo F; nem todos precisarão ser alterados.

## 6. Validação futura e evidências esperadas

| Área | Caracterização e regressão |
| --- | --- |
| Beans/VOs | Getters/setters, construtor sem argumentos nos Beans; invariantes dos construtores especiais; igualdade/hashing; cópias existentes e `toString`. |
| Jackson/validação | Round trip, campos faltantes/nulos, unknown/ignored legado, números/strings, listas e constraints de elementos, exceções e status das entradas inválidas. |
| API/OpenAPI | Mesmas rotas, nomes/estrutura/tipos, autorização, schemas publicados, headers e mensagens; testes HTTP por controlador. |
| Pagar.me | Mocks do transporte efetivo PF/PJ; nascimento brasileiro; defaults e metadata dinâmico; erros, timeouts, reenvio e histórico sem novos recebedores reais. |
| Configurações/SMS | Segredos preservados e não expandidos, armazenamento cifrado, chave vazia, seleção DEV/PROD sem fallback, envio SMS mockado. |
| Identidade/sessão | Igualdade do principal, autenticação, cookies, refresh/rotação/revogação, autorização e limite de tentativas. |
| Persistência | Mapeamento sem alterações, JSON protegido legível, versões/conflitos, tentativas antigas, permissão e isolamento entre usuários. |
| Consumidores | Build/typecheck/testes frontend e build/testes Android com mocks e os contratos existentes. |

Backend: executar `mvn clean verify`. Se o ambiente impedir o attach do Mockito, utilizar o agente Byte Buddy já resolvido pelo projeto em `argLine`, sem adicionar dependência ou mudar o código para acomodar o ambiente.

Integrações PostgreSQL existentes são cinco classes de teste controladas por quatro flags: `TESTE_PERSISTENCIA_POSTGRES`, `TESTE_AUTENTICACAO_POSTGRES`, `TESTE_ADMINISTRACAO_POSTGRES` e `TESTE_APP_POSTGRES`. Conferir `TESTE_DATABASE_URL`, usuário, banco `hub_de_servicos` e schema da instância isolada antes de habilitar. Não executar migrations nem testes com escrita sobre o ambiente de trabalho do usuário nesta refatoração.

Frontend: `npm run typecheck`, `npm test` e `npm run build`. Android: `./gradlew assembleDebug testDebugUnitTest`. Testes de integração reais opcionais permanecem desativados salvo autorização específica; a criação externa de recebedores e envios SMS reais ficam fora do escopo.

Nenhum build/teste de execução foi rodado para este planejamento. A validação desta etapa é estática: contagens, exemplos, abrangência, links, tarefas e comprovação de que os fontes permaneceram iguais.

## 7. Riscos, limites e reversibilidade

- Não transformar setters em novas regras de negócio. Não mudar assinaturas HTTP para receber status interno, IDs externos ou credenciais adicionais.
- Não alterar `hashCode` de uma chave inserida em mapa nem permitir mutação do principal JWT; as exceções precisam de aprovação.
- Não gerar `toString` com senha, token ou chave por Lombok/IDE. Manter os bloqueios Jackson e a política administrativa atual.
- Não modificar assinaturas de repositórios, projeções, JPQL, `@MapsId` ou tipos de identificador para facilitar um rename de DTO.
- Não introduzir serialização polimórfica com discriminador de classe Java. A interface selada mantém sua função atual e o campo `type` já existente.
- Não confundir nomes de classes Bean de transporte com nomes dos componentes gerenciados pelo Spring.
- Não usar limites de tamanho como justificativa automática para alterar arquitetura ou criar novos serviços. Os limiares 7 parâmetros e 140 caracteres servem à triagem, não são métricas de correção.
- Cada lote deve ser reversível por seu diff/commit, sem migration. Não misturar renames com correções de funcionalidade; não desfazer alterações do usuário que antecedem este planejamento.
- Preservar identificadores já aprovados de dados persistidos. Não emitir chamadas externas para “provar” a equivalência.

## Anexo A — Inventário nominal dos 83 candidatos a Bean

`get/set` abaixo significa proposta de conversão após caracterização e aprovação. As contagens de componentes são as do record atual, não a quantidade futura de parâmetros de construtor. Caminhos relativos à pasta Java de produção do backend.

| Tipo atual | Nome proposto | Arquivo atual e linha | Componentes | Tratamento |
| --- | --- | --- | --- | --- |
| `SessaoNavegadorController.AcessoNavegador` | `SessaoNavegadorController.AcessoNavegadorBean` | `controller/autenticacao/SessaoNavegadorController.java:100` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovaAplicacao` | `ContratosAdministrativos.NovaAplicacaoBean` | `dto/administracao/ContratosAdministrativos.java:41` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AtualizarAplicacao` | `ContratosAdministrativos.AtualizarAplicacaoBean` | `dto/administracao/ContratosAdministrativos.java:46` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovoUsuario` | `ContratosAdministrativos.NovoUsuarioBean` | `dto/administracao/ContratosAdministrativos.java:50` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AtualizarUsuario` | `ContratosAdministrativos.AtualizarUsuarioBean` | `dto/administracao/ContratosAdministrativos.java:65` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovaSenha` | `ContratosAdministrativos.NovaSenhaBean` | `dto/administracao/ContratosAdministrativos.java:72` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovaPermissao` | `ContratosAdministrativos.NovaPermissaoBean` | `dto/administracao/ContratosAdministrativos.java:80` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovaPermissaoPlataforma` | `ContratosAdministrativos.NovaPermissaoPlataformaBean` | `dto/administracao/ContratosAdministrativos.java:85` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.PermissaoPlataformaInfo` | `ContratosAdministrativos.PermissaoPlataformaInfoBean` | `dto/administracao/ContratosAdministrativos.java:90` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AtualizarPermissao` | `ContratosAdministrativos.AtualizarPermissaoBean` | `dto/administracao/ContratosAdministrativos.java:97` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.NovoPerfil` | `ContratosAdministrativos.NovoPerfilBean` | `dto/administracao/ContratosAdministrativos.java:100` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AtualizarPerfil` | `ContratosAdministrativos.AtualizarPerfilBean` | `dto/administracao/ContratosAdministrativos.java:103` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AplicacaoInfo` | `ContratosAdministrativos.AplicacaoInfoBean` | `dto/administracao/ContratosAdministrativos.java:107` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.UsuarioInfo` | `ContratosAdministrativos.UsuarioInfoBean` | `dto/administracao/ContratosAdministrativos.java:115` | 9 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.PermissaoUsuario` | `ContratosAdministrativos.PermissaoUsuarioBean` | `dto/administracao/ContratosAdministrativos.java:125` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.UsuarioListagem` | `ContratosAdministrativos.UsuarioListagemBean` | `dto/administracao/ContratosAdministrativos.java:128` | 11 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.TotalUsuariosStatus` | `ContratosAdministrativos.TotalUsuariosStatusBean` | `dto/administracao/ContratosAdministrativos.java:137` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.TotalUsuariosMes` | `ContratosAdministrativos.TotalUsuariosMesBean` | `dto/administracao/ContratosAdministrativos.java:140` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.TotalUsuariosTipo` | `ContratosAdministrativos.TotalUsuariosTipoBean` | `dto/administracao/ContratosAdministrativos.java:143` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.TotalUsuariosPermissao` | `ContratosAdministrativos.TotalUsuariosPermissaoBean` | `dto/administracao/ContratosAdministrativos.java:146` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.DashboardUsuarios` | `ContratosAdministrativos.DashboardUsuariosBean` | `dto/administracao/ContratosAdministrativos.java:149` | 6 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.PermissaoInfo` | `ContratosAdministrativos.PermissaoInfoBean` | `dto/administracao/ContratosAdministrativos.java:154` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.PerfilInfo` | `ContratosAdministrativos.PerfilInfoBean` | `dto/administracao/ContratosAdministrativos.java:160` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.VinculoInfo` | `ContratosAdministrativos.VinculoInfoBean` | `dto/administracao/ContratosAdministrativos.java:166` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.SessaoInfo` | `ContratosAdministrativos.SessaoInfoBean` | `dto/administracao/ContratosAdministrativos.java:172` | 8 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.AuditoriaInfo` | `ContratosAdministrativos.AuditoriaInfoBean` | `dto/administracao/ContratosAdministrativos.java:182` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosAdministrativos.Pagina` | `ContratosAdministrativos.PaginaBean` | `dto/administracao/ContratosAdministrativos.java:190` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosApp.CadastroEntrada` | `ContratosApp.CadastroEntradaBean` | `dto/app/ContratosApp.java:18` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosApp.LoginAppEntrada` | `ContratosApp.LoginAppEntradaBean` | `dto/app/ContratosApp.java:26` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosApp.ContaApp` | `ContratosApp.ContaAppBean` | `dto/app/ContratosApp.java:35` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosApp.AcessoApp` | `ContratosApp.AcessoAppBean` | `dto/app/ContratosApp.java:41` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.AtualizarCadastro` | `ContratosPerfilApp.AtualizarCadastroBean` | `dto/app/ContratosPerfilApp.java:26` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.Remuneracao` | `ContratosPerfilApp.RemuneracaoBean` | `dto/app/ContratosPerfilApp.java:30` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.SelecionarPapel` | `ContratosPerfilApp.SelecionarPapelBean` | `dto/app/ContratosPerfilApp.java:34` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.IdentificarMunicipio` | `ContratosPerfilApp.IdentificarMunicipioBean` | `dto/app/ContratosPerfilApp.java:37` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.SelecionarMunicipio` | `ContratosPerfilApp.SelecionarMunicipioBean` | `dto/app/ContratosPerfilApp.java:41` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.OpcaoMunicipio` | `ContratosPerfilApp.OpcaoMunicipioBean` | `dto/app/ContratosPerfilApp.java:44` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.Municipio` | `ContratosPerfilApp.MunicipioBean` | `dto/app/ContratosPerfilApp.java:50` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.IniciarConfirmacao` | `ContratosPerfilApp.IniciarConfirmacaoBean` | `dto/app/ContratosPerfilApp.java:53` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.ConfirmarTelefone` | `ContratosPerfilApp.ConfirmarTelefoneBean` | `dto/app/ContratosPerfilApp.java:56` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.Cadastro` | `ContratosPerfilApp.CadastroBean` | `dto/app/ContratosPerfilApp.java:63` | 9 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosPerfilApp.Desafio` | `ContratosPerfilApp.DesafioBean` | `dto/app/ContratosPerfilApp.java:81` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `LoginEntrada` | `LoginEntradaBean` | `dto/autenticacao/LoginEntrada.java:7` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `RenovacaoEntrada` | `RenovacaoEntradaBean` | `dto/autenticacao/RenovacaoEntrada.java:7` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `TokensAutenticacao` | `TokensAutenticacaoBean` | `dto/autenticacao/TokensAutenticacao.java:5` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `UsuarioAutenticado` | `UsuarioAutenticadoBean` | `dto/autenticacao/UsuarioAutenticado.java:5` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.ConfiguracaoInfo` | `ContratosConfiguracao.ConfiguracaoInfoBean` | `dto/configuracao/ContratosConfiguracao.java:21` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.ModoDesenvolvimento` | `ContratosConfiguracao.ModoDesenvolvimentoBean` | `dto/configuracao/ContratosConfiguracao.java:30` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.AlterarProvedorSms` | `ContratosConfiguracao.AlterarProvedorSmsBean` | `dto/configuracao/ContratosConfiguracao.java:33` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.ServicoInfo` | `ContratosConfiguracao.ServicoInfoBean` | `dto/configuracao/ContratosConfiguracao.java:36` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.ConfiguracaoExternaInfo` | `ContratosConfiguracao.ConfiguracaoExternaInfoBean` | `dto/configuracao/ContratosConfiguracao.java:39` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosConfiguracao.AtualizarServico` | `ContratosConfiguracao.AtualizarServicoBean` | `dto/configuracao/ContratosConfiguracao.java:48` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Dados` | `ContratosCredenciamento.DadosBean` | `dto/credenciamento/ContratosCredenciamento.java:24` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.PessoaFisica` | `ContratosCredenciamento.PessoaFisicaBean` | `dto/credenciamento/ContratosCredenciamento.java:36` | 10 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Empresa` | `ContratosCredenciamento.EmpresaBean` | `dto/credenciamento/ContratosCredenciamento.java:45` | 12 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Representante` | `ContratosCredenciamento.RepresentanteBean` | `dto/credenciamento/ContratosCredenciamento.java:56` | 12 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Endereco` | `ContratosCredenciamento.EnderecoBean` | `dto/credenciamento/ContratosCredenciamento.java:66` | 8 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.ContaBancaria` | `ContratosCredenciamento.ContaBancariaBean` | `dto/credenciamento/ContratosCredenciamento.java:73` | 9 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Pendencia` | `ContratosCredenciamento.PendenciaBean` | `dto/credenciamento/ContratosCredenciamento.java:80` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Completude` | `ContratosCredenciamento.CompletudeBean` | `dto/credenciamento/ContratosCredenciamento.java:82` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Credenciamento` | `ContratosCredenciamento.CredenciamentoBean` | `dto/credenciamento/ContratosCredenciamento.java:85` | 7 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.EnviarRecebedor` | `ContratosCredenciamento.EnviarRecebedorBean` | `dto/credenciamento/ContratosCredenciamento.java:93` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.ReenviarRecebedor` | `ContratosCredenciamento.ReenviarRecebedorBean` | `dto/credenciamento/ContratosCredenciamento.java:94` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ContratosCredenciamento.Recebedor` | `ContratosCredenciamento.RecebedorBean` | `dto/credenciamento/ContratosCredenciamento.java:95` | 13 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor` | `PagarmeRecebedorBean` | `dto/credenciamento/PagarmeRecebedor.java:13` | 6 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Antecipacao` | `PagarmeRecebedorBean.AntecipacaoBean` | `dto/credenciamento/PagarmeRecebedor.java:22` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Metadata` | `PagarmeRecebedorBean.MetadataBean` | `dto/credenciamento/PagarmeRecebedor.java:24` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Transferencia` | `PagarmeRecebedorBean.TransferenciaBean` | `dto/credenciamento/PagarmeRecebedor.java:27` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Individual` | `PagarmeRecebedorBean.IndividualBean` | `dto/credenciamento/PagarmeRecebedor.java:37` | 10 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Corporation` | `PagarmeRecebedorBean.CorporationBean` | `dto/credenciamento/PagarmeRecebedor.java:46` | 12 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Representante` | `PagarmeRecebedorBean.RepresentanteBean` | `dto/credenciamento/PagarmeRecebedor.java:55` | 10 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Endereco` | `PagarmeRecebedorBean.EnderecoBean` | `dto/credenciamento/PagarmeRecebedor.java:63` | 8 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Telefone` | `PagarmeRecebedorBean.TelefoneBean` | `dto/credenciamento/PagarmeRecebedor.java:68` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `PagarmeRecebedor.Banco` | `PagarmeRecebedorBean.BancoBean` | `dto/credenciamento/PagarmeRecebedor.java:74` | 9 | Construtor vazio, get/set; preservar contrato e semântica. |
| `SmsSendRequest` | `SmsSendRequestBean` | `dto/sms/SmsSendRequest.java:3` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |
| `SmsSendResult` | `SmsSendResultBean` | `dto/sms/SmsSendResult.java:3` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ClienteRecebedorPagarme.DiagnosticoHttp` | `ClienteRecebedorPagarme.DiagnosticoHttpBean` | `service/credenciamento/ClienteRecebedorPagarme.java:309` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ClienteRecebedorPagarme.Resultado` | `ClienteRecebedorPagarme.ResultadoBean` | `service/credenciamento/ClienteRecebedorPagarme.java:314` | 5 | Construtor vazio, get/set; preservar contrato e semântica. |
| `CredenciamentoService.Previa` | `CredenciamentoService.PreviaBean` | `service/credenciamento/CredenciamentoService.java:106` | 3 | Construtor vazio, get/set; preservar contrato e semântica. |
| `DiagnosticoRecebedorService.Resumo` | `DiagnosticoRecebedorService.ResumoBean` | `service/credenciamento/DiagnosticoRecebedorService.java:139` | 14 | Construtor vazio, get/set; preservar contrato e semântica. |
| `RecebedorStoneService.OperacaoEnvio` | `RecebedorStoneService.OperacaoEnvioBean` | `service/credenciamento/RecebedorStoneService.java:280` | 4 | Construtor vazio, get/set; preservar contrato e semântica. |
| `ValidacaoCredenciamento.EmailCadastral` | `ValidacaoCredenciamento.EmailCadastralBean` | `service/credenciamento/ValidacaoCredenciamento.java:135` | 1 | Construtor vazio, get/set; preservar contrato e semântica. |
| `SmsProviderResolver.ProvedorSms` | `SmsProviderResolver.ProvedorSmsBean` | `service/sms/SmsProviderResolver.java:35` | 2 | Construtor vazio, get/set; preservar contrato e semântica. |

## Anexo B — VOs e wrapper que exigem aprovação específica

| Tipo atual | Proposta de nome | Motivo da exceção e decisão necessária |
| --- | --- | --- |
| `IdentidadeJwt` | `IdentidadeJwtVO` | Principal/contexto de segurança; manter estado imutável e igualdade por componentes. Getters explícitos; não dar setters que permitam trocar identidade/sessão. |
| `LimiteTentativasApp.Chave` | `LimiteTentativasApp.ChaveVO` | Chave do `HashMap`; setters após inserção alterariam o hashing e o limite de tentativas. Construtor com os dois componentes. |
| `LimiteTentativasApp.Tentativas` | `LimiteTentativasApp.TentativasVO` | Snapshot de expiração/contador substituído dentro de operação sincronizada; preservar substituição, relógio e valor. |
| `AutenticacaoService.LoginComIdentidade` | `AutenticacaoService.LoginComIdentidadeVO` | Par coerente de tokens/usuário usado no resultado da autenticação; preservar associação e `toString` seguro. |
| `ConfiguracoesExternasService.Stone` | `ConfiguracoesExternasService.StoneVO` | Snapshot do ambiente/credenciais; preservar `JsonIgnoreType`, `JsonIgnore`, conteúdo e seleção sem mutação posterior. |
| `ConfiguracoesExternasService.Infobip` | `ConfiguracoesExternasService.InfobipVO` | Snapshot da configuração SMS; preservar chave ignorada no JSON e ausência de vazamento por `toString`. |
| `AutenticacaoPagarme.Conta` | `AutenticacaoPagarme.ContaVO` | Wrapper com comportamento de aplicar Basic Auth; manter factory de seleção/validação e construtor privado. Expor somente getters equivalentes a `ambiente/accountId`, sem criar acesso público novo à SecretKey. |

Os seis primeiros são records. `Conta` já é classe final com construtor privado. A classificação como VO do wrapper é proposta de contexto de valor; caso seja considerado uma abstração técnica de autenticação, manter `Conta` como exceção nominal sem sufixo. Essa escolha precisa ser decidida antes da execução, sem renomear o componente `AutenticacaoPagarme`.

## Anexo C — Tipos que não serão convertidos indiscriminadamente

| Categoria | Quantidade/nomes | Política |
| --- | --- | --- |
| Configuração imutável | `ConfiguracaoAutenticacao` (1 record) | Preservar constructor binding, cópia/validação de mapa e prefixo Spring. Sem rename `Bean`/`VO` automático. |
| Agrupadores sem dados | `ContratosAdministrativos`, `ContratosApp`, `ContratosPerfilApp`, `ContratosConfiguracao`, `ContratosCredenciamento` (5 classes) | Manter classe de namespace e construtor privado; renomear só os tipos de dados internos. |
| JPA | 19 entidades e 6 chaves `@Embeddable` | Manter nomes, construtores JPA, igualdade, atributos de identidade e mapeamentos. Alterar composição nos consumidores sem mudar o domínio persistido. |
| Chaves embutidas | `AplicacaoPermissaoId`, `PerfilPermissaoId`, `UsuarioAplicacaoId`, `UsuarioPerfilId`, `UsuarioPermissaoPlataformaId`, `RecebedorExternoEntity.Chave` | Não adicionar setters de identidade nem aplicar sufixo só porque são objetos de valor; requerer nova aprovação para alteração própria da categoria. |
| Enums | 22 de produção | Preservar nomes Java e valores serializados/persistidos, inclusive enums aninhados. |
| Anotações de validação | `EmailUsuario`, `SenhaNova` | Manter anotações, targets e constraints; não confundir com Beans. |
| Serviços/DI | Componentes, factories de infraestrutura e serviços | Getters/setters não substituem injeção por construtor. Métodos só recebem ajustes de legibilidade e referências. |
| Acumulador de validação | `ValidacaoCredenciamento.Resultado` | Comportamento de reunir faltantes/inválidos/confirmações; manter encapsulamento e métodos de intenção. |
| Testes técnicos | Classes `...Test`, configurações e transporte controlado | Não adicionar sufixos Bean/VO a configurações de teste nem utilitários. |

Quatro records de fixtures também serão analisados: `AdministracaoPostgresTest.ContaCredenciamento`, `PerfilAppPostgresTest.Conta`, `PerfilAppPostgresTest.Envio` e `AutenticacaoPostgresTest.PermissaoTeste`. Proposta inicial: nomes `...Bean` por transporte de dados de teste, get/set e montagem explícita; preservar as funções auxiliares de `Envio` e não incluir tokens no `toString`. São adicionais aos 83 tipos de produção.

## Anexo D — Construtores grandes e exceções

Limiar de triagem: pelo menos 7 parâmetros/componentes. Os construtores por DI e JPA não entram automaticamente na política de construção de DTOs.

### Records com muitos componentes

| Tipo | Componentes | Arquivo e linha |
| --- | --- | --- |
| `ContratosAdministrativos.NovoUsuario` | 7 | `dto/administracao/ContratosAdministrativos.java:50` |
| `ContratosAdministrativos.AtualizarUsuario` | 7 | `dto/administracao/ContratosAdministrativos.java:65` |
| `ContratosAdministrativos.AplicacaoInfo` | 7 | `dto/administracao/ContratosAdministrativos.java:107` |
| `ContratosAdministrativos.UsuarioInfo` | 9 | `dto/administracao/ContratosAdministrativos.java:115` |
| `ContratosAdministrativos.UsuarioListagem` | 11 | `dto/administracao/ContratosAdministrativos.java:128` |
| `ContratosAdministrativos.SessaoInfo` | 8 | `dto/administracao/ContratosAdministrativos.java:172` |
| `ContratosAdministrativos.AuditoriaInfo` | 7 | `dto/administracao/ContratosAdministrativos.java:182` |
| `ContratosPerfilApp.Cadastro` | 9 | `dto/app/ContratosPerfilApp.java:63` |
| `ContratosConfiguracao.ConfiguracaoInfo` | 7 | `dto/configuracao/ContratosConfiguracao.java:21` |
| `ContratosCredenciamento.PessoaFisica` | 10 | `dto/credenciamento/ContratosCredenciamento.java:36` |
| `ContratosCredenciamento.Empresa` | 12 | `dto/credenciamento/ContratosCredenciamento.java:45` |
| `ContratosCredenciamento.Representante` | 12 | `dto/credenciamento/ContratosCredenciamento.java:56` |
| `ContratosCredenciamento.Endereco` | 8 | `dto/credenciamento/ContratosCredenciamento.java:66` |
| `ContratosCredenciamento.ContaBancaria` | 9 | `dto/credenciamento/ContratosCredenciamento.java:73` |
| `ContratosCredenciamento.Credenciamento` | 7 | `dto/credenciamento/ContratosCredenciamento.java:85` |
| `ContratosCredenciamento.Recebedor` | 13 | `dto/credenciamento/ContratosCredenciamento.java:95` |
| `PagarmeRecebedor.Individual` | 10 | `dto/credenciamento/PagarmeRecebedor.java:37` |
| `PagarmeRecebedor.Corporation` | 12 | `dto/credenciamento/PagarmeRecebedor.java:46` |
| `PagarmeRecebedor.Representante` | 10 | `dto/credenciamento/PagarmeRecebedor.java:55` |
| `PagarmeRecebedor.Endereco` | 8 | `dto/credenciamento/PagarmeRecebedor.java:63` |
| `PagarmeRecebedor.Banco` | 9 | `dto/credenciamento/PagarmeRecebedor.java:74` |
| `DiagnosticoRecebedorService.Resumo` | 14 | `service/credenciamento/DiagnosticoRecebedorService.java:139` |

### Construtores explícitos

| Tipo | Parâmetros | Política | Arquivo e linha |
| --- | --- | --- | --- |
| `InicializacaoAdministrativa` | 9 | Preservar injeção por construtor; não criar setters de dependência. | `bootstrap/administracao/InicializacaoAdministrativa.java:49` |
| `AuditoriaEntity` | 7 | Preservar construtor e invariantes da entidade. | `entity/auditoria/AuditoriaEntity.java:70` |
| `SessaoEntity` | 7 | Preservar construtor e invariantes da entidade. | `entity/sessao/SessaoEntity.java:70` |
| `AdministracaoAplicacaoService` | 13 | Preservar injeção por construtor; não criar setters de dependência. | `service/administracao/AdministracaoAplicacaoService.java:63` |
| `AdministracaoPlataformaService` | 16 | Preservar injeção por construtor; não criar setters de dependência. | `service/administracao/AdministracaoPlataformaService.java:88` |
| `AutenticacaoAppService` | 7 | Preservar injeção por construtor; não criar setters de dependência. | `service/app/AutenticacaoAppService.java:40` |
| `ConfirmacaoTelefoneService` | 8 | Preservar injeção por construtor; não criar setters de dependência. | `service/app/ConfirmacaoTelefoneService.java:44` |
| `CredenciamentoService` | 15 | Preservar injeção por construtor; não criar setters de dependência. | `service/credenciamento/CredenciamentoService.java:60` |
| `RecebedorStoneService` | 13 | Preservar injeção por construtor; não criar setters de dependência. | `service/credenciamento/RecebedorStoneService.java:55` |

## Anexo E — Ocorrências de composição e triagem de legibilidade

A tabela lista todos os arquivos de produção com algum indicador observado. `R` são linhas de `return new`; `N` são linhas de `new` aninhado; `I` é quantidade de `if` sem bloco; `L` é quantidade de linhas acima de 140 caracteres. Esses indicadores pedem revisão, não autorizam mudanças de condição nem implicam que todo encadeamento seja inadequado. O `this(...)` de Pagarme e chamadas como `body(new ...)` exigem inspeção adicional, já prevista.

| Arquivo de produção | Linhas R | Linhas N | I | L |
| --- | --- | --- | --- | --- |
| `bootstrap/administracao/InicializacaoAdministrativa.java` | — | 88, 89 | 0 | 0 |
| `config/autenticacao/ConfiguracaoAutenticacao.java` | 41 | — | 0 | 0 |
| `config/autenticacao/SegurancaWeb.java` | 90 | — | 0 | 0 |
| `controller/administracao/ErrosAdministracaoController.java` | — | — | 0 | 1 |
| `controller/autenticacao/SessaoNavegadorController.java` | — | — | 2 | 0 |
| `dto/administracao/ContratosAdministrativos.java` | 92, 110, 118, 132, 156, 162, 168, 177, 185, 192 | — | 0 | 3 |
| `dto/app/ContratosApp.java` | 37, 43 | — | 0 | 0 |
| `dto/app/ContratosPerfilApp.java` | 46, 75 | 75 | 3 | 2 |
| `dto/configuracao/ContratosConfiguracao.java` | 24 | — | 0 | 0 |
| `dto/sms/SmsSendResult.java` | 27, 31 | — | 0 | 0 |
| `dto/usuario/SituacaoTelefone.java` | — | — | 1 | 0 |
| `entity/credenciamento/TentativaRecebedorEntity.java` | — | — | 2 | 0 |
| `provider/infobip/InfobipSmsProvider.java` | — | — | 5 | 0 |
| `repository/configuracao/ConfiguracaoSistemaRepository.java` | — | — | 0 | 1 |
| `service/administracao/AdministracaoAplicacaoService.java` | 265 | — | 0 | 0 |
| `service/administracao/AdministracaoConfiguracaoService.java` | 81, 94, 114 | — | 0 | 0 |
| `service/administracao/AdministracaoPlataformaService.java` | 152, 222, 394 | — | 3 | 0 |
| `service/administracao/PermissoesPlataformaService.java` | — | — | 1 | 0 |
| `service/app/AutenticacaoAppService.java` | 68 | 65 | 0 | 0 |
| `service/app/ConfirmacaoTelefoneService.java` | 73, 153 | — | 5 | 0 |
| `service/app/LimiteTentativasApp.java` | 63 | — | 0 | 0 |
| `service/app/PerfilAppService.java` | — | — | 3 | 0 |
| `service/autenticacao/AutenticacaoService.java` | 100, 156, 218 | — | 3 | 0 |
| `service/autenticacao/TokenJwt.java` | 133, 144, 177 | 177 | 0 | 0 |
| `service/configuracao/ConfiguracoesExternasService.java` | 108, 129, 131, 147 | — | 1 | 0 |
| `service/configuracao/CriptografiaCredenciais.java` | 53 | — | 4 | 0 |
| `service/credenciamento/AutenticacaoPagarme.java` | 34 | — | 0 | 0 |
| `service/credenciamento/ClienteRecebedorPagarme.java` | 168, 171, 298, 319, 321 | 171 | 11 | 0 |
| `service/credenciamento/ComparacaoCredenciamento.java` | — | — | 3 | 1 |
| `service/credenciamento/CredenciamentoService.java` | 103, 218, 248, 263, 267 | — | 12 | 1 |
| `service/credenciamento/DiagnosticoRecebedorService.java` | 79, 136 | — | 4 | 0 |
| `service/credenciamento/DocumentosCredenciamento.java` | — | — | 2 | 0 |
| `service/credenciamento/MapeadorPagarme.java` | 40, 52, 63, 71 | 40 | 0 | 0 |
| `service/credenciamento/RecebedorStoneService.java` | 166, 178, 183, 204, 254 | — | 14 | 1 |
| `service/credenciamento/SituacaoCredenciamento.java` | 70, 94 | — | 13 | 0 |
| `service/credenciamento/ValidacaoCredenciamento.java` | 246 | — | 14 | 1 |
| `service/usuario/TelefoneUsuarioService.java` | 56, 60 | — | 7 | 0 |

## Anexo F — Matriz completa dos arquivos inspecionados

Os 140 arquivos estão relacionados abaixo. “Referências/legibilidade” significa revisar imports, assinaturas e estilo se afetados pelos tipos migrados; não alterar nomes ou responsabilidades dessa categoria. Caminhos relativos a `hub-de-servicos-backend/src/`.

| Arquivo | Disposição proposta |
| --- | --- |
| `main/java/br/com/hubdeservicos/HubDeServicosApplication.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/bootstrap/administracao/BootstrapAdmin.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/bootstrap/administracao/InicializacaoAdministrativa.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/common/StatusCadastro.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/config/autenticacao/ConfiguracaoAutenticacao.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/config/autenticacao/FiltroJwt.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/config/autenticacao/SegurancaWeb.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/administracao/AdministracaoAplicacaoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/administracao/AdministracaoConfiguracaoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/administracao/AdministracaoCredenciamentoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/administracao/AdministracaoPlataformaController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/administracao/ErrosAdministracaoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/app/AutenticacaoAppController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/app/CredenciamentoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/app/ErrosAutenticacaoAppController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/app/PerfilAppController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/autenticacao/AutenticacaoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/autenticacao/ChavesPublicasController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/autenticacao/ErrosAutenticacaoController.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/controller/autenticacao/SessaoNavegadorController.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/dto/administracao/ContratosAdministrativos.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/app/ContratosApp.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/app/ContratosPerfilApp.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/autenticacao/LoginEntrada.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/autenticacao/RenovacaoEntrada.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/autenticacao/TokensAutenticacao.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/autenticacao/UsuarioAutenticado.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/configuracao/ContratosConfiguracao.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/credenciamento/ContratosCredenciamento.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/credenciamento/PagarmeRecebedor.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/sms/SmsSendRequest.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/sms/SmsSendResult.java` | Migrar tipos de transporte e factories conforme anexo A. |
| `main/java/br/com/hubdeservicos/dto/usuario/EmailUsuario.java` | Preservar enum/anotação; não aplicar sufixo Bean/VO. |
| `main/java/br/com/hubdeservicos/dto/usuario/SenhaNova.java` | Preservar enum/anotação; não aplicar sufixo Bean/VO. |
| `main/java/br/com/hubdeservicos/dto/usuario/SituacaoTelefone.java` | Preservar enum/anotação; não aplicar sufixo Bean/VO. |
| `main/java/br/com/hubdeservicos/entity/aplicacao/AplicacaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/aplicacao/AplicacaoPermissaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/aplicacao/AplicacaoPermissaoId.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/aplicacao/TipoAplicacao.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/auditoria/AuditoriaEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/configuracao/ConfiguracaoSistemaEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/configuracao/ServicoExterno.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/credenciamento/CredenciamentoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/credenciamento/DiagnosticoRecebedorEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/credenciamento/ProvedorCredenciamento.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/credenciamento/RecebedorExternoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/credenciamento/TentativaRecebedorEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/perfil/PerfilEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/perfil/PerfilPermissaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/perfil/PerfilPermissaoId.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/permissao/PermissaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/sessao/SessaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/sessao/StatusSessao.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/sessao/TokenAtualizacaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/DesafioTelefoneEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/MunicipioEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/PapelUsuario.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/StatusUsuario.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioAplicacaoEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioAplicacaoId.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioPerfilEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioPerfilId.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioPermissaoPlataformaEntity.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/entity/usuario/UsuarioPermissaoPlataformaId.java` | Preservar identidade/JPA; somente legibilidade ou referências necessárias. |
| `main/java/br/com/hubdeservicos/exception/autenticacao/AutenticacaoNegadaException.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/provider/SmsProvider.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/provider/infobip/InfobipSmsProvider.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/repository/administracao/ConsultaUsuariosRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/aplicacao/AplicacaoPermissaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/aplicacao/AplicacaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/auditoria/AuditoriaRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/configuracao/ConfiguracaoSistemaRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/credenciamento/CredenciamentoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/credenciamento/DiagnosticoRecebedorRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/credenciamento/RecebedorExternoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/credenciamento/TentativaRecebedorRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/perfil/PerfilPermissaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/perfil/PerfilRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/permissao/PermissaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/sessao/SessaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/sessao/TokenAtualizacaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/DesafioTelefoneRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/MunicipioRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/UsuarioAplicacaoRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/UsuarioPerfilRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/UsuarioPermissaoPlataformaRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/repository/usuario/UsuarioRepository.java` | Preservar contrato/query; atualizar referências somente se necessárias. |
| `main/java/br/com/hubdeservicos/service/administracao/AcessoAdministrativo.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/AdministracaoAplicacaoService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/AdministracaoConfiguracaoService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/AdministracaoPlataformaService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/PermissoesPlataformaService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/RegistroAdministrativo.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/administracao/SenhasAdministrativas.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/app/AutenticacaoAppService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/app/ConfirmacaoTelefoneService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/app/LimiteTentativasApp.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/app/PerfilAppService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/autenticacao/AutenticacaoService.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/autenticacao/IdentidadeJwt.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/autenticacao/SegredosAutenticacao.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/autenticacao/TokenJwt.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/autorizacao/AutorizacaoService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/configuracao/AmbienteServicosService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/configuracao/ConfiguracoesExternasService.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/configuracao/CriptografiaCredenciais.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/credenciamento/AutenticacaoPagarme.java` | Wrapper candidato a VO sob aprovação; preservar autenticação e seleção de credenciais. |
| `main/java/br/com/hubdeservicos/service/credenciamento/ClienteRecebedorPagarme.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/credenciamento/ComparacaoCredenciamento.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/credenciamento/CredenciamentoService.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/credenciamento/DiagnosticoRecebedorService.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/credenciamento/DocumentosCredenciamento.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/credenciamento/MapeadorPagarme.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/credenciamento/RecebedorStoneService.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/credenciamento/SituacaoCredenciamento.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/credenciamento/ValidacaoCredenciamento.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/sessao/RevogacaoSessaoService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/sms/SmsProviderResolver.java` | Classificar auxiliares de dados/VO/configuração conforme anexos A–C; referências e legibilidade. |
| `main/java/br/com/hubdeservicos/service/sms/SmsService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/usuario/CadastroUsuarioService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `main/java/br/com/hubdeservicos/service/usuario/TelefoneUsuarioService.java` | Referências/legibilidade; preservar nome, papel e funcionamento. |
| `test/java/br/com/hubdeservicos/config/autenticacao/AutenticacaoHttpTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/controller/administracao/AdministracaoPostgresTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/controller/app/AutenticacaoAppPostgresTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/controller/app/PerfilAppPostgresTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/controller/autenticacao/AutenticacaoPostgresTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/persistencia/ConexaoPostgresTeste.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/persistencia/MapeamentoPersistenciaTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/persistencia/PersistenciaPostgresTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/provider/infobip/InfobipConfiguracaoTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/provider/infobip/InfobipSmsProviderTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/provider/infobip/InfobipTransporteControlado.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/administracao/AdministracaoConfiguracaoHttpTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/app/LimiteTentativasAppTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/autenticacao/TokenJwtTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/configuracao/CriptografiaCredenciaisTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/credenciamento/ClienteRecebedorPagarmeTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/credenciamento/PagarmeRecebedorTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |
| `test/java/br/com/hubdeservicos/service/sms/SmsServiceTest.java` | Revisar testes/fixtures e referências; preservar cenários e assertivas. |

## 8. Registro da entrega inicial de planejamento

Entregues quatro documentos da mesma solicitação, com os inventários integrados neste plano. Nenhum tipo foi renomeado e nenhum getter, setter, construtor ou fluxo de aplicação foi alterado. A execução só começa depois da aprovação explícita da versão apresentada, incluindo as exceções de VOs, configuração e JPA. Qualquer mudança funcional ou de contrato descoberta posteriormente exige nova análise e autorização.


## 9. Resultado da execução autorizada

A sequência foi executada com pilotos SMS e payload, migração dos demais contratos e revisão global. Os 83 candidatos nominais do anexo A foram convertidos em Beans. As exceções do anexo B foram aprovadas: seis records VO e `ContaVO`, sem setters. O record Spring, identidade JPA e DI por construtor foram preservados.

A caracterização por família foi consolidada em `ContratosCompatibilidadeTest` e um fixture JSON anterior à conversão; `ValoresImutaveisTest` e `BootstrapAdminTest` cobrem as exceções e invariantes específicas. São três classes novas de teste. O helper que capturou a fixture não foi incorporado como ferramenta de geração do projeto.

`DadosBean` mantém um `@JsonCreator` para finalizar a modalidade legada somente após o recebimento dos blocos. Constraints de elementos genéricos nos getters mantêm validação e OpenAPI equivalentes aos records; os nomes publicados usam `@Schema`. Os defaults foram separados no mapper. Não houve nova dependência, migration ou mudança nos consumidores Kotlin/Vue.

Backend final: `clean verify` passou com 72 testes executados, zero falhas/erros e 43 integrações PostgreSQL desativadas. A instância PostgreSQL temporária não iniciou por restrição de memória compartilhada. Frontend: typecheck/build passaram; três testes falharam ao procurar um login já comentado no componente original. Android: build passou e testes unitários estavam `UP-TO-DATE`.

A implementação está aplicada; o encerramento integral depende dos testes PostgreSQL e da validação frontend pendente. Consulte o documento principal para comandos, arquivos e resultados, e `tasks.md` para as caixas ainda abertas. A matriz original foi mantida como histórico do inventário anterior.
