# Backend do Hub de Serviços

Monólito modular em Java 21, Spring Boot 3.2.4 e Maven. Em `br.com.hubdeservicos`, as camadas `controller`, `repository`, `service`, `entity` e `dto` têm subpacotes por funcionalidade, como `controller.administracao` e `entity.usuario`. Configurações, exceções, bootstrap, provedores e tipos compartilhados ficam em `config`, `exception`, `bootstrap`, `provider` e `common`. A classe principal fica no pacote raiz para o escaneamento do Spring. Os testes seguem as camadas correspondentes e as suítes transversais ficam em `persistencia`. As entidades terminam com `Entity` e mantêm mapeamentos explícitos de tabelas e colunas. As migrações ficam em `src/main/resources/db/migration`.

## Ambientes

| Perfil | Banco | API e autenticação |
| --- | --- | --- |
| `local` | `DATABASE_URL` opcional. Padrão `jdbc:postgresql://127.0.0.1:5432/hub_de_servicos` | Backend em `127.0.0.1:8080`. Permite HTTP local. Gera um par RSA temporário quando os arquivos de chave não são fornecidos. |
| `homologacao` | `DATABASE_URL` obrigatória. Use a instância de homologação com o banco `hub_de_servicos`. | Use emissor HTTPS e um par RSA da homologação. O cookie de renovação exige HTTPS. |
| `producao` | `DATABASE_URL` obrigatória. Use a instância de produção com o banco `hub_de_servicos`. | Use emissor HTTPS e um par RSA da produção. O cookie de renovação exige HTTPS. |

Em todos os ambientes, forneça `DATABASE_USERNAME` e `DATABASE_PASSWORD` por configuração externa. A URL JDBC deve conter o host e o nome do banco. O datasource e o Liquibase verificam o nome `hub_de_servicos`. Hikari, Liquibase e Hibernate usam o schema `public`, inclusive para as tabelas de controle do Liquibase. O Liquibase verifica o schema da sessão e aplica os changesets pendentes. O Hibernate valida o schema com `ddl-auto: validate`.

Em homologação e produção, defina `HUB_AUTENTICACAO_EMISSOR`, `HUB_AUTENTICACAO_CHAVEPRIVADA` e `HUB_AUTENTICACAO_CHAVEPUBLICA`. Informe a URL HTTPS do ambiente e os caminhos dos arquivos PEM. Use um par RSA correspondente de pelo menos 2048 bits, com chave privada PKCS#8 e pública X.509. Mantenha os arquivos de chave e as credenciais fora do repositório. A chave temporária do perfil local perde a validade ao reiniciar o processo.

Defina `HUB_ADMINISTRACAO_APLICACAOID` com o UUID da aplicação administrativa provisionada no respectivo ambiente. O mesmo valor deve ser usado no bootstrap e na inicialização normal.

## Desenvolvimento local

Em uma instância PostgreSQL de desenvolvimento, use uma conta com permissão para criar o banco e indique um papel existente destinado ao Hub. Esse papel deverá poder aplicar as migrações e acessar o schema da aplicação. Com `PGHOST`, `PGPORT`, `PGUSER` e `DATABASE_USERNAME` configurados para a instância e os papéis escolhidos, execute uma vez:

```sh
createdb -O "$DATABASE_USERNAME" hub_de_servicos
```

No zsh, com a conta do Hub configurada em `DATABASE_USERNAME`, informe a senha e confirme o destino por uma consulta de leitura:

```zsh
read -rs 'DATABASE_PASSWORD?Senha do PostgreSQL: '
printf '\n'
export DATABASE_USERNAME DATABASE_PASSWORD
export DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/hub_de_servicos'
PGPASSWORD="$DATABASE_PASSWORD" PGOPTIONS='-c default_transaction_read_only=on' \
  psql -X -w -h 127.0.0.1 -p 5432 -U "$DATABASE_USERNAME" -d hub_de_servicos \
  -Atc 'select current_database(), current_schema()'
```

Quando o resultado for `hub_de_servicos|public`, inicie o backend para aplicar as migrações:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

No Eclipse, importe `hub-de-servicos-backend` como projeto Maven. Use a classe `br.com.hubdeservicos.HubDeServicosApplication` e configure `SPRING_PROFILES_ACTIVE=local` e as mesmas variáveis na execução.

### Aplicação pelo Liquibase CLI

Depois de conferir o schema de destino, consulte as tabelas existentes e os registros de `public.databasechangelog`, se a tabela de controle existir. Se houver tabelas do Hub sem o histórico correspondente, confronte o catálogo com os changesets antes de aplicar a migração. A partir deste diretório, com `DATABASE_USERNAME` e `DATABASE_PASSWORD` definidos no terminal local, o fluxo para o destino local é:

```sh
export LIQUIBASE_COMMAND_URL='jdbc:postgresql://127.0.0.1:5432/hub_de_servicos?currentSchema=public'
export LIQUIBASE_COMMAND_USERNAME="$DATABASE_USERNAME"
export LIQUIBASE_COMMAND_PASSWORD="$DATABASE_PASSWORD"
export LIQUIBASE_COMMAND_CHANGELOG_FILE='db/migration/db.changelog-master.xml'
export LIQUIBASE_COMMAND_DEFAULT_SCHEMA_NAME=public
export LIQUIBASE_LIQUIBASE_SCHEMA_NAME=public
export LIQUIBASE_SEARCH_PATH=src/main/resources
export LIQUIBASE_ANALYTICS_ENABLED=false
liquibase validate
liquibase update-sql
```

Depois de conferir a prévia, execute `liquibase update`. Para validar pelo Hibernate com as mesmas credenciais e o perfil `local`, execute `mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--spring.liquibase.enabled=false`. Essa inicialização aplica `ddl-auto: validate` ao schema instalado.

### Reversão de migrações

O changeset `2-tokens-integridade-cadeia` acrescenta o índice único de raiz por família e o check de autorreferência. Sua reversão remove esses dois objetos e conserva todas as linhas. Se houver registros incompatíveis, a aplicação do changeset falha e a transação reverte a alteração. Investigue os registros antes de decidir como tratá-los.

Os rollbacks históricos de criação de tabela removem as respectivas tabelas e seus dados, inclusive o histórico de auditoria. O rollback de `2-usuario-aplicacao-ativo` remove a coluna `ativo`. Reaplicar esse changeset preenche os vínculos existentes com `true`, perdendo o estado inativo anterior. O rollback de `2-usuario-indices` conserva os registros, mas remove a proteção contra login e e-mail duplicados sem distinção de caixa.

O changeset `1-permissoes-diretas-plataforma` não tem rollback suportado. Ele cria a tabela de concessões, cadastra ou atualiza permissões e pode copiar vínculos legados. Uma reversão completa precisa de restauração de backup ou de uma migração corretiva que considere os dados existentes. Use a prévia do Liquibase para inspecionar o SQL antes de qualquer reversão.

## Primeiro administrador

Depois das migrações, encerre a primeira execução local e rode o bootstrap no mesmo terminal que tem as variáveis do banco. Informe um login, um e-mail e uma senha com pelo menos 12 caracteres e até 72 bytes em UTF-8:

```zsh
export HUB_ADMINISTRACAO_APLICACAOID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
(
  set -e
  read -r 'HUB_BOOTSTRAP_LOGIN?Login do primeiro administrador: ' </dev/tty
  read -r 'HUB_BOOTSTRAP_EMAIL?E-mail do primeiro administrador: ' </dev/tty
  read -rs 'HUB_BOOTSTRAP_SENHA?Senha do primeiro administrador: ' </dev/tty
  printf '\n'
  export HUB_BOOTSTRAP_LOGIN HUB_BOOTSTRAP_EMAIL HUB_BOOTSTRAP_SENHA
  mvn spring-boot:run -Dspring-boot.run.profiles=local,bootstrap-admin
)
```

Guarde o valor de `HUB_ADMINISTRACAO_APLICACAOID` na configuração do ambiente e reutilize-o nas próximas execuções. O bootstrap cria a aplicação reservada `hub-de-servicos-admin` e a primeira conta. Ele recusa repetir a inicialização para uma conta ou aplicação existente. Em ambientes publicados, combine `bootstrap-admin` com o perfil do próprio ambiente.

Reinicie o backend no perfil `local` e entre pelo frontend. Para chamadas manuais, use `POST /api/v1/aplicacoes/hub-de-servicos-admin/autenticacao/login` e informe o access token em **Authorize** no [Swagger UI local](http://127.0.0.1:8080/swagger-ui/index.html).

## Contrato de autenticação

O prefixo é `/api/v1/aplicacoes/{slug}/autenticacao`.

| Método e rota | Credencial | Resultado |
| --- | --- | --- |
| `POST /login` | JSON com `login` e `senha` | Access token e refresh token |
| `POST /renovar` | JSON com `tokenAtualizacao` | Novo par de tokens |
| `GET /eu` | `Authorization: Bearer <tokenAcesso>` | Identidade na aplicação |
| `GET /permissoes` | `Authorization: Bearer <tokenAcesso>` | Lista de códigos de permissão efetivos |
| `POST /logout` | `Authorization: Bearer <tokenAcesso>` | Sessão revogada, HTTP 204 |

O login usa BCrypt com custo 12 e aceita senhas de até 72 bytes UTF-8. Requer usuário ativo, aplicação ativa e vínculo. A resposta de login e renovação contém `tokenAcesso`, `tokenAtualizacao` e `expiracaoTokenAcesso`.

Para aplicações públicas, envie as credenciais do usuário no login e o refresh token na renovação. Para aplicações confidenciais, envie também `X-Client-Secret` a partir do servidor consumidor. Gere esse segredo com 32 bytes aleatórios codificados em Base64URL sem padding. Em configuração externa do Spring Boot, associe o UUID da aplicação ao SHA-256 hexadecimal do texto do segredo sob `hub.autenticacao.clientes-confidenciais`. Use TLS no ambiente publicado e limite tentativas de login no gateway antes de expor a API.

Consumidores podem buscar a chave pública em `GET /.well-known/jwks.json`. Valide o emissor esperado, a audiência com o slug da aplicação e o algoritmo RS256. Nas rotas protegidas do Hub, a consulta à sessão aplica a revogação imediatamente. Um consumidor que valida o JWT localmente observa sua validade até a expiração.

O navegador usa as rotas `/navegador/login`, `/navegador/renovar` e `/navegador/logout` sob o mesmo prefixo. Envie `X-Sessao-Navegador: 1` na mesma origem. Login e renovação retornam o access token em JSON e entregam a credencial de renovação em um cookie `HttpOnly` e `SameSite=Strict`.

O access token dura até cinco minutos. A sessão expira em até 30 dias, com limite de sete dias sem renovação. A cada renovação, substitua o refresh token anterior pelo recebido. Se um refresh token já consumido for reapresentado no contexto correto, a plataforma revoga a família e exige novo login.

## Autorização por aplicação

O endpoint `GET /permissoes` valida a sessão e a aplicação do access token. O backend consulta os perfis ativos do usuário naquela aplicação e inclui apenas permissões ativas habilitadas para ela. O resultado contém códigos ordenados e sem duplicatas. Um usuário vinculado sem permissões recebe `[]`.

Use `@PreAuthorize("@autorizacao.temPermissao(authentication, #slug, 'CODIGO_DA_OPERACAO')")` em métodos protegidos chamados por outros beans. O método deve receber o slug da aplicação sobre a qual atua e usar um código de permissão definido pela operação. O backend consulta o estado atual a cada verificação e nega com HTTP 403 quando uma operação protegida exige uma permissão ausente.

## Rotas e escopos administrativos

| Prefixo | Permissão | Operações |
| --- | --- | --- |
| `/api/v1/admin` | `PLATAFORMA_ADMINISTRAR` na aplicação reservada configurada | Cadastrar, consultar e atualizar aplicações, usuários e permissões, alterar senhas, habilitar permissões nas aplicações e consultar a auditoria global |
| `/api/v1/aplicacoes/{slug}/admin` | `APLICACAO_ADMINISTRAR` na aplicação da sessão ou administrador da plataforma | Gerenciar perfis, permissões dos perfis, vínculos dos usuários e suas atribuições, revogar sessões e consultar auditoria da aplicação |

As listagens paginadas usam `pagina` a partir de zero e `tamanho` entre 1 e 100. O retorno contém `itens`, `pagina`, `tamanho` e `total`. As requisições administrativas aparecem no Swagger com os contratos JSON.

Para delegar a administração local, habilite `APLICACAO_ADMINISTRAR` na aplicação, atribua essa permissão a um perfil ativo e atribua o perfil a um usuário vinculado. As mudanças de senha revogam as sessões do usuário. Ao desvincular um usuário, a API revoga suas sessões na aplicação, remove as atribuições de perfil daquele vínculo e preserva o histórico. Reativar o vínculo permite um novo login com os perfis que forem atribuídos novamente.

### Permissões da plataforma

O catálogo reutiliza a tabela `permissao`. O campo `nome_plataforma` identifica as permissões que podem ser atribuídas diretamente pela tabela `usuario_permissao_plataforma`. A migração cadastra `PLATAFORMA_ADMINISTRAR`, `PLATAFORMA_VISITANTE` e `PLATAFORMA_ANALISTA`. O changeset histórico mantém a consulta ao slug `plataforma-admin` para preservar seu checksum. Em um banco novo, a consulta não encontra registros e o bootstrap concede o acesso à aplicação `hub-de-servicos-admin`. As demais aplicações usam seus vínculos e perfis.

- `GET /api/v1/admin/permissoes-plataforma` retorna o catálogo com identificador, código, nome, descrição e status.
- `POST /api/v1/admin/permissoes-plataforma` registra uma nova opção. Envie `codigo` com prefixo `PLATAFORMA_`, `nome` e, opcionalmente, `descricao`. A tela carrega as opções do catálogo.
- `POST /api/v1/admin/usuarios` e `PUT /api/v1/admin/usuarios/{id}` recebem e retornam a lista `permissoesPlataforma`. No PUT, omitir o campo preserva as atribuições. Enviar `[]` remove todas as atribuições, respeitando a proteção do último administrador ativo.
- `GET /api/v1/admin/usuarios/{id}` retorna `permissoesPlataforma` com as atribuições e `permissoes` com os acessos efetivos.
- `GET /api/v1/aplicacoes/hub-de-servicos-admin/autenticacao/permissoes` retorna as permissões diretas ativas da conta autenticada. A primeira atribuição ativa cria o vínculo técnico para login. A remoção das atribuições preserva vínculos existentes.

A classificação de visitante ou analista fica disponível na sessão. A autorização das operações administrativas continua exigindo `PLATAFORMA_ADMINISTRAR`. As concessões de perfis antigos da aplicação reservada permanecem no histórico, e o backend consulta as atribuições diretas para decidir o acesso à plataforma.

## Envio de SMS

Os módulos internos usam `SmsService.send(SmsSendRequest)`. O serviço lê a configuração ativa `SMS_PROVEDOR_ATIVO` do banco a cada solicitação. O `SmsProviderResolver` reúne os beans `SmsProvider` por código único e escolhe o delegate. O resultado informa aceitação pelo provedor, identificador externo quando houver e código de erro quando aplicável. O serviço não oferece um endpoint HTTP de envio. Evite reenviar automaticamente após timeout, pois o provedor pode ter aceitado a solicitação antes de a conexão falhar.

O changeset `1-configuracao-sistema` cria `configuracao_sistema` e insere a configuração inicial ativa com valor `TEXTBEE`. A tabela contém `id` UUID, `chave` única, `valor`, `descricao`, `tipo`, `status`, `data_criacao` e `data_atualizacao`. O changeset é aditivo e não define rollback que apague a tabela e suas configurações. As alterações administrativas registram operador, aplicação e valores anterior e novo na tabela `auditoria`.

### Configurar o TextBee

- Defina `TEXTBEE_API_KEY` no ambiente do backend. No Eclipse, configure-a na aba **Environment** da execução local. Em outros ambientes, forneça-a pelo gerenciador de segredos.
- Defina `TEXTBEE_DEVICE_ID` se precisar usar um dispositivo específico. Sem essa variável, o TextBee seleciona o dispositivo padrão ou um dispositivo habilitado disponível na conta.
- O envio real precisa de conta configurada e dispositivo Android com SIM e permissão de SMS. Use um destinatário autorizado, preferencialmente em formato internacional. A configuração pode ser consultada sem chave. Uma tentativa interna de envio sem chave retorna `CREDENCIAIS_AUSENTES`.
- O adaptador usa `POST /api/v1/gateway/send-sms` com `x-api-key`. O `smsBatchId` identifica o lote quando retornado. A aceitação ou o despacho para o dispositivo emissor não confirma entrega ao destinatário. [Documentação oficial do TextBee](https://textbee.dev/docs/sending-sms/sending-sms)

### Administração

A tela **Configurações → Sistema** usa as rotas abaixo, protegidas pela autorização administrativa existente (`PLATAFORMA_ADMINISTRAR` na aplicação reservada).

| Método e rota | Resultado |
| --- | --- |
| `GET /api/v1/admin/configuracoes` | Lista as configurações permitidas. Atualmente expõe apenas a seleção de SMS. |
| `GET /api/v1/admin/configuracoes/sms/provedores` | Lista os provedores registrados com código e nome de exibição. |
| `GET /api/v1/admin/configuracoes/sms/provedor-ativo` | Retorna o registro da configuração de seleção de SMS, incluindo seu status. |
| `PUT /api/v1/admin/configuracoes/sms/provedor-ativo` | Valida o código enviado em `{"codigo":"TEXTBEE"}` e persiste a escolha. |

Para adicionar um provedor, implemente `SmsProvider` em `sms/provider` e registre-o como bean Spring, com código exclusivo, nome de exibição e configuração externa para seus segredos. O resolver e a tela passam a listar a implementação. Após a implantação do novo bean, a escolha é efetuada no painel e vale para o envio seguinte. Nenhuma credencial faz parte dos contratos administrativos.

## Testes

`mvn clean verify` compila, executa os testes que independem de PostgreSQL e gera o JAR executável em `target/`. Para executar as três suítes de integração, use uma instância de teste com o banco `hub_de_servicos` já migrado e exporte `DATABASE_USERNAME`, `DATABASE_PASSWORD` e `TESTE_DATABASE_URL` com a conexão JDBC da instância de teste:

```sh
TESTE_PERSISTENCIA_POSTGRES=true TESTE_AUTENTICACAO_POSTGRES=true \
  TESTE_ADMINISTRACAO_POSTGRES=true mvn verify
```

`TESTE_POSTGRES_SCHEMA` permite indicar um schema de teste já migrado. O padrão é `public`. As suítes gravam registros temporários e a sequência de auditoria pode avançar. Elas verificam persistência, autenticação, renovação concorrente, logout, isolamento e operações administrativas.
