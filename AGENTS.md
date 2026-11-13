# Convenções do projeto

## Frontend

- Cadastre os rótulos de enums exibidos na interface em `base-gerenciamento-plataforma-front/src/shared/traducoes/enums.ts`.
- Use `traduzirEnum` para exibir valores e `opcoesEnum` para montar opções de campos. Preserve os valores originais nas requisições, nos filtros e nas regras de negócio.
- Para permissões cadastradas em tempo de execução, forneça o nome amigável retornado pelo catálogo, quando disponível. Valores desconhecidos usam um rótulo genérico e geram um aviso no console de desenvolvimento.

## Backend

- Classes anotadas com `@Entity` devem terminar com `Entity`. Use o nome da entidade nas consultas JPQL/HQL e mantenha explícitos os mapeamentos de tabela e coluna.
- Uma renomeação de classe Java preserva tabelas, colunas, constraints, sequences e contratos da API.
