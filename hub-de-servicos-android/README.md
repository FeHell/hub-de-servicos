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
