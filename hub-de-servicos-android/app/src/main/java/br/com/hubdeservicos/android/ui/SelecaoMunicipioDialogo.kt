package br.com.hubdeservicos.android.ui

import android.app.Activity
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.Municipio

internal class SelecaoMunicipioDialogo(
    activity: Activity,
    pesquisarMunicipios: (String, (List<Municipio>?) -> Unit) -> Unit,
    confirmarMunicipio: (Municipio, (Boolean) -> Unit) -> Unit,
    usarLocalizacao: (() -> Unit)? = null,
    aoFechar: () -> Unit
) {
    private val dialogo = SelecaoPesquisavelDialogo(
        activity = activity,
        textos = TextosSelecao(R.string.municipio_selecionar, R.string.municipio_pesquisar, R.string.municipio_vazio),
        rotuloItem = { activity.getString(R.string.localizacao_cidade, it.nome, it.uf) },
        buscarItens = pesquisarMunicipios,
        confirmarItem = confirmarMunicipio,
        acaoSecundaria = usarLocalizacao?.let { R.string.municipio_usar_localizacao to it },
        aoFechar = aoFechar
    )

    fun fechar() = dialogo.fechar()
}
