# Backend da plataforma de gerenciamento

## Iniciar no Mac

Use Java 21, Maven e o PostgreSQL local com o banco `gerenciamento_usuarios`. No diretório do backend, execute no zsh:

```zsh
(
  set -e
  read -r 'DATABASE_USERNAME?Usuário do PostgreSQL: ' </dev/tty
  read -rs 'DATABASE_PASSWORD?Senha do PostgreSQL: ' </dev/tty
  printf '\n'
  export DATABASE_USERNAME DATABASE_PASSWORD
  destino=$(PGPASSWORD="$DATABASE_PASSWORD" PGOPTIONS='-c default_transaction_read_only=on' \
    /opt/homebrew/opt/postgresql@12/bin/psql -X -w -h 127.0.0.1 -p 5432 \
    -U "$DATABASE_USERNAME" -d gerenciamento_usuarios -Atc 'select current_database()')
  [[ "$destino" == 'gerenciamento_usuarios' ]] || exit 1
  printf 'Banco confirmado: %s\n' "$destino"
  jdbc='jdbc:postgresql://127.0.0.1:5432/gerenciamento_usuarios'
  mvn spring-boot:run -Dspring-boot.run.profiles=local \
    "-Dspring-boot.run.arguments=--spring.datasource.url=$jdbc --spring.liquibase.url=$jdbc"
)
```

O perfil `local` escuta em `127.0.0.1:8080`. Confira a linha `Tomcat started on port 8080` no console ou abra o [Swagger UI local](http://127.0.0.1:8080/swagger-ui/index.html). Pelo Eclipse, crie uma configuração **Java Application** para `BaseGerenciamentoPlataformaApplication`. Em **Environment**, defina `SPRING_PROFILES_ACTIVE=local`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`. Em **Program arguments**, informe as URLs do datasource e do Liquibase após confirmar o banco, como no bloco acima.

O Liquibase aplica apenas changesets pendentes e o Hibernate valida o schema. O perfil local usa uma chave RSA temporária na ausência de arquivos PEM. Os JWTs emitidos por essa chave perdem a validade após reiniciar o processo.

## Configuração da assinatura

Para um ambiente persistente, forneça um emissor HTTPS e um par RSA correspondente de pelo menos 2048 bits. Use PEM PKCS#8 para a chave privada e PEM X.509 para a pública. Configure `PLATAFORMA_AUTENTICACAO_EMISSOR`, `PLATAFORMA_AUTENTICACAO_CHAVEPRIVADA` e `PLATAFORMA_AUTENTICACAO_CHAVEPUBLICA`. As duas últimas variáveis recebem caminhos de arquivos. Proteja a chave privada e mantenha-a fora do repositório.

Aplicações consumidoras podem buscar a chave pública em `GET /.well-known/jwks.json`. Para validar um access token, configure o emissor esperado, o slug da aplicação como audiência e o algoritmo RS256. A plataforma consulta a sessão em suas próprias rotas protegidas e aplica revogação imediata nelas. Um consumidor que verifica o JWT localmente considera a validade do token até sua expiração, de até cinco minutos.

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

Para aplicações públicas, envie as credenciais do usuário no login e o refresh token na renovação. Para aplicações confidenciais, envie também `X-Client-Secret` a partir do servidor consumidor. Gere esse segredo com 32 bytes aleatórios codificados em Base64URL sem padding. Em configuração externa do Spring Boot, associe o UUID da aplicação ao SHA-256 hexadecimal do texto do segredo sob `plataforma.autenticacao.clientes-confidenciais`. Use TLS no ambiente publicado e limite tentativas de login no gateway antes de expor a API.

O access token dura até cinco minutos. A sessão expira em até 30 dias, com limite de sete dias sem renovação. A cada renovação, substitua o refresh token anterior pelo recebido. Se um refresh token já consumido for reapresentado no contexto correto, a plataforma revoga a família e exige novo login.

## Autorização por aplicação

O endpoint `GET /permissoes` valida a sessão e a aplicação do access token. O backend consulta os perfis ativos do usuário naquela aplicação e inclui apenas permissões ativas habilitadas para ela. O resultado contém códigos ordenados e sem duplicatas. Um usuário vinculado sem permissões recebe `[]`.

Use `@PreAuthorize("@autorizacao.temPermissao(authentication, #slug, 'CODIGO_DA_OPERACAO')")` em métodos protegidos chamados por outros beans. O método deve receber o slug da aplicação sobre a qual atua e usar um código de permissão definido pela operação. O backend consulta o estado atual a cada verificação e nega com HTTP 403 quando uma operação protegida exige uma permissão ausente.

## Administração

Inicie o backend normalmente uma vez para aplicar a migração `2-usuario-aplicacao-ativo`. Os vínculos já cadastrados ficam ativos.

### Primeiro administrador

Com as migrações aplicadas, execute o bloco abaixo no diretório do backend para criar a conta e a aplicação reservada `plataforma-admin`. Use uma senha com pelo menos 12 caracteres e até 72 bytes em UTF-8.

```zsh
(
  set -e
  read -r 'DATABASE_USERNAME?Usuário do PostgreSQL: ' </dev/tty
  read -rs 'DATABASE_PASSWORD?Senha do PostgreSQL: ' </dev/tty
  printf '\n'
  read -r 'PLATAFORMA_BOOTSTRAP_LOGIN?Login do primeiro administrador: ' </dev/tty
  read -r 'PLATAFORMA_BOOTSTRAP_EMAIL?E-mail do primeiro administrador: ' </dev/tty
  read -rs 'PLATAFORMA_BOOTSTRAP_SENHA?Senha do primeiro administrador: ' </dev/tty
  printf '\n'
  export DATABASE_USERNAME DATABASE_PASSWORD
  export PLATAFORMA_BOOTSTRAP_LOGIN PLATAFORMA_BOOTSTRAP_EMAIL PLATAFORMA_BOOTSTRAP_SENHA
  id_admin=$(uuidgen | tr '[:upper:]' '[:lower:]')
  jdbc='jdbc:postgresql://127.0.0.1:5432/gerenciamento_usuarios'
  mvn spring-boot:run -Dspring-boot.run.profiles=local,bootstrap-admin \
    "-Dspring-boot.run.arguments=--spring.main.web-application-type=none --spring.datasource.url=$jdbc --spring.liquibase.enabled=false --plataforma.administracao.aplicacao-id=$id_admin"
)
```

Copie o UUID exibido pelo comando e inclua `--plataforma.administracao.aplicacao-id=<UUID>` em **Program arguments** na configuração normal do Eclipse. Use o perfil `local` para iniciar o servidor. Faça login em `POST /api/v1/aplicacoes/plataforma-admin/autenticacao/login` e informe o access token em **Authorize** no Swagger. A inicialização é única e recusa reutilizar uma conta ou aplicação preexistente.

### Rotas e escopos

| Prefixo | Permissão | Operações |
| --- | --- | --- |
| `/api/v1/admin` | `PLATAFORMA_ADMINISTRAR` na aplicação reservada configurada | Cadastrar, consultar e atualizar aplicações, usuários e permissões, alterar senhas, habilitar permissões nas aplicações e consultar a auditoria global |
| `/api/v1/aplicacoes/{slug}/admin` | `APLICACAO_ADMINISTRAR` na aplicação da sessão ou administrador da plataforma | Gerenciar perfis, permissões dos perfis, vínculos dos usuários e suas atribuições, revogar sessões e consultar auditoria da aplicação |

As listagens paginadas usam `pagina` a partir de zero e `tamanho` entre 1 e 100. O retorno contém `itens`, `pagina`, `tamanho` e `total`. As requisições administrativas aparecem no Swagger com os contratos JSON.

Para delegar a administração local, habilite `APLICACAO_ADMINISTRAR` na aplicação, atribua essa permissão a um perfil ativo e atribua o perfil a um usuário vinculado. As mudanças de senha revogam as sessões do usuário. Ao desvincular um usuário, a API revoga suas sessões na aplicação, remove as atribuições de perfil daquele vínculo e preserva o histórico. Reativar o vínculo permite um novo login com os perfis que forem atribuídos novamente.

### Permissões da plataforma

O catálogo reutiliza a tabela `permissao`. O campo `nome_plataforma` identifica as permissões que podem ser atribuídas diretamente pela tabela `usuario_permissao_plataforma`. A migração cadastra `PLATAFORMA_ADMINISTRAR`, `PLATAFORMA_VISITANTE` e `PLATAFORMA_ANALISTA` e copia a concessão administrativa do perfil legado de `plataforma-admin`. As demais aplicações continuam usando seus vínculos e perfis.

- `GET /api/v1/admin/permissoes-plataforma` retorna o catálogo com identificador, código, nome, descrição e status.
- `POST /api/v1/admin/permissoes-plataforma` registra uma nova opção. Envie `codigo` com prefixo `PLATAFORMA_`, `nome` e, opcionalmente, `descricao`. A tela carrega as opções do catálogo.
- `POST /api/v1/admin/usuarios` e `PUT /api/v1/admin/usuarios/{id}` recebem e retornam a lista `permissoesPlataforma`. No PUT, omitir o campo preserva as atribuições. Enviar `[]` remove todas as atribuições, respeitando a proteção do último administrador ativo.
- `GET /api/v1/admin/usuarios/{id}` retorna `permissoesPlataforma` com as atribuições e `permissoes` com os acessos efetivos.
- `GET /api/v1/aplicacoes/plataforma-admin/autenticacao/permissoes` retorna as permissões diretas ativas da conta autenticada. A primeira atribuição ativa cria o vínculo técnico para login. A remoção das atribuições preserva vínculos existentes.

A classificação de visitante ou analista fica disponível na sessão. A autorização das operações administrativas continua exigindo `PLATAFORMA_ADMINISTRAR`. As concessões de perfis antigos da aplicação reservada permanecem no histórico, e o backend consulta as atribuições diretas para decidir o acesso à plataforma.

## Testes

Execute `mvn test` para os testes que independem do PostgreSQL. Para validar login, renovação concorrente, logout e autorização por aplicação, cole o bloco abaixo no zsh a partir do diretório do backend. As credenciais da configuração de depuração do Eclipse são específicas daquela execução.

```zsh
(
  set -e
  read -r 'DATABASE_USERNAME?Usuário do PostgreSQL: ' </dev/tty
  [[ -n "$DATABASE_USERNAME" ]] || { echo 'Informe o usuário do PostgreSQL.'; exit 1; }
  read -rs 'DATABASE_PASSWORD?Senha do PostgreSQL: ' </dev/tty
  printf '\n'
  export DATABASE_USERNAME DATABASE_PASSWORD
  TESTE_AUTENTICACAO_POSTGRES=true mvn -Dtest=AutenticacaoPostgresTest test
)
```

O teste cria registros próprios no schema já migrado e remove-os no encerramento de cada caso. A sequência de auditoria pode avançar.

Com as mesmas credenciais exportadas, rode `TESTE_ADMINISTRACAO_POSTGRES=true mvn -Dtest=AdministracaoPostgresTest test` para validar as APIs administrativas e o isolamento no PostgreSQL. O teste cria seu próprio administrador temporário e remove os registros ao concluir.

Para executar os testes PostgreSQL em um esquema isolado já migrado no banco `gerenciamento_usuarios`, defina `TESTE_POSTGRES_SCHEMA`. O valor padrão é `public`.
