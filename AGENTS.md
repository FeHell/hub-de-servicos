# Convenções do Hub de Serviços

## Organização

- Consulte o [README da raiz](README.md) e os guias de [frontend](hub-de-servicos-front/README.md) e [backend](hub-de-servicos-backend/README.md) para configurar, iniciar e validar o projeto.
- Preserve o monólito modular e a organização por funcionalidade. Reutilize componentes e serviços existentes e mantenha os padrões de nomenclatura, validação, tratamento de erros e autorização.
- Implemente funcionalidades no módulo correspondente e concentre infraestrutura comum nas estruturas compartilhadas existentes. Mantenha a lógica de negócio fora dos componentes de apresentação e dos controllers.

## Frontend

- Cadastre os rótulos de enums exibidos na interface em `hub-de-servicos-front/src/shared/traducoes/enums.ts`.
- Use `traduzirEnum` para exibir valores e `opcoesEnum` para montar opções de campos. Preserve os valores originais nas requisições, nos filtros e nas regras de negócio.
- Para permissões cadastradas em tempo de execução, forneça o nome amigável retornado pelo catálogo, quando disponível. Valores desconhecidos usam um rótulo genérico e geram um aviso no console de desenvolvimento.
- Reutilize componentes de `src/components`, o cliente HTTP de `src/services` e os tokens de `src/styles`. Preserve a acessibilidade, os estados de carregamento e a adaptação a telas pequenas.
- Mantenha as variáveis `VITE_` restritas a informações públicas. As rotas protegidas na interface complementam a autorização aplicada pelo backend.

## Backend

- Classes anotadas com `@Entity` devem terminar com `Entity`. Use o nome da entidade nas consultas JPQL/HQL e mantenha explícitos os mapeamentos de tabela e coluna.
- Uma renomeação de classe Java preserva tabelas, colunas, constraints, sequences e contratos da API.
- Organize classes em `controller.<funcionalidade>`, `service.<funcionalidade>`, `repository.<funcionalidade>`, `entity.<funcionalidade>` e `dto.<funcionalidade>` sob `br.com.hubdeservicos`. Mantenha configurações, exceções, componentes compartilhados, bootstrap e provedores em seus pacotes próprios. Use a mesma organização nos testes da camada e mantenha as suítes transversais de persistência em `persistencia`.
- Aplique validação nas entradas e mantenha as transações nos services.
- Gerencie o schema pelo Liquibase em `src/main/resources/db/migration` e mantenha o Hibernate com `ddl-auto: validate`. Preserve o histórico de changesets aplicados.
- Use o banco exclusivo `hub_de_servicos` no ambiente correspondente. Execute os testes PostgreSQL em uma instância isolada e migrada. Forneça credenciais, chaves e configurações de implantação fora do repositório.

## Trabalho com o Codex

- Antes de editar, identifique os pontos de entrada, os componentes reutilizáveis, os contratos afetados e as instruções do módulo.
- Preserve alterações locais do usuário. Mantenha a implementação restrita ao escopo solicitado e peça aprovação antes de excluir funcionalidades herdadas.
- Rode build e testes do módulo afetado. Em alterações de autenticação, persistência ou ambiente, confirme o destino antes de executar migrações ou testes que gravam dados. Registre no retorno os comandos, resultados e validações pendentes.
