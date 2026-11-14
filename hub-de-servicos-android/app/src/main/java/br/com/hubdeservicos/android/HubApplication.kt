package br.com.hubdeservicos.android

import android.app.Application
import br.com.hubdeservicos.android.auth.ApiAutenticacao
import br.com.hubdeservicos.android.auth.SessaoApp
import br.com.hubdeservicos.android.auth.SessaoKeystore
import br.com.hubdeservicos.android.ui.AutenticacaoModel

class HubApplication : Application() {
    val autenticacao by lazy {
        val origem = BuildConfig.API_ORIGEM.trimEnd('/')
        val api = ApiAutenticacao(origem)
        AutenticacaoModel(SessaoApp(api, SessaoKeystore(this, origem)), api)
    }
}
