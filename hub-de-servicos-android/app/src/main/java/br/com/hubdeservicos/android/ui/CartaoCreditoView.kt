package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R

internal data class ResumoCartaoTokenizado(val bandeira: String, val ultimosQuatro: String, val principal: Boolean) {
    init {
        require(ultimosQuatro.matches(Regex("[0-9]{4}")))
    }
}

internal class CartaoCreditoView(
    context: Context,
    cartoes: List<ResumoCartaoTokenizado> = emptyList(),
    voltar: () -> Unit
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        background = context.superficie(R.color.gastro_papel, 18, R.color.gastro_linha)
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))

        adicionar(context.texto(context.getString(R.string.cartao_credito_titulo), 27, R.color.gastro_oliva,
            Typeface.create("serif", Typeface.BOLD)).apply { isAccessibilityHeading = true })
        adicionar(context.texto(context.getString(R.string.cartao_credito_descricao), 16, R.color.gastro_texto), 10)

        if (cartoes.isEmpty()) {
            adicionar(context.texto(context.getString(R.string.cartao_credito_em_breve), 14, R.color.gastro_texto_suave), 12)
        }
        for (cartao in cartoes) {
            adicionar(LinearLayout(context).apply {
                orientation = VERTICAL
                background = context.superficie(R.color.gastro_fundo, 12, R.color.gastro_linha)
                setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
                adicionar(context.texto(context.getString(R.string.cartao_credito_final,
                    cartao.bandeira, cartao.ultimosQuatro), 16, R.color.gastro_texto))
                if (cartao.principal) {
                    adicionar(context.texto(context.getString(R.string.cartao_credito_principal), 14, R.color.gastro_oliva), 4)
                }
            }, 12)
        }
        adicionar(context.botaoCadastro(R.string.cartao_credito_adicionar, false) {}, 16)
        adicionar(context.botaoCadastro(R.string.perfil_voltar, true, false, voltar), 8)
    }
}
