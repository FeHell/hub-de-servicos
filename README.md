# Plataforma de gerenciamento

## Frontend Vue

A primeira tela administrativa está em `base-gerenciamento-plataforma-front`. Com o backend iniciado no Eclipse, abra outro terminal:

```sh
cd /Users/fernandoluciooliveira/dev/projetos/base-gerenciamento-plataforma/base-gerenciamento-plataforma-front
npm ci
npm run dev
```

Abra [a tela de login local](http://127.0.0.1:5173) e use sua conta cadastrada na aplicação `plataforma-admin`. O proxy local encaminha `/api` para `http://127.0.0.1:8080`. A configuração, a organização dos componentes e os comandos de verificação estão no [README do frontend](base-gerenciamento-plataforma-front/README.md).

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
| `POST /logout` | `Authorization: Bearer <tokenAcesso>` | Sessão revogada, HTTP 204 |

O login usa BCrypt com custo 12 e aceita senhas de até 72 bytes UTF-8. Requer usuário ativo, aplicação ativa e vínculo. A resposta de login e renovação contém `tokenAcesso`, `tokenAtualizacao` e `expiracaoTokenAcesso`.

Para aplicações públicas, envie as credenciais do usuário no login e o refresh token na renovação. Para aplicações confidenciais, envie também `X-Client-Secret` a partir do servidor consumidor. Gere esse segredo com 32 bytes aleatórios codificados em Base64URL sem padding. Em configuração externa do Spring Boot, associe o UUID da aplicação ao SHA-256 hexadecimal do texto do segredo sob `plataforma.autenticacao.clientes-confidenciais`. Use TLS no ambiente publicado e limite tentativas de login no gateway antes de expor a API.

O access token dura até cinco minutos. A sessão expira em até 30 dias, com limite de sete dias sem renovação. A cada renovação, substitua o refresh token anterior pelo recebido. Se um refresh token já consumido for reapresentado no contexto correto, a plataforma revoga a família e exige novo login.

O frontend usa `POST /navegador/login`, `POST /navegador/renovar` e `POST /navegador/logout` sob o mesmo prefixo. Essas rotas exigem `X-Sessao-Navegador: 1` e devem ser acessadas pela mesma origem do frontend, sem CORS. Login e renovação retornam apenas o access token no JSON e entregam a credencial de renovação em um cookie `HttpOnly` e `SameSite=Strict`. O cookie é `Secure` por padrão; o perfil `local` permite HTTP no loopback. O contrato JSON das rotas originais permanece disponível aos consumidores da API.

## Testes

Execute `mvn test` para os testes que independem do PostgreSQL. Com as credenciais exportadas, execute `TESTE_AUTENTICACAO_POSTGRES=true mvn -Dtest=AutenticacaoPostgresTest test` para validar login, isolamento, renovação concorrente e logout no banco local do projeto. Esse teste cria registros próprios, remove-os no encerramento de cada caso e usa o schema já migrado. A sequência de auditoria pode avançar.
