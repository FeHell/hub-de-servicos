# Convenções do Hub de Serviços

## 1. Organização do projeto

- Consulte o [README da raiz](README.md) e os guias de [frontend](hub-de-servicos-front/README.md) e [backend](hub-de-servicos-backend/README.md) para configurar, iniciar e validar o projeto.
- Preserve o monólito modular e a organização por funcionalidade.
- Reutilize componentes e serviços existentes.
- Mantenha os padrões de nomenclatura, validação, tratamento de erros e autorização.
- Implemente funcionalidades no módulo correspondente e concentre infraestrutura comum nas estruturas compartilhadas existentes.
- Mantenha a lógica de negócio fora dos componentes de apresentação e dos controllers.
- Não introduza novas arquiteturas, padrões ou dependências sem justificativa técnica e aprovação quando houver impacto relevante.

## 2. Frontend

- Cadastre os rótulos de enums exibidos na interface em `hub-de-servicos-front/src/shared/traducoes/enums.ts`.
- Use `traduzirEnum` para exibir valores e `opcoesEnum` para montar opções de campos.
- Preserve os valores originais nas requisições, nos filtros e nas regras de negócio.
- Para permissões cadastradas em tempo de execução, forneça o nome amigável retornado pelo catálogo, quando disponível.
- Valores desconhecidos usam um rótulo genérico e geram um aviso no console de desenvolvimento.
- Reutilize componentes de `src/components`, o cliente HTTP de `src/services` e os tokens de `src/styles`.
- Preserve a acessibilidade, os estados de carregamento e a adaptação a telas pequenas.
- Mantenha as variáveis `VITE_` restritas a informações públicas.
- As rotas protegidas na interface complementam a autorização aplicada pelo backend. Nunca considere a proteção do frontend suficiente para garantir autorização.

## 3. Backend

- Classes anotadas com `@Entity` devem terminar com `Entity`.
- Use o nome da entidade nas consultas JPQL/HQL e mantenha explícitos os mapeamentos de tabela e coluna.
- Uma renomeação de classe Java não autoriza modificar tabelas, colunas, constraints, sequences ou contratos da API.
- Organize classes em:
  - `controller.<funcionalidade>`
  - `service.<funcionalidade>`
  - `repository.<funcionalidade>`
  - `entity.<funcionalidade>`
  - `dto.<funcionalidade>`

  Todos esses pacotes devem ficar sob `br.com.hubdeservicos`.

- Mantenha configurações, exceções, componentes compartilhados, bootstrap e provedores em seus pacotes próprios.
- Use a mesma organização nos testes da camada e mantenha as suítes transversais de persistência em `persistencia`.
- Aplique validação nas entradas e mantenha as transações nos services.
- Gerencie o schema pelo Liquibase em `src/main/resources/db/migration`.
- Mantenha o Hibernate com `ddl-auto: validate`.
- Preserve o histórico de changesets aplicados.
- Use o banco exclusivo `hub_de_servicos` no ambiente correspondente.
- Execute os testes PostgreSQL em uma instância isolada e migrada.
- Forneça credenciais, chaves e configurações de implantação fora do repositório.

## 4. Trabalho com o Codex

- Antes de editar, identifique os pontos de entrada, os componentes reutilizáveis, os contratos afetados e as instruções do módulo.
- Preserve alterações locais do usuário.
- Mantenha a implementação restrita ao escopo solicitado.
- Não exclua funcionalidades existentes sem aprovação.
- Não sobrescreva alterações que não pertençam à solicitação atual.
- Rode build e testes do módulo afetado.
- Em alterações de autenticação, persistência ou ambiente, confirme o destino antes de executar migrações ou testes que gravam dados.
- Registre no retorno os comandos executados, os resultados e as validações pendentes.
- Nunca afirme que um teste, build, migração ou validação foi executado com sucesso sem ter realizado e verificado sua execução.

---

# Fluxo obrigatório de desenvolvimento orientado por especificação

## 5. Regra geral

Toda solicitação que envolva implementação de funcionalidades, correções, alterações estruturais, refatorações ou modificações no código deverá seguir obrigatoriamente o fluxo de planejamento, aprovação e execução descrito neste documento.

**Não iniciar alterações de implementação antes da aprovação explícita do planejamento pelo usuário.**

Essa regra vale mesmo quando o usuário utiliza expressões como "implemente", "corrija", "ajuste", "crie" ou "modifique".

A solicitação inicial define o objetivo do trabalho, mas não representa autorização para começar a implementação.

Perguntas, explicações, análises, consultas e revisões que não exijam implementação podem ser respondidas normalmente, sem criar um planejamento.

Pequenos ajustes também devem seguir o fluxo quando envolverem alteração de código. O tamanho da tarefa não elimina a necessidade de aprovação.

## 6. Etapa 1 — Análise da solicitação

Ao receber uma solicitação de implementação:

- Compreender o objetivo e os requisitos.
- Analisar a estrutura atual do projeto.
- Identificar os módulos, arquivos, classes e componentes envolvidos.
- Identificar dependências, impactos e possíveis riscos.
- Consultar as instruções e documentações existentes.
- Verificar se existem implementações semelhantes que possam ser reutilizadas.
- Verificar se há alterações locais do usuário que precisam ser preservadas.
- Identificar dúvidas ou decisões que possam afetar a solução.

Não modificar arquivos de implementação nesta etapa.

Se houver dúvidas que impeçam um planejamento confiável, solicitar esclarecimentos ao usuário antes de finalizar os documentos.

A análise deverá utilizar o código existente como referência. Não presumir estruturas, classes, métodos ou comportamentos sem verificá-los no projeto.

## 7. Etapa 2 — Criação dos documentos

Criar uma pasta exclusiva para cada solicitação dentro de `.specs/`.

O nome da pasta deverá seguir obrigatoriamente o padrão:

`aaaa-mm-dd-hh-mm-breve-descricao`

Onde:

- `aaaa`: ano com quatro dígitos.
- Primeiro `mm`: mês com dois dígitos.
- `dd`: dia com dois dígitos.
- `hh`: hora com dois dígitos, formato de 24 horas.
- Segundo `mm`: minuto com dois dígitos.
- `breve-descricao`: descrição curta do objetivo da solicitação.

Exemplo:

```text
.specs/
└── 2026-11-24-21-15-padronizacao-estilo-codigo-java/
```

Regras de nomenclatura:

- Utilizar a data e a hora local no momento da criação dos documentos.
- Utilizar letras minúsculas na descrição.
- Não utilizar acentos ou caracteres especiais.
- Substituir espaços por hífens.
- Não utilizar segundos.
- Não reutilizar identificadores de solicitações anteriores.
- Manter todos os documentos da mesma solicitação dentro de uma única pasta.

Como o identificador possui precisão de minuto, verificar se a pasta já existe antes de criá-la. Se houver colisão, acrescentar um identificador sequencial ao final da descrição, sem modificar o padrão de data e hora.

Exemplo:

```text
.specs/
├── 2026-11-24-21-15-padronizacao-estilo-java/
└── 2026-11-24-21-15-padronizacao-estilo-java-02/
```

### 7.1. Documentos obrigatórios

Criar os seguintes documentos:

**`spec.md`**

Deve apresentar:

- Objetivo da solicitação.
- Requisitos funcionais e não funcionais.
- Regras de negócio.
- Escopo.
- Restrições.
- Critérios de aceitação.

**`plan.md`**

Deve apresentar:

- Estratégia técnica.
- Arquitetura envolvida.
- Componentes e arquivos afetados.
- Dependências.
- Impactos.
- Riscos.
- Decisões técnicas necessárias.
- Estratégia de implementação e validação.

**`tasks.md`**

Deve apresentar a sequência ordenada e detalhada de todas as atividades necessárias.

As tarefas deverão ser específicas, verificáveis e organizadas em uma sequência lógica.

Cada tarefa deverá começar com uma caixa de seleção vazia:

```markdown
- [ ] Descrição da atividade
```

Não marcar tarefas como concluídas durante o planejamento.

### 7.2. Documento principal para revisão

Além dos três documentos anteriores, criar um documento principal com o mesmo identificador da pasta:

`aaaa-mm-dd-hh-mm-breve-descricao.md`

Exemplo:

```text
.specs/
└── 2026-11-24-21-15-padronizacao-estilo-java/
    ├── 2026-11-24-21-15-padronizacao-estilo-java.md
    ├── spec.md
    ├── plan.md
    └── tasks.md
```

Esse documento deverá ser o ponto de entrada para a análise do usuário.

Deverá consolidar as informações necessárias para a revisão:

- Objetivo e especificação da solicitação.
- Escopo e regras de negócio.
- Estratégia técnica proposta.
- Arquivos e componentes afetados.
- Sequência de atividades.
- Riscos e impactos.
- Critérios de aceitação.
- Referências aos documentos complementares.

O documento principal não substitui os arquivos `spec.md`, `plan.md` e `tasks.md`. Ele consolida as informações e facilita a revisão.

Manter os quatro documentos sincronizados. Quando uma decisão ou requisito mudar, atualizar todos os documentos afetados.

## 8. Etapa 3 — Revisão e aprovação

Após criar os documentos:

- Apresentar um resumo da solução proposta.
- Informar os arquivos que serão criados, modificados ou removidos.
- Apresentar os riscos e impactos identificados.
- Informar as principais decisões técnicas.
- Informar a localização da pasta e do documento principal.
- Apresentar os documentos para conferência.

Interromper o trabalho e aguardar a conferência do usuário.

É expressamente proibido iniciar a implementação antes da autorização explícita.

A simples solicitação inicial de uma funcionalidade não representa autorização para implementar.

Pedidos de explicação, análise, esclarecimento ou revisão dos documentos também não representam autorização para executar.

## 9. Etapa 4 — Autorização

Somente iniciar a execução quando o usuário autorizar explicitamente o planejamento.

A autorização corresponde à versão dos documentos apresentada para revisão.

Se o usuário solicitar alterações no planejamento:

1. Atualizar os documentos afetados.
2. Conferir a consistência entre os quatro arquivos.
3. Apresentar novamente o planejamento.
4. Aguardar nova aprovação.

Não interpretar comentários, sugestões ou pedidos de esclarecimento como autorização.

A autorização deve ser inequívoca e permitir identificar que o usuário aprovou o início da implementação.

## 10. Etapa 5 — Execução controlada

Após a aprovação:

- Executar as atividades na ordem definida em `tasks.md`.
- Respeitar integralmente o escopo aprovado.
- Não executar atividades que não estejam previstas no planejamento.
- Marcar uma tarefa como concluída somente depois de realizar e verificar a atividade.
- Não marcar atividades parcialmente executadas como concluídas.
- Atualizar os documentos quando houver informações relevantes descobertas durante a execução.
- Preservar as alterações locais do usuário.
- Não realizar alterações não relacionadas à solicitação.

### 10.1. Alterações não previstas

Se surgir a necessidade de uma alteração não prevista, uma mudança de arquitetura ou uma ampliação de escopo:

1. Interromper a execução da atividade afetada.
2. Registrar o motivo e os impactos identificados.
3. Atualizar `spec.md`, `plan.md`, `tasks.md` e o documento principal, conforme necessário.
4. Apresentar as mudanças ao usuário.
5. Aguardar nova autorização antes de continuar a parte afetada.

Não realizar alterações adicionais por iniciativa própria apenas porque parecem convenientes.

Correções estritamente necessárias para concluir uma tarefa aprovada poderão ser realizadas quando não alterarem seu escopo, comportamento esperado ou riscos. Em caso de dúvida, interromper e solicitar aprovação.

## 11. Etapa 6 — Validação e encerramento

Ao concluir:

- Executar os testes pertinentes.
- Verificar os critérios de aceitação.
- Conferir se todas as tarefas foram executadas.
- Marcar somente as atividades efetivamente concluídas.
- Registrar testes que não puderam ser executados e suas justificativas.
- Informar eventuais pendências, limitações e divergências em relação ao planejamento aprovado.
- Atualizar os documentos para refletir o resultado real da implementação.

Apresentar um resumo final contendo:

- O que foi implementado.
- Principais arquivos criados ou modificados.
- Testes e comandos executados.
- Resultados obtidos.
- Validações pendentes.
- Problemas encontrados.
- Divergências em relação ao planejamento aprovado.

Nunca declarar uma implementação concluída se houver atividades pendentes ou validações obrigatórias não realizadas.

## 12. Persistência e rastreabilidade

- Manter os documentos de cada solicitação dentro de sua respectiva pasta em `.specs/`.
- Não sobrescrever planejamentos anteriores.
- Preservar o histórico das atividades e decisões.
- Não apagar documentos de solicitações anteriores.
- Ao retomar uma implementação, consultar os documentos e identificar o estado real do trabalho antes de continuar.
- Verificar o código existente e o estado dos arquivos antes de presumir que uma tarefa foi concluída.
- Nunca presumir que uma tarefa foi concluída apenas porque foi mencionada em uma conversa anterior.
- Manter o documento principal e os documentos complementares atualizados.

## 13. Princípio fundamental

O usuário é responsável por aprovar o que será implementado.

O agente é responsável por analisar, planejar, documentar, executar o que foi aprovado e comprovar o resultado.

**Planejar primeiro. Aguardar aprovação. Executar depois.**

---

# Convenções de nomenclatura e estrutura dos objetos Java

## 14. Classificação de Beans, DTOs e Value Objects

Toda classe que represente dados deverá ser classificada de acordo com sua responsabilidade e finalidade dentro da aplicação.

O projeto deverá adotar três categorias distintas:

- **Bean:** objeto de dados utilizado internamente pela aplicação.
- **DTO (Data Transfer Object):** objeto destinado ao transporte de dados entre limites da aplicação, camadas, APIs ou sistemas.
- **VO (Value Object):** objeto que representa um conceito ou valor do domínio da aplicação.

Não classificar uma classe apenas pela presença de atributos, getters, setters ou construtores.

A classificação deverá considerar sua responsabilidade real, seus usos e sua relação com o domínio.

### 14.1. Convenção obrigatória de nomenclatura

Os nomes das classes deverão utilizar os seguintes sufixos:

| Categoria | Sufixo | Exemplo |
|---|---|---|
| Bean | `Bean` | `UsuarioBean` |
| DTO | `DTO` | `UsuarioCadastroDTO` |
| Value Object | `VO` | `DocumentoVO` |

Exemplos:

```java
UsuarioBean
UsuarioCadastroDTO
UsuarioAtualizacaoDTO
UsuarioRespostaDTO
DocumentoVO
EnderecoVO
DinheiroVO
PagarmeRecebedorBean
PagarmeRecebedorDTO
```

A nomenclatura deverá ser aplicada consistentemente em todo o projeto, incluindo imports, atributos, parâmetros, retornos, instanciações, conversores, mapeadores e testes.

Não utilizar o sufixo `Bean` como alternativa genérica para classes cuja responsabilidade seja de DTO ou VO.

### 14.2. Bean

Beans são objetos utilizados para organizar e transportar dados internamente na aplicação, sem necessariamente representar um conceito de domínio com identidade ou regras próprias.

Regras:

- Utilizar o sufixo `Bean`.
- Manter os atributos privados.
- Preferir construtor sem argumentos para instanciação.
- Utilizar getters e setters explícitos para leitura e alteração dos atributos.
- Evitar construtores com muitos parâmetros.
- Não adicionar regras de negócio complexas sem justificativa arquitetural.
- Utilizar variáveis intermediárias na preparação e composição dos objetos.
- Preservar as exigências de frameworks e mecanismos de serialização.

Exemplo:

```java
public class UsuarioBean {

    private UUID id;
    private String nome;
    private String email;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
```

### 14.3. DTO — Data Transfer Object

DTOs são objetos destinados ao transporte de informações entre diferentes limites da aplicação, camadas, APIs ou sistemas.

Regras:

- Utilizar o sufixo `DTO`.
- Identificar claramente sua finalidade, principalmente quando representar entrada ou saída de uma operação.
- Preferir nomes descritivos, como `UsuarioCadastroDTO`, `UsuarioAtualizacaoDTO` e `UsuarioRespostaDTO`.
- Evitar concentrar regras de negócio nos DTOs.
- Utilizar getters e setters quando forem objetos mutáveis.
- Avaliar mecanismos de validação apropriados para dados recebidos.
- Não expor automaticamente entidades internas ou estruturas de persistência nos contratos externos.
- Preservar compatibilidade de serialização e desserialização.
- Avaliar construtores específicos ou `record` quando forem compatíveis com o uso e com os frameworks.

Exemplo:

```java
public class UsuarioCadastroDTO {

    private String nome;
    private String email;
    private String senha;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
```

### 14.4. VO — Value Object

Value Objects representam conceitos ou valores do domínio da aplicação.

Exemplos:

- `DocumentoVO`
- `EnderecoVO`
- `DinheiroVO`
- `PeriodoVO`

Regras:

- Utilizar o sufixo `VO`.
- Priorizar a imutabilidade.
- Avaliar a utilização de `record` quando compatível com a arquitetura e as necessidades do objeto.
- Concentrar no próprio VO as validações e regras que garantam a validade do valor representado, quando apropriado.
- Não utilizar setters como padrão.
- Preferir a criação de objetos já válidos, evitando estados intermediários inconsistentes.
- Considerar a igualdade entre objetos com base em seus valores, e não em identificadores próprios.
- Preservar a consistência entre `equals` e `hashCode`.
- Evitar transformar VOs em simples estruturas de dados mutáveis.

Exemplo ilustrativo:

```java
public final class DocumentoVO {

    private final String numero;
    private final String tipo;

    public DocumentoVO(String numero, String tipo) {
        this.numero = validarNumero(numero);
        this.tipo = validarTipo(tipo);
    }

    public String getNumero() {
        return numero;
    }

    public String getTipo() {
        return tipo;
    }
}
```

O exemplo não determina que todos os VOs devam utilizar exatamente essa estrutura. A implementação deverá respeitar as necessidades de cada conceito de domínio.

### 14.5. Uso de construtores

A utilização de construtores deverá respeitar a responsabilidade de cada categoria.

- Beans: preferir construtor sem argumentos e preenchimento por setters.
- DTOs: preferir construtor sem argumentos e getters/setters quando a serialização e o framework permitirem. Construtores específicos podem ser utilizados quando houver justificativa.
- VOs: preferir construtores que garantam a validade e a consistência do objeto no momento da criação.

Não eliminar construtores de forma indiscriminada.

A existência de construtores especiais deverá ser justificada pela necessidade técnica, pelo domínio ou pelas exigências dos frameworks.

### 14.6. Classificação antes de renomear

Antes de modificar qualquer classe existente:

1. Analisar sua finalidade e os locais onde é utilizada.
2. Identificar se representa um Bean, DTO ou VO.
3. Verificar se já existe outra classe com responsabilidade semelhante.
4. Avaliar impactos em APIs, serialização, persistência e integrações.
5. Justificar a classificação escolhida.
6. Apresentar casos ambíguos para aprovação do usuário.

Não renomear entidades JPA, serviços, repositórios, controllers ou outras categorias de classes apenas para adequá-las a essa convenção.

### 14.7. Preservação de compatibilidade

A padronização não deverá alterar o comportamento funcional do sistema.

Antes de modificar nomes ou estruturas:

- Verificar dependências de serialização.
- Verificar contratos de APIs e integrações externas.
- Verificar mecanismos de injeção de dependência.
- Verificar frameworks que dependam de construtores específicos.
- Verificar referências em testes e configurações.
- Identificar possíveis impactos de reflexão, mapeamento e persistência.
- Preservar os nomes de propriedades JSON, salvo aprovação específica.

Qualquer alteração que possa modificar contratos ou comportamento deverá ser apresentada no planejamento e depender de aprovação explícita.

### 14.8. Exceções e decisões já aprovadas

- Preservar a configuração Spring `ConfiguracaoAutenticacao`.
- Preservar as identidades JPA e os nomes terminados em `Entity`.
- Preservar construtores exigidos por injeção de dependências.
- Preservar a imutabilidade e a construção controlada dos VOs quando necessárias.
- Não adicionar setters a objetos que tenham sido definidos como imutáveis por decisão técnica aprovada, incluindo o principal de autenticação, chaves de mapas e snapshots de credenciais.

O planejamento `2026-11-24-21-15-padronizacao-estilo-codigo-java` foi autorizado pelo usuário. Seus lotes e exceções aprovados orientam a execução dessa padronização. As convenções permanentes deste documento devem ser respeitadas nas novas implementações e alterações futuras, sem desfazer decisões específicas já aprovadas.

### 14.9. Princípio fundamental

A classificação deverá seguir a responsabilidade do objeto:

- Bean organiza dados para uso interno.
- DTO transporta dados entre limites da aplicação.
- VO representa um valor ou conceito do domínio.

**Não sacrificar a semântica, a integridade ou a arquitetura do projeto apenas para padronizar nomes ou eliminar construtores.**

---

# Padrão obrigatório de legibilidade, organização e manutenção do código

## 15. Princípio geral

Todo código novo ou alterado deverá priorizar a legibilidade, a facilidade de compreensão e a depuração humana.

O código deverá ser escrito pensando não apenas em seu funcionamento, mas também na facilidade com que outro desenvolvedor conseguirá compreender sua finalidade, acompanhar seu fluxo de execução e identificar possíveis problemas.

**A prioridade é produzir código claro, organizado e fácil de manter, e não código excessivamente compacto.**

Essas orientações deverão ser seguidas em todas as implementações, correções, refatorações e ajustes realizados no projeto.

## 16. Organização e separação das operações

Evitar concentrar várias operações em uma única expressão ou sequência de comandos compactada.

Preferir:

- Separar etapas importantes em blocos visualmente organizados.
- Utilizar variáveis intermediárias quando facilitarem a compreensão.
- Dividir operações complexas em métodos auxiliares com responsabilidades claras.
- Manter condições e validações em estruturas fáceis de identificar.
- Separar a preparação dos dados da execução das operações.
- Utilizar espaçamento adequado entre etapas com responsabilidades diferentes.
- Dar nomes descritivos às variáveis e métodos, evitando abreviações desnecessárias.

Evitar encadeamentos extensos de chamadas quando dificultarem o acompanhamento dos valores utilizados.

Não criar métodos auxiliares desnecessários apenas para fragmentar o código. Cada método deverá possuir uma responsabilidade compreensível e contribuir efetivamente para a organização.

## 17. Instanciação e composição de objetos

Evitar instanciações diretamente no retorno de métodos.

Evitar:

```java
return new UsuarioBean(
    nome,
    email,
    new EnderecoVO(
        logradouro,
        numero,
        cidade
    )
);
```

Preferir:

```java
EnderecoVO endereco = prepararEndereco(
    logradouro,
    numero,
    cidade
);

UsuarioBean usuario = new UsuarioBean();

usuario.setNome(nome);
usuario.setEmail(email);
usuario.setEndereco(endereco);

return usuario;
```

Regras:

- Não instanciar objetos diretamente em instruções `return`.
- Não instanciar objetos dentro dos parâmetros de outros construtores quando isso dificultar a leitura.
- Preparar objetos compostos em etapas independentes.
- Utilizar variáveis intermediárias para deixar explícitos os valores utilizados.
- Evitar expressões extensas que misturem transformação, validação e instanciação.
- Manter o retorno dos métodos visualmente simples.

Essa orientação também se aplica a DTOs, Beans, VOs e demais objetos utilizados na aplicação.

A utilização de construtores continua permitida quando apropriada. O objetivo é evitar a composição excessiva de instanciações em uma única expressão, e não proibir construtores.

Não decompor operações triviais sem benefício real de legibilidade.

## 18. Comentários explicativos

Adicionar comentários sempre que contribuírem para a compreensão da implementação.

Os comentários deverão explicar principalmente:

- A finalidade de uma etapa importante.
- O motivo de determinada regra ou condição existir.
- Regras de negócio que não sejam evidentes pelo código.
- Decisões técnicas que possam gerar dúvidas.
- Tratamentos específicos para integrações externas.
- Comportamentos excepcionais ou particularidades de determinado fluxo.

Evitar comentários redundantes que apenas descrevam literalmente a instrução seguinte.

Exemplo inadequado:

```java
// Soma o valor ao total.
total = total.add(valor);
```

Exemplo adequado:

```java
// Considera somente os valores efetivamente aprovados pelo provedor.
totalAprovado = totalAprovado.add(valor);
```

Os comentários devem permanecer atualizados quando o código for modificado.

Não adicionar comentários apenas para aumentar a quantidade de documentação. O objetivo é explicar intenção, contexto e decisões relevantes.

## 19. Clareza em condições e fluxos de execução

Evitar condições excessivamente extensas e expressões booleanas difíceis de interpretar.

Quando necessário:

- Dividir condições complexas em variáveis booleanas com nomes descritivos.
- Separar validações independentes.
- Utilizar estruturas condicionais que facilitem acompanhar cada possibilidade.
- Evitar efeitos colaterais dentro de condições.
- Evitar modificar variáveis de controle de maneira que o resultado dependa da ordem de execução de um loop.
- Priorizar fluxos previsíveis e fáceis de depurar.

Exemplo:

Evitar:

```java
if (pagamentoOnline && plataformaHabilitada && aceitaCartao && autorizado && !bloqueado) {
    // ...
}
```

Quando isso facilitar a análise, preferir:

```java
boolean pagamentoOnlineHabilitado = pagamentoOnline && plataformaHabilitada;

boolean cartaoDisponivel = pagamentoOnlineHabilitado
        && aceitaCartao
        && autorizado
        && !bloqueado;

if (cartaoDisponivel) {
    // ...
}
```

Não criar variáveis intermediárias para condições triviais quando isso não melhorar a compreensão.

Preservar a avaliação preguiçosa de lambdas, expressões condicionais e operadores de curto-circuito.

Não alterar a ordem de avaliação de operações quando houver efeitos colaterais ou dependências entre elas.

## 20. Métodos e responsabilidades

Cada método deverá possuir uma responsabilidade clara.

Regras:

- Evitar métodos excessivamente longos.
- Dividir processos complexos em etapas nomeadas.
- Utilizar nomes que indiquem claramente a ação realizada.
- Evitar métodos que misturem preparação de dados, validação, persistência e comunicação externa sem uma justificativa arquitetural.
- Preservar as responsabilidades das camadas existentes.
- Evitar criar abstrações desnecessárias.

A divisão em métodos auxiliares deverá facilitar o entendimento do processo como um todo.

Não dividir métodos apenas por quantidade de linhas. Considerar a complexidade, a responsabilidade e a clareza do fluxo.

## 21. Variáveis e nomenclatura

Utilizar nomes que permitam compreender a finalidade dos dados sem precisar acompanhar toda a implementação.

Evitar:

```java
var d = dados.get();
var v = calcular(d);
var r = executar(v);
```

Preferir:

```java
DadosBean dados = obterDados();

BigDecimal valorCalculado = calcularValor(dados);

ResultadoBean resultado = executarOperacao(valorCalculado);
```

Utilizar `var` somente quando o tipo for evidente no contexto e sua utilização não prejudicar a compreensão.

Priorizar nomes completos e descritivos, respeitando as convenções já adotadas pelo projeto.

Não utilizar nomes genéricos como `obj`, `dados2`, `temp`, `aux` ou abreviações sem significado claro, exceto quando forem variáveis locais de uso trivial e inequívoco.

## 22. Padrão de retorno dos métodos

Sempre que possível, manter um único ponto de retorno por método, desde que isso não prejudique a legibilidade ou produza estruturas condicionais desnecessariamente complexas.

Preferir preparar os valores necessários, organizá-los em variáveis intermediárias e realizar o retorno de maneira explícita.

Não criar retornos intermediários ou estruturas artificiais apenas para cumprir essa regra.

Retornos antecipados podem ser utilizados quando tornarem validações, condições de guarda ou tratamentos excepcionais mais claros. A legibilidade deve prevalecer sobre a aplicação mecânica da regra de retorno único.

## 23. Preservação do comportamento

As melhorias de legibilidade não devem modificar o funcionamento do sistema.

Ao realizar qualquer ajuste:

- Preservar as regras de negócio.
- Preservar as validações.
- Preservar a ordem das operações quando ela tiver relevância.
- Preservar os tratamentos de exceção.
- Preservar os contratos de métodos e APIs, salvo quando houver aprovação específica.
- Preservar os efeitos colaterais existentes.
- Não remover comportamentos apenas por parecerem desnecessários.
- Preservar a avaliação preguiçosa de lambdas e operadores de curto-circuito.
- Preservar tipos monetários, formatos de datas, criptografia e regras de autorização.
- Preservar as anotações de segurança e os mecanismos de proteção de informações sensíveis.
- Preservar os métodos `toString` existentes que ocultem segredos.

Renomear uma classe Java não autoriza alterar propriedades JSON, schemas publicados, validações, dados persistidos ou contratos externos.

Se uma melhoria de legibilidade exigir uma alteração funcional, apresentar a necessidade e aguardar aprovação.

## 24. Aplicação contínua do padrão

Estas regras não se limitam a uma migração ou tarefa específica.

Devem ser aplicadas automaticamente:

- Na criação de novas classes.
- Na implementação de novos métodos.
- Na criação de Beans, DTOs e VOs.
- Na alteração de métodos existentes.
- Na correção de bugs.
- Na refatoração.
- Na implementação de integrações.
- Na criação de testes.
- Em qualquer código novo produzido pelo agente.

Ao modificar um método existente, avaliar também se a região diretamente relacionada à alteração possui problemas evidentes de legibilidade que possam ser corrigidos sem ampliar indevidamente o escopo.

Não realizar refatorações gerais e não relacionadas à solicitação apenas para uniformizar arquivos inteiros.

Não modificar código fora do escopo aprovado somente para aplicar essas convenções retroativamente.

## 25. Critério de qualidade obrigatório

Antes de considerar uma implementação concluída, verificar:

- O fluxo pode ser compreendido sem precisar decifrar expressões extensas?
- As etapas importantes estão visualmente separadas?
- Os objetos são preparados de maneira clara?
- Os nomes das variáveis e métodos são descritivos?
- Os comentários explicam decisões relevantes?
- As condições são fáceis de analisar?
- A implementação pode ser depurada sem dificuldade desnecessária?
- As responsabilidades dos métodos estão claras?
- O comportamento original foi preservado?
- Os testes pertinentes foram executados?
- As convenções de Bean, DTO e VO foram respeitadas?
- Os contratos e integrações existentes foram preservados?

**Princípio fundamental: código não deve ser escrito apenas para funcionar. Deve ser escrito para que outro desenvolvedor consiga entender, analisar, depurar e manter com facilidade.**