# Conecta Cheff para Android

App nativo em Kotlin. Esta primeira versão permite criar conta, entrar com e-mail e senha, consultar a conta conectada, restaurar a sessão após reabrir o app e sair. O cadastro confirma a senha localmente e chama o login existente para abrir a Home. Se o cadastro for concluído e o login automático falhar, o app volta para o login com o e-mail preenchido.

O app usa os endpoints de cadastro, login, renovação, identidade e logout em `/api/v1/app/autenticacao`. O access token fica em memória. Somente o refresh token é persistido, cifrado com uma chave do Android Keystore. As operações de sessão são serializadas para evitar rotações concorrentes no mesmo processo.

## Rodar no Android Studio

1. Inicie o [backend em desenvolvimento](../hub-de-servicos-backend/README.md#desenvolvimento-local) na porta 8080. O Liquibase deve ter aplicado o changeset `2-aplicacao-app-publica`. Confira as rotas móveis no [Swagger local](http://127.0.0.1:8080/swagger-ui/index.html).
2. No Android Studio, selecione **Open** e abra o diretório `hub-de-servicos-android`. Use JDK 17 para o toolchain e instale o Android SDK 35.
3. Selecione um emulador, por exemplo `Pixel_8_Pro_API_35`, e execute a variante `debug`. Ela usa `http://10.0.2.2:8080`, o endereço do Mac acessível pelo emulador.
4. Crie uma conta com um e-mail de teste e senha com pelo menos 12 caracteres e até 72 bytes em UTF-8. Confirme a senha e verifique que a Home abre após o cadastro. Force a parada do app, abra de novo e confira a saudação com o e-mail na Home. Toque em **Sair** e reabra para conferir que voltou ao login.

Para um aparelho conectado por USB com depuração habilitada, use `adb reverse tcp:8080 tcp:8080` e gere a variante debug com `./gradlew assembleDebug -PhubApiUrl=http://localhost:8080`. Instale o APK com `adb install -r app/build/outputs/apk/debug/app-debug.apk`. O HTTP local fica limitado à variante de debug e aos endereços de desenvolvimento. Configure `-PhubApiUrl=https://...` para um backend HTTPS.

## Build e testes

```sh
./gradlew assembleDebug testDebugUnitTest
```

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`. Os testes JVM cobrem as chamadas HTTP e a restauração, rotação e revogação de sessão. O teste real de integração é opcional. Defina `HUB_ANDROID_API_TESTE` com a origem de uma instância dedicada a testes antes de executar `testDebugUnitTest`. Ele cria uma conta de teste e valida o fluxo completo com o backend.

A Home oferece seleção independente de Empresa e Trabalhador e mostra o município quando a localização aproximada é autorizada. O app identifica município e UF com o geocoder do Android e envia apenas esses nomes ao backend, que resolve o código do IBGE e o horário da observação. É possível atualizar a cidade na Home, e o acesso ao app continua disponível sem permissão. Use o backend com as migrations de preferências cadastrais aplicadas para testar a persistência dos papéis, da remuneração e do município.

Na Home, o papel Trabalhador habilita **Conta que vou receber**. O papel Empresa habilita **Cartão de crédito**. Com ambos os papéis, as duas opções ficam disponíveis. A navegação consulta os papéis efetivos antes de abrir as telas financeiras. O usuário escolhe explicitamente pessoa física ou jurídica. PF informa os dados da pessoa que receberá. PJ informa os dados da empresa e de um representante, com declarações explícitas de representação legal e qualificação no QSA. As duas modalidades usam os mesmos campos bancários.

**Salvar rascunho** persiste os campos preenchidos no Hub e permite retomar o formulário. **Verificar dados da conta** consulta a validação do backend e mostra campos pendentes. O tipo do telefone aparece com as opções **Celular** e **Telefone fixo** em PF, PJ e representante. O telefone de Meu Cadastro continua sendo uma sugestão quando o formulário PJ é aberto sem telefone da empresa.

No fluxo PF, **Enviar cadastro à Pagar.me** confirma o envio pelo backend depois da validação. **Consultar andamento** recupera o resultado se o envio ficou pendente e acompanha a análise quando o recebedor já existe. A confirmação de criação é distinta do estado **ativa** informado pela Pagar.me. O ambiente exibido vem do backend. No fluxo PJ, a validação continua servindo à prévia local.

Em **Dados bancários**, a pesquisa usa a lista local de instituições com código de três dígitos do [Banco Central do Brasil — Participantes do STR](https://dadosabertos.bcb.gov.br/dataset/lista-de-participantes-do-str), sob a [ODbL](https://opendatacommons.org/licenses/odbl/1-0/). O arquivo `app/src/main/res/raw/catalogo_bancos.tsv` contém código, nome completo e nome reduzido. Para atualizar a lista, use as colunas `Número_Código`, `Nome_Extenso` e `Nome_Reduzido` do CSV oficial e filtre os códigos de três dígitos. O app envia o código como texto, preservando zeros à esquerda.

Em qualquer seção de endereço, sair do campo com um CEP válido consulta o ViaCEP. O seletor de cidade usa o mesmo catálogo do IBGE e o mesmo diálogo da Home. Escolher um município atualiza apenas o endereço da seção atual. O seletor mostra a cidade e a UF e preserva ambos no rascunho. O ponto de referência é preenchido pelo usuário e continua obrigatório, inclusive no endereço do representante. **Site ou perfil profissional** mantém a obrigatoriedade e a propriedade `siteUrl`. A aceitação de uma URL pública de perfil profissional no lugar de um site próprio depende de confirmação da Stone/Pagar.me.

A tela **Cartão de crédito** informa que o cadastro estará disponível em breve. A ação de adicionar fica desabilitada até a integração de tokenização. A interface está preparada para apresentar bandeira, quatro últimos dígitos e o indicador de cartão principal a partir dos dados da integração.
