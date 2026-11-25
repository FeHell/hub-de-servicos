# Revisão e padronização do estilo de código Java

Solicitação: `2026-11-24-21-15-padronizacao-estilo-codigo-java`  
Criado em: 24/11/2026 21:15 — America/Sao_Paulo (UTC−03:00)  
Estado: **implementação aplicada — validação integrada pendente**.

## 1. Resultado da execução e documentos

Este é o ponto de entrada para a revisão e o acompanhamento da execução autorizada. A padronização foi aplicada ao Java de produção e testes. O backend compila e passou pelos 72 testes habilitados; os 43 testes PostgreSQL e três falhas nos testes do frontend impedem declarar a validação integrada concluída. A seção 8 registra os resultados e arquivos efetivamente alterados. As seções de diagnóstico e estratégia preservam a referência do planejamento inicial.

- [Especificação e critérios de aceitação](spec.md).
- [Plano técnico, inventário completo e riscos](plan.md).
- [Tarefas executadas e validações pendentes](tasks.md).

O fluxo de análise → documentos → aprovação → execução → validação segue o `AGENTS.md` da raiz. A execução foi expressamente autorizada pelo usuário. `AGENTS.md` e READMEs foram atualizados, preservando o conteúdo local preexistente e registrando as convenções e exceções aprovadas. As tarefas parciais continuam desmarcadas.

## 2. Diagnóstico inicial do projeto inteiro

Foram inventariados todos os 140 arquivos Java de código-fonte encontrados no Hub, incluindo testes e tipos aninhados, excluindo `target`, `build`, dependências e arquivos gerados. Todo o Java encontrado pertence ao backend. O Android usa Kotlin e o painel usa Vue/TypeScript; serão consumidores para testes de compatibilidade, sem conversão de estilo dessas linguagens.

| Item | Resultado observado |
| --- | --- |
| Arquivos Java de produção / testes | 122 / 18 |
| Declarações de produção | 87 classes, 90 records, 22 enums e 30 interfaces, incluindo 2 anotações |
| Declarações de teste | 22 classes e 4 records |
| Records de DTO | 75, distribuídos em 12 arquivos |
| Candidatos a Beans de transporte | 83 records de produção: 75 em DTO e 8 fora de DTO |
| VOs candidatos com exceção de mutabilidade a aprovar | 6 records e 1 wrapper de autenticação já implementado como classe |
| Outros tipos que exigem preservação específica | 1 record de configuração Spring; 6 chaves JPA embutidas |
| Entidades JPA | 19; nomes e mapeamentos preservados |
| `return new` | 68 ocorrências em 26 arquivos de produção; 19 em 11 arquivos de teste |
| `new` dentro dos argumentos de outro `new` | 7 ocorrências em 6 arquivos de produção; 22 em 8 arquivos de teste |
| Records com 7 ou mais componentes | 22; o maior tem 14 componentes |
| Construtores explícitos com 7 ou mais parâmetros | 9: 7 de injeção de dependências e 2 de entidades |
| Triagem de legibilidade | 113 condições `if` sem bloco e 11 linhas acima de 140 caracteres em produção |

Os números de padrões de expressão são um levantamento léxico com comentários e literais removidos, seguido de inspeção dos exemplos críticos; não substituem análise semântica. Os conjuntos se sobrepõem e não devem ser somados como um total de classes a alterar. O plano contém a classificação nominal e a matriz dos 140 arquivos. Há ainda uma delegação `this(...)` em `PagarmeRecebedor` com três objetos novos, que não entra nas 7 ocorrências de `new` aninhado.

Lombok já está declarado como dependência opcional no `pom.xml`, mas nenhum dos 140 arquivos utiliza imports Lombok. A proposta usa getters/setters explícitos, sem novas dependências e sem introduzir `@Data`.

## 3. Padrão proposto

### 3.1. Organização dos métodos

Separar visualmente validação, normalização, seleção de modalidade, montagem dos componentes e resultado. Criar variáveis intermediárias com nomes que expressem o dado obtido. Extrair métodos privados pequenos somente quando houver uma responsabilidade identificável; não criar um método para cada linha.

Nenhum objeto novo deve aparecer diretamente em `return` ou dentro de outro construtor. Os mesmos valores serão preparados antes, em variáveis locais. Aplicar a regra também em factories, lambdas e testes. Construtores de exceções usados em `throw` não serão proibidos indiscriminadamente. Objetos em métodos de terceiros, como `save(new ...)` e `body(new ...)`, também serão separados quando esconderem valores ou efeitos importantes.

Preservar a avaliação preguiçosa de `orElseGet` e `orElseThrow`: as variáveis deverão ser preparadas dentro da lambda, nunca antecipando sua execução. Preservar a ordem das validações, os limites transacionais, a ordem dos setters quando relevante e o comportamento em caso de exceção.

### 3.2. Nomes e acessores

- **Bean**: requisições, respostas e estruturas transportadas entre camadas. Proposta: classe com campos privados, construtor sem argumentos e métodos `getX`/`setX`; nomes JSON, tipos, anotações e comportamento de igualdade preservados.
- **VO**: valores/coerência de contexto sem identidade própria, chaves e snapshots internos com uso comprovado de igualdade, imutabilidade ou segurança. Proposta de exceção: getters explícitos, estado imutável e construtores/factories controlados. Esta exceção precisa ser aprovada; não é uma dispensa implícita do requisito de setters.
- **Tipos técnicos**: entidades `Entity`, repositórios, serviços, controllers, configurações Spring, enums, anotações, chaves JPA e classes agrupadoras `Contratos...` não recebem sufixos indiscriminadamente.

A interface selada `PagarmeRecebedor.Registro` é um contrato de transporte: propor `PagarmeRecebedorBean.RegistroBean`, atualizando `permits` e implementações; não criar atributos ou setters em uma interface que não contém estado.

Propor manter os tipos aninhados nos mesmos arquivos. Apenas os arquivos de tipos de topo que realmente mudarem de nome serão renomeados. Não há criação de um novo pacote paralelo de DTOs.

### 3.3. Exemplo real e direção da mudança

Em `MapeadorPagarme.preparar`, o backend atualmente retorna `new PagarmeRecebedor(..., new PagarmeRecebedor.Banco(...), usuario)`. O método também normaliza documento, titular e dígitos nos argumentos dos construtores. O construtor auxiliar de `PagarmeRecebedor` cria antecipação, metadata e transferência em uma única delegação.

Direção proposta, ilustrativa e ainda não implementada:

```java
// O banco já passou pelas validações de titularidade e formato do credenciamento.
PagarmeRecebedorBean.BancoBean banco = prepararBanco(conta);

// A factory preserva os padrões atuais e obtém companyToken do usuário Hub informado pelo serviço.
PagarmeRecebedorBean recebedor = prepararRecebedor(
    codigo,
    registro,
    banco,
    usuario
);

return recebedor;
```

Na implementação desses preparadores, cada Bean será criado em variável local e receberá os valores por setters. Serão preservados os nomes internos do contrato, como `code`, `registerInformation`, `holderName` e `companyToken`; a padronização não traduzirá nem renomeará propriedades da API.

### 3.4. Comentários

Explicar a razão de regras que não são evidentes: isolamento de DEV/PROD, bloqueio de documento contra duplicidade, resposta atrasada de tentativas antigas, manutenção de segredos fora de respostas comuns e compatibilidade dos dados criptografados antigos. Não comentar operações óbvias como “cria objeto”, “define nome” ou “retorna resultado”. Não acrescentar dados reais nem ampliar logs como parte desta padronização.

## 4. Migração segura e incremental

| Lote | Conteúdo e fechamento obrigatório |
| --- | --- |
| 0 — Convenções e baseline | Aprovar classificação/exceções; documentar padrão; caracterizar JSON, validações, igualdade, segurança e persistência antes de converter tipos. |
| 1 — Piloto pequeno | `SmsSendRequest`, `SmsSendResult` e consumidores; comprovar que factories, status e corpo HTTP permanecem iguais. |
| 2 — Credenciamento | Payload Pagar.me, dados internos PF/PJ, mapeadores, prévia, resultados/diagnósticos e referências; dividir em sublotes compiláveis. |
| 3 — Configurações | Contratos administrativos e snapshots de serviços; preservar criptografia, `DEV_MODE`, valores vazios, autorização e política de cache. |
| 4 — Administração e perfil | Contratos administrativos, cadastro, telefone, município e remuneração; preservar validação, permissões, paginação e metadados. |
| 5 — Autenticação e VOs críticos | Contratos de login/sessão, principal JWT e limite de tentativas; executar por último, mantendo o mecanismo atual. |
| 6 — Revisão global | Retornos, composição, condições e comentários em todas as camadas; testes completos e conferência das exceções autorizadas. |

Cada sublote inclui os consumidores e testes necessários para compilar. Não haverá uma etapa que renomeie todos os arquivos e deixe referências quebradas para corrigir depois. Não haverá mudança de banco, migration ou chamada real a provedores.

## 5. Riscos e pontos de aprovação

1. **Setters em VOs e principal de segurança:** podem permitir alterar chaves de mapa, contexto de autenticação ou snapshots de credenciais. Aprovar as exceções nominais do plano ou definir outro desenho antes da execução.
2. **JSON e validação:** anotações em componentes de records precisam acompanhar campos/acessores dos Beans. Preservar `WRITE_ONLY`, `JsonIgnoreType`, campos opcionais, ausência versus `null`, números versus strings e validação aninhada.
3. **Dados criptografados já persistidos:** `Dados` e `DiagnosticoHttp` são lidos por Jackson; um rename Java não pode alterar suas propriedades nem a lógica de leitura do legado. Testar sem regravar registros reais.
4. **Comportamento de construção:** `Dados` infere a modalidade legada no construtor compacto; o payload externo aplica padrões no construtor auxiliar. Precisam de preparação/finalização explícitas e testes antes de remover construtores.
5. **Igualdade e coleções:** records implementam `equals/hashCode`; classes mutáveis não podem perder essa semântica ou ganhar outra por uso automático de Lombok. Evitar alterações de cópia e de ordenação sem aprovação.
6. **OpenAPI:** renomear classes pode mudar identificadores de schemas, mesmo com JSON estável. Preservar os nomes publicados por configuração explícita quando necessário; qualquer mudança pública restante exige nova aprovação.
7. **JPA e injeção:** não trocar construtores de DI por setters, não renomear entidades/chaves por conveniência e não modificar `@MapsId`, consultas ou limites de transação.

O piloto e os lotes seguintes ficam condicionados à aprovação deste planejamento. Uma incompatibilidade que não possa ser resolvida sem alterar comportamento será registrada e reapresentada antes de continuar a parte afetada.

## 6. Validação e aceitação

A futura implementação deverá comprovar igualdade estrutural de JSON antes/depois para cada família de contratos, inclusive erros e dados legados; invariantes de construção; mensagens/status HTTP; igualdade e hashing; autorização; mapeamentos JPA e integração dos consumidores.

Preservar especificamente no credenciamento: nascimento `dd/MM/uuuu`, ausência de `mother_name`, padrões de transferência/antecipação, `metadata.companyToken` dinâmico, seleção global de ambiente, estrutura distinta PF/PJ, zeros bancários e controle de tentativas. Nenhuma dessas regras será rediscutida nesta tarefa.

Comandos previstos: backend `mvn clean verify`; frontend `npm run typecheck`, `npm test`, `npm run build`; Android `./gradlew assembleDebug testDebugUnitTest`. As suítes PostgreSQL só serão executadas após conferir uma instância isolada do banco `hub_de_servicos`. Integrações externas usarão mocks. Testes reais não fazem parte desta padronização.

**Registro da entrega inicial de planejamento:** naquela fase houve somente inspeção estática. Após autorização, os builds e testes foram executados conforme a seção 8; nenhuma requisição real a provedores foi realizada.

## 7. Arquivos e limites

São propostos até 25 arquivos com tipos de dados/contexto de produção sujeitos a classificação ou migração, além dos consumidores localizados no inventário. Esse número não inclui automaticamente todos os 122 arquivos como alterações necessárias. Os 18 arquivos Java de teste serão conferidos por referência e cenário. O plano lista nominalmente os 83 Beans, os 6 records candidatos a VO, o wrapper de autenticação e as exceções.

Após autorização, também será proposto atualizar `AGENTS.md`, `README.md` e `hub-de-servicos-backend/README.md` para consolidar as convenções. O `pom.xml` não precisa mudar na estratégia proposta. Os projetos externos Bigdim e Gerenciamento de Usuários ficam fora do escopo. Frontend e Android terão apenas validação de contrato, sem refatoração de linguagem ou novas funcionalidades.

A aprovação deverá abranger o escopo, os lotes e a política de exceções; não representa aprovação para alterações de contratos, banco, comportamento ou novas dependências.

## Registro de autorização

Em 24/11/2026 21:33 (America/Sao_Paulo), o usuário autorizou: “Pode iniciar as tasks de `2026-11-24-21-15-padronizacao-estilo-codigo-java`”. A autorização abrange o planejamento apresentado e suas exceções: VOs imutáveis com getters explícitos, manutenção do record de configuração, identidade JPA e injeção por construtor. Para `AutenticacaoPagarme.Conta`, adota-se a classificação proposta `ContaVO`, preservando a factory e o construtor privados.


## 8. Execução autorizada — resultado real

Atualização: 24/11/2026 22:11 — America/Sao_Paulo.

### Implementação

- Convertidos os **83 records de transporte de produção** em classes finais `...Bean`, com campos privados, construtor sem argumentos e getters/setters explícitos. Os quatro records usados como fixtures de teste seguem o mesmo padrão.
- Aplicado `VO` aos seis records internos aprovados e ao wrapper `AutenticacaoPagarme.ContaVO`. Esses sete tipos mantêm a construção controlada e não receberam setters. O wrapper não ganhou um getter para a SecretKey.
- Preservados o record Spring `ConfiguracaoAutenticacao`, os nomes de entidades/chaves JPA, os construtores de injeção e os construtores de domínio existentes. `DadosBean` mantém um `@JsonCreator` específico para finalizar a inferência da modalidade legada depois de receber todos os blocos.
- Atualizados imports, assinaturas, factories, referências de método e consumidores. Os defaults de transferência, antecipação e metadata foram separados em `MapeadorPagarme.prepararPadroes`; continuam com os mesmos valores e com o ID dinâmico do usuário. A preparação do banco foi extraída de `MapeadorPagarme.preparar`.
- Separadas construções antes de retornos e dentro de outros construtores; condições ganharam blocos. Mantida a avaliação preguiçosa de lambdas e do curto-circuito, inclusive validação de e-mail e autorização por vínculo.
- Mantidos os formatos existentes, inclusive nascimento `dd/MM/uuuu`, ausência de `mother_name`, valores monetários e zeros bancários. Nenhuma correção funcional de integração foi incluída nesta solicitação.
- Preservados os `toString` que ocultavam segredos e a semântica de igualdade/hash dos records convertidos, inclusive comparação de `byte[]` por referência. Beans mutáveis não devem ser modificados enquanto utilizados como chaves de mapas/sets; as chaves internas usadas pelo controle de tentativas continuam imutáveis.
- Não foram alterados `pom.xml`, recursos de execução, migrations, frontend ou Android. Não houve escrita no banco do usuário nem chamadas reais à Stone/Infobip.

### Compatibilidade e escolhas técnicas

Os testes de caracterização foram consolidados em `ContratosCompatibilidadeTest`, com fixtures capturadas antes da conversão para todos os 83 contratos, em vez dos nomes de testes por família inicialmente sugeridos. Há comparação do JSON serializado em duas variantes, Bean Validation e nomes/estruturas de schemas OpenAPI, além de getters/setters, igualdade/hash, legado cifrado, campos ignorados, constraints de elementos de coleções e segurança do diagnóstico. As fixtures usam exclusivamente dados sintéticos.

Os nomes publicados foram mantidos por `@Schema(name = "nome anterior")`. Durante a conversão, descobriu-se que constraints de elementos genéricos em campos de classes faziam o Swagger adicionar limites aos itens que não existiam no schema anterior. As constraints de elementos ficaram nos getters, preservando a validação e o schema original; os testes verificam ambos. Nenhuma annotation de segredo foi removida.

Foi usado o formatador Eclipse JDT já instalado apenas como ferramenta local. Não foi adicionado gerador, plugin ou dependência ao projeto. O padrão resultante permanece em Java explícito.

### Inventário final

| Verificação | Resultado |
| --- | --- |
| Fontes Java de produção / teste | 122 / 21; três novas classes de teste |
| Beans convertidos de produção / fixtures | 83 / 4 |
| VOs aprovados | 6 records + 1 wrapper |
| Records restantes | 7: os seis VOs e a configuração Spring |
| `return new` / `new` dentro de outro construtor | 0 / 0, em produção e testes |
| `if` sem bloco | 0, em produção e testes |
| Contratos JSON / validação / schemas comparados | 83; duas variantes de valores por contrato |
| Anotações dos 30 arquivos em `entity` | Iguais ao estado anterior, desconsiderando espaçamento |
| Mudanças em migrations, recursos, dependências, frontend e Android | Nenhuma |
| `git diff --check` | Sem erros |

A inspeção léxica é complementar aos testes e à revisão dos fluxos críticos; não prova isoladamente a ausência de mudança funcional. A execução com PostgreSQL permanece necessária.

### Builds e testes

| Projeto / comando | Resultado observado |
| --- | --- |
| Backend antes da migração: `mvn -o -f hub-de-servicos-backend/pom.xml -DargLine=-javaagent:<byte-buddy-agent-local> clean verify` | Build passou; 105 testes descobertos, 62 executados, 43 PostgreSQL desativados; zero falhas |
| Piloto SMS: `SmsServiceTest,InfobipConfiguracaoTest,InfobipSmsProviderTest` | 22 passaram; transporte controlado |
| Piloto credenciamento: `PagarmeRecebedorTest,ClienteRecebedorPagarmeTest` | 22 passaram; HTTP mockado |
| Backend final: mesmo `mvn -o ... clean verify` com Java 21.0.13 e agente já resolvido pelo projeto | **BUILD SUCCESS**; 115 testes descobertos, **72 executados, zero falhas/erros**, 43 desativados |
| Frontend: `npm run typecheck` | Passou |
| Frontend: `npm test` | 55 testes: 52 passaram, 3 falharam em `UsuarioFormulario.test.ts` |
| Frontend: `npm run build` | Build de produção passou |
| Android: `./gradlew --offline --no-daemon -Pkotlin.compiler.execution.strategy=in-process assembleDebug testDebugUnitTest`, com cache Gradle copiado para diretório temporário gravável | **BUILD SUCCESSFUL**; a tarefa unitária estava `UP-TO-DATE`, sem reexecução dos testes |
| Inicialização de PostgreSQL 12 temporário, exclusivo para testes | Bloqueada em `initdb`: `could not create shared memory segment: Operation not permitted`; nenhum servidor iniciou |

As três falhas do frontend procuram `#usuario-form-login`, que já está comentado em `UsuarioFormulario.vue`. Esses arquivos não foram alterados nesta tarefa. Não foram modificados os testes ou o formulário para ocultar a divergência. O Android não teve alterações de código; a evidência é o build compatível e o reaproveitamento válido do resultado de teste pelo Gradle, sem alegar execução nova de cada cenário.

As 43 integrações PostgreSQL pertencem às suítes de persistência, autenticação, administração e perfil. Não foram habilitadas contra o banco de trabalho. Assim, persistência real, revogação/refresh transacionais, concorrência, histórico e isolamento entre usuários não têm validação integrada nova nesta execução. Os testes HTTP/unitários dessas áreas que não exigem banco foram executados.

### Pendências e divergências

1. Disponibilizar uma instância PostgreSQL isolada e migrada para executar as 43 integrações. O ambiente bloqueou o PostgreSQL temporário por memória compartilhada; não se tentou contornar a restrição usando dados reais. As flags e variáveis de conexão permanecem documentadas no README do backend.
2. Resolver, em solicitação própria se necessário, a divergência já existente entre os três testes do frontend e o campo de login comentado. A validação completa do consumidor continua pendente.
3. Revalidar os cenários transacionais das tarefas T024, T027, T031 e T036 nessa instância isolada. A revisão e a conversão de código dessas áreas foram aplicadas, mas seus requisitos de teste com banco permanecem parciais.

A implementação de estilo está aplicada; **a solicitação não está declarada integralmente concluída**, por essas validações pendentes. Não houve alteração de escopo funcional, de contrato público, de esquema ou de dependência. Os métodos de acesso Java foram alterados conforme o plano; integrações Java externas ao monólito, se existirem fora deste repositório, precisam recompilar seus imports e acessores.

### Arquivos efetivamente alterados

Os caminhos seguintes são relativos à raiz do Hub. São 105 arquivos Java existentes modificados, oito renomeações de arquivo e três classes novas de teste. Alterações de documentação são listadas separadamente. A relação distingue a execução das alterações locais já presentes antes dela.

**Documentação:** `AGENTS.md` (preservado o conteúdo local), `README.md`, `hub-de-servicos-backend/README.md` e os quatro documentos desta pasta.

**Renomeações:**

- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/autenticacao/LoginEntrada.java` → `LoginEntradaBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/autenticacao/RenovacaoEntrada.java` → `RenovacaoEntradaBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/autenticacao/TokensAutenticacao.java` → `TokensAutenticacaoBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/autenticacao/UsuarioAutenticado.java` → `UsuarioAutenticadoBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/credenciamento/PagarmeRecebedor.java` → `PagarmeRecebedorBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/sms/SmsSendRequest.java` → `SmsSendRequestBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/sms/SmsSendResult.java` → `SmsSendResultBean.java` (no mesmo diretório).
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/autenticacao/IdentidadeJwt.java` → `IdentidadeJwtVO.java` (no mesmo diretório).

**Novos testes e fixture:**

- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/bootstrap/administracao/BootstrapAdminTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/dto/ContratosCompatibilidadeTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/dto/ValoresImutaveisTest.java`.
- `hub-de-servicos-backend/src/test/resources/contratos/baseline-java.json`.

**Produção — arquivos modificados:**

- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/bootstrap/administracao/BootstrapAdmin.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/bootstrap/administracao/InicializacaoAdministrativa.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/common/StatusCadastro.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/config/autenticacao/ConfiguracaoAutenticacao.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/config/autenticacao/SegurancaWeb.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/administracao/AdministracaoAplicacaoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/administracao/AdministracaoConfiguracaoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/administracao/AdministracaoCredenciamentoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/administracao/AdministracaoPlataformaController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/administracao/ErrosAdministracaoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/app/AutenticacaoAppController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/app/CredenciamentoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/app/ErrosAutenticacaoAppController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/app/PerfilAppController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/autenticacao/AutenticacaoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/autenticacao/ErrosAutenticacaoController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/controller/autenticacao/SessaoNavegadorController.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/administracao/ContratosAdministrativos.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/app/ContratosApp.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/app/ContratosPerfilApp.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/configuracao/ContratosConfiguracao.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/credenciamento/ContratosCredenciamento.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/usuario/EmailUsuario.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/usuario/SenhaNova.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/dto/usuario/SituacaoTelefone.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/aplicacao/AplicacaoPermissaoId.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/aplicacao/TipoAplicacao.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/auditoria/AuditoriaEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/configuracao/ServicoExterno.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/credenciamento/CredenciamentoEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/credenciamento/DiagnosticoRecebedorEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/credenciamento/RecebedorExternoEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/credenciamento/TentativaRecebedorEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/perfil/PerfilPermissaoEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/perfil/PerfilPermissaoId.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/sessao/SessaoEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/sessao/StatusSessao.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/sessao/TokenAtualizacaoEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/DesafioTelefoneEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/MunicipioEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/PapelUsuario.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/StatusUsuario.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/UsuarioAplicacaoId.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/UsuarioEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/UsuarioPerfilEntity.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/UsuarioPerfilId.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/entity/usuario/UsuarioPermissaoPlataformaId.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/provider/SmsProvider.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/provider/infobip/InfobipSmsProvider.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/administracao/ConsultaUsuariosRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/aplicacao/AplicacaoRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/credenciamento/DiagnosticoRecebedorRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/credenciamento/TentativaRecebedorRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/sessao/SessaoRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/sessao/TokenAtualizacaoRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/usuario/DesafioTelefoneRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/usuario/UsuarioPerfilRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/usuario/UsuarioPermissaoPlataformaRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/repository/usuario/UsuarioRepository.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/AcessoAdministrativo.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/AdministracaoAplicacaoService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/AdministracaoConfiguracaoService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/AdministracaoPlataformaService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/PermissoesPlataformaService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/administracao/RegistroAdministrativo.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/app/AutenticacaoAppService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/app/ConfirmacaoTelefoneService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/app/LimiteTentativasApp.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/app/PerfilAppService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/autenticacao/AutenticacaoService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/autenticacao/TokenJwt.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/autorizacao/AutorizacaoService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/configuracao/AmbienteServicosService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/configuracao/ConfiguracoesExternasService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/configuracao/CriptografiaCredenciais.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/AutenticacaoPagarme.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/ClienteRecebedorPagarme.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/ComparacaoCredenciamento.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/CredenciamentoService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/DiagnosticoRecebedorService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/DocumentosCredenciamento.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/MapeadorPagarme.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/RecebedorStoneService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/SituacaoCredenciamento.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/credenciamento/ValidacaoCredenciamento.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/sms/SmsProviderResolver.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/sms/SmsService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/usuario/CadastroUsuarioService.java`.
- `hub-de-servicos-backend/src/main/java/br/com/hubdeservicos/service/usuario/TelefoneUsuarioService.java`.

**Testes existentes — arquivos modificados:**

- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/config/autenticacao/AutenticacaoHttpTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/controller/administracao/AdministracaoPostgresTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/controller/app/AutenticacaoAppPostgresTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/controller/app/PerfilAppPostgresTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/controller/autenticacao/AutenticacaoPostgresTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/persistencia/MapeamentoPersistenciaTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/persistencia/PersistenciaPostgresTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/provider/infobip/InfobipConfiguracaoTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/provider/infobip/InfobipSmsProviderTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/provider/infobip/InfobipTransporteControlado.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/administracao/AdministracaoConfiguracaoHttpTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/autenticacao/TokenJwtTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/configuracao/CriptografiaCredenciaisTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/credenciamento/ClienteRecebedorPagarmeTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/credenciamento/PagarmeRecebedorTest.java`.
- `hub-de-servicos-backend/src/test/java/br/com/hubdeservicos/service/sms/SmsServiceTest.java`.
