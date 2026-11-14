# Frontend do Hub de Serviços

Interface administrativa em Vue 3 e TypeScript. Após o login, Vue Router e Pinia coordenam as rotas autenticadas e a sessão.

## Executar

Use Node.js `^20.19.0` ou `>=22.12.0`. Inicie o [backend no perfil local](../hub-de-servicos-backend/README.md#desenvolvimento-local) e, neste diretório, execute:

```sh
npm ci
npm run dev
```

Abra [o frontend local](http://127.0.0.1:5173) e entre com a conta cadastrada para `hub-de-servicos-admin`.

## Ambientes

| Ambiente        | Arquivo de configuração local | Comando                               |
| --------------- | ----------------------------- | ------------------------------------- |
| Desenvolvimento | `.env.local`                  | `npm run dev`                         |
| Homologação     | `.env.homologacao.local`      | `npm run build -- --mode homologacao` |
| Produção        | `.env.production.local`       | `npm run build`                       |

Use `.env.example` como referência. `VITE_APLICACAO_NOME` define o nome exibido e tem como padrão `Hub de Serviços`. `VITE_APLICACAO_SLUG` tem como padrão `hub-de-servicos-admin`. Use outro slug somente para uma aplicação cadastrada no backend. `BACKEND_URL` configura o proxy de desenvolvimento e o padrão é `http://127.0.0.1:8080`. As variáveis `VITE_` são incluídas no build e recebem somente informações públicas. Na publicação, encaminhe `/api` ao backend na mesma origem HTTPS.

## Organização

- `src/app` contém configuração, rotas e o layout autenticado.
- `src/modules/autenticacao` contém a página de login, os contratos, o serviço da API e o estado da sessão.
- `src/modules/inicio` contém a tela inicial autenticada.
- `src/components` reúne os botões, campos, ícones e a navegação reutilizáveis.
- `src/styles/tokens` centraliza tipografia, espaçamentos, raios, transições e o breakpoint.
- `src/styles/themes` define as cores do tema.
- `src/services` concentra a comunicação HTTP e a tradução de erros.

Vue Router faz o carregamento das páginas por rota e protege a área autenticada. Pinia coordena a identidade, as permissões, a renovação e o encerramento da sessão. Componentes visuais chamam ações da store. As requisições ficam nos serviços.

## Sessão e navegação

O backend mantém a credencial de renovação em um cookie `HttpOnly` com `SameSite=Strict`, restrito às rotas de sessão da aplicação. O frontend não grava tokens no armazenamento do navegador. Ao recarregar, verifica a sessão no backend antes de abrir a rota; um novo login só é necessário se não houver sessão válida. Na primeira execução após esta atualização, entre novamente para receber o cookie.

A renovação acontece antes do vencimento do access token. As chamadas são agrupadas na aba e coordenadas entre abas quando o navegador oferece Web Locks. Sair revoga a sessão e remove o cookie. Se o servidor não confirmar o encerramento, a tela informa a falha e permite repetir a ação.

No desktop, o menu recolhido expande ao passar o mouse, ao receber foco pelo teclado ou ao clicar no ícone do cabeçalho. Um clique no conteúdo ou em um item do menu, inclusive o item da página atual, recolhe o menu. Em telas pequenas e dispositivos de toque, o ícone abre o menu sobre o conteúdo. O fundo, o botão de fechar e a tecla Escape encerram o menu. O foco fica dentro do menu enquanto o diálogo móvel está aberto.

A interface respeita a preferência por redução de movimento. Os campos têm rótulos, erros associados e foco visível. Enter envia o formulário, e o botão fica desabilitado durante o envio.

As capturas da interface herdada registram o layout original em [desktop](docs/imagens/login-desktop.jpg) e [mobile](docs/imagens/login-mobile.jpg).

## Verificar

```sh
npm run typecheck
npm test
npm run lint
npm run format:check
npm run build
```

O build gera `dist/`. Na publicação, configure o servidor para retornar `index.html` nas rotas da aplicação e encaminhar `/api` ao backend no mesmo domínio, com HTTPS. O proxy de desenvolvimento do Vite funciona no servidor local.

Os testes automatizados usam respostas controladas da API e não acessam o PostgreSQL. Eles verificam validação e preservação dos campos, contexto de aplicação, renovação simultânea, logout durante a rotação, erros HTTP e navegação por teclado no menu. Para conferir a integração real, inicie o backend com sua configuração normal, abra a interface, entre com sua conta e use **Sair da conta**. Credenciais de teste ou respostas fictícias não fazem parte da aplicação entregue.

## Padrões de manutenção

Adicione funcionalidades em seus módulos e reutilize os componentes existentes. Cadastre rótulos de enums em `src/shared/traducoes/enums.ts`. Use `traduzirEnum` para exibição, `opcoesEnum` para opções e mantenha os valores originais nos filtros e na API. Passe o nome do catálogo quando disponível para permissões dinâmicas. Centralize novos padrões visuais nos tokens. Preserve a tipagem das requisições, o comportamento de teclado, os estados de carregamento e a adaptação a telas pequenas. Novas rotas devem validar o acesso e usar a autorização aplicada pelo backend.

Páginas de pesquisa usam o sufixo `Pesquisa` e páginas de criação/edição usam `Edicao`, tanto no nome do componente quanto no arquivo. Exemplos: `UsuariosPesquisa.vue` e `UsuarioEdicao.vue`.
