# Hub de Serviços

Base administrativa independente com autenticação, controle de acesso e auditoria. O frontend Vue consome a API REST do backend Spring Boot na mesma origem.

| Diretório | Conteúdo |
| --- | --- |
| [hub-de-servicos-front](hub-de-servicos-front/README.md) | Interface Vue 3, TypeScript, Pinia, Vue Router, Vite e Vitest |
| [hub-de-servicos-backend](hub-de-servicos-backend/README.md) | Monólito modular em Java 21, Spring Boot 3.2.4, Spring Security, JPA/Hibernate, Liquibase e PostgreSQL |
| [hub-de-servicos-android](hub-de-servicos-android/README.md) | App Android nativo com cadastro, login e sessão |
| [AGENTS.md](AGENTS.md) | Convenções do projeto e orientações para futuras implementações com o Codex |

## Executar em desenvolvimento

Use Java 21, Maven, PostgreSQL e Node.js `^20.19.0` ou `>=22.12.0`. O backend requer o banco exclusivo `hub_de_servicos`. Consulte o [guia do backend](hub-de-servicos-backend/README.md#desenvolvimento-local) para criar o banco, definir `DATABASE_USERNAME` e `DATABASE_PASSWORD`, aplicar as migrações e provisionar a conta inicial.

Com o UUID da aplicação administrativa definido em `HUB_ADMINISTRACAO_APLICACAOID`, inicie o backend a partir da raiz:

```sh
cd hub-de-servicos-backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Em outro terminal, na raiz do projeto:

```sh
cd hub-de-servicos-front
npm ci
npm run dev
```

Abra [a interface local](http://127.0.0.1:5173) e entre com a conta provisionada para `hub-de-servicos-admin`. O Vite encaminha `/api` para o backend em `127.0.0.1:8080`. A [documentação da API](http://127.0.0.1:8080/swagger-ui/index.html) fica no Swagger UI.

## Homologação e produção

Use o perfil Spring `homologacao` ou `producao` e forneça a conexão ao banco `hub_de_servicos` da respectiva instância, a identidade administrativa e a configuração de assinatura JWT. Gere a interface com `npm run build -- --mode homologacao` ou `npm run build`. Sirva o conteúdo de `dist/` com HTTPS, fallback para `index.html` e proxy de `/api` para o backend no mesmo domínio. Os nomes das variáveis estão nos guias do [backend](hub-de-servicos-backend/README.md#ambientes) e do [frontend](hub-de-servicos-front/README.md#ambientes).

## Builds e testes

```sh
(cd hub-de-servicos-front && npm ci && npm run build && npm test && npm run lint)
(cd hub-de-servicos-backend && mvn clean verify)
```

O frontend gera `dist/`. O backend gera `target/hub-de-servicos-backend-1.0.0-SNAPSHOT.jar`. As suítes de integração com PostgreSQL são habilitadas explicitamente em um banco isolado e migrado. Veja [como executá-las](hub-de-servicos-backend/README.md#testes).

## Escopo da base

A infraestrutura reaproveitável inclui autenticação, sessões, autorização por aplicação, usuários, perfis, permissões, auditoria, componentes Vue, tema e tradução de enums. As telas administrativas de usuários e o Dashboard Usuários continuam disponíveis para contas autorizadas. A exposição dessas telas, a administração de outras aplicações e o uso do catálogo de permissões globais no produto dependem da definição funcional do Hub.
