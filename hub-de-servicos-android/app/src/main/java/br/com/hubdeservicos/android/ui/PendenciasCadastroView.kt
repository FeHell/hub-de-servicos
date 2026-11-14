package br.com.hubdeservicos.android.ui

import android.content.Context
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.CadastroUsuario

internal class PendenciasCadastroView(
    context: Context, perfil: CadastroUsuario?, habilitado: Boolean, abrir: () -> Unit
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        val pendencias = perfil?.pendencias.orEmpty()
        if (perfil == null || pendencias.isNotEmpty()) {
            background = context.superficie(R.color.gastro_terracota_clara, 16)
            setPadding(context.dp(16), context.dp(16), context.dp(16), context.dp(12))
            val mensagem = if (perfil == null) R.string.perfil_indisponivel else R.string.perfil_pendente
            adicionar(context.texto(context.getString(mensagem), 15, R.color.gastro_oliva))
            pendencias.map {
                when (it) {
                    "NOME" -> R.string.pendencia_nome
                    "TELEFONE" -> R.string.pendencia_telefone
                    "CONFIRMACAO_TELEFONE" -> R.string.pendencia_confirmacao
                    else -> R.string.pendencia_outro
                }
            }.distinct().forEach { rotulo ->
                adicionar(context.texto("• " + context.getString(rotulo), 14, R.color.gastro_texto), 6)
            }
        }
        val acao = when {
            perfil == null -> R.string.tentar_novamente
            pendencias.isEmpty() -> R.string.perfil_editar
            else -> R.string.perfil_continuar
        }
        val botao = context.botaoCadastro(acao, habilitado, principal = false, acao = abrir)
        adicionar(botao, if (perfil == null || pendencias.isNotEmpty()) 10 else 0)
    }
}
