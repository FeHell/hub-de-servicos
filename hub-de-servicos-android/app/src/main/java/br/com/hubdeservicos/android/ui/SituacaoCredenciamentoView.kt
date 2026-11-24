package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.AmbienteRecebedor
import br.com.hubdeservicos.android.auth.ErroRecebedor
import br.com.hubdeservicos.android.auth.RecebedorCredenciamento
import br.com.hubdeservicos.android.auth.SituacaoRecebedor

internal enum class ApresentacaoCredenciamento(
    val titulo: Int, val mensagem: Int, val icone: Int, val fundo: Int, val cor: Int
) {
    NAO_INICIADO(R.string.cred_status_nao_iniciado, R.string.cred_status_preencher, R.drawable.cred_status_documento,
        R.color.cred_status_neutro_fundo, R.color.cred_status_neutro_texto),
    AGUARDANDO(R.string.cred_status_aguardando, R.string.cred_recebedor_analise, R.drawable.cred_status_relogio,
        R.color.cred_status_espera_fundo, R.color.cred_status_espera_texto),
    INDETERMINADO(R.string.cred_status_aguardando, R.string.cred_envio_consultar_antes, R.drawable.cred_status_relogio,
        R.color.cred_status_espera_fundo, R.color.cred_status_espera_texto),
    PROCESSANDO(R.string.cred_status_processando, R.string.cred_status_processando_texto, R.drawable.cred_status_relogio,
        R.color.cred_status_processando_fundo, R.color.cred_status_processando_texto),
    APROVADO(R.string.cred_status_aprovado, R.string.cred_recebedor_ativo, R.drawable.gastro_check,
        R.color.cred_status_aprovado_fundo, R.color.cred_status_aprovado_texto),
    RECUSADO(R.string.cred_status_recusado, R.string.cred_status_recusado_texto, R.drawable.cred_status_recusado,
        R.color.cred_status_erro_fundo, R.color.gastro_erro),
    RESTRITO(R.string.cred_status_restrito, R.string.cred_recebedor_restrito, R.drawable.gastro_alerta,
        R.color.cred_status_erro_fundo, R.color.gastro_erro),
    ERRO(R.string.cred_status_erro, R.string.cred_recebedor_falha_orientacao, R.drawable.gastro_alerta,
        R.color.cred_status_erro_fundo, R.color.gastro_erro),
    NOVA_SUBMISSAO(R.string.cred_status_nova_submissao, R.string.cred_dados_alterados, R.drawable.cred_status_reenviar,
        R.color.cred_status_reenvio_fundo, R.color.cred_status_reenvio_texto);

    companion object {
        fun de(recebedor: RecebedorCredenciamento) = when {
            recebedor.emProcessamento || recebedor.situacao == SituacaoRecebedor.EM_ANALISE -> PROCESSANDO
            recebedor.situacao == SituacaoRecebedor.AGUARDANDO_NOVA_SUBMISSAO -> NOVA_SUBMISSAO
            recebedor.aprovado -> APROVADO
            recebedor.statusProvedor == "REFUSED" -> RECUSADO
            recebedor.statusProvedor in setOf("BLOCKED", "SUSPENDED", "INACTIVE") -> RESTRITO
            recebedor.situacao == SituacaoRecebedor.CRIADO -> AGUARDANDO
            recebedor.situacao == SituacaoRecebedor.INDETERMINADO -> INDETERMINADO
            recebedor.situacao == SituacaoRecebedor.FALHA -> ERRO
            else -> NAO_INICIADO
        }
    }
}

internal class SituacaoCredenciamentoView(
    context: Context, recebedor: RecebedorCredenciamento, habilitado: Boolean,
    reenviar: () -> Unit, consultar: () -> Unit
) : LinearLayout(context) {
    init {
        val apresentacao = ApresentacaoCredenciamento.de(recebedor)
        orientation = VERTICAL
        background = context.superficie(apresentacao.fundo, 14)
        val margem = context.dp(14)
        setPaddingRelative(margem, margem, margem, margem)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        val linha = LinearLayout(context).apply {
            val icone = ImageView(context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageResource(apresentacao.icone)
                setColorFilter(context.getColor(apresentacao.cor))
            }
            addView(icone, LayoutParams(context.dp(24), context.dp(24)).apply { marginEnd = context.dp(10) })
            addView(context.texto(context.getString(apresentacao.titulo), 18, apresentacao.cor, Typeface.DEFAULT_BOLD),
                LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        adicionar(linha)
        val texto = when (recebedor.erro) {
            ErroRecebedor.AUTENTICACAO -> context.getString(R.string.cred_envio_autenticacao_falhou)
            ErroRecebedor.DADOS_RECUSADOS -> context.getString(R.string.cred_envio_dados_recusados)
            else -> context.getString(apresentacao.mensagem)
        }
        adicionar(context.texto(texto, 15, apresentacao.cor), 6)
        recebedor.orientacao?.let { adicionar(context.texto(it, 14, apresentacao.cor), 6) }
        if (recebedor.ambiente == AmbienteRecebedor.DEV) {
            adicionar(context.texto(context.getString(R.string.cred_ambiente_teste), 14, apresentacao.cor), 6)
        }
        if (recebedor.podeReenviar && !recebedor.emProcessamento) {
            adicionar(context.botaoCadastro(R.string.cred_reenviar_recebedor, habilitado, acao = reenviar), 12)
        }
        if (recebedor.podeConsultar) {
            adicionar(context.botaoCadastro(R.string.cred_consultar_envio, habilitado, false, consultar), 8)
        }
    }
}
