package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.PapelUsuario

internal class PreferenciasHomeView(
    context: Context, estado: Estado, selecionar: (PapelUsuario, Boolean) -> Unit
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        adicionar(context.texto(context.getString(R.string.papeis_titulo), 21, R.color.gastro_oliva,
            Typeface.DEFAULT_BOLD).apply { isAccessibilityHeading = true })
        val papeis = estado.perfil?.papeis
        if (papeis == null) {
            adicionar(context.texto(context.getString(R.string.perfis_indisponiveis), 15, R.color.gastro_texto_suave), 8)
        } else {
            adicionar(opcao(R.string.papel_empresa, R.string.papel_empresa_descricao, R.drawable.gastro_oportunidades,
                PapelUsuario.EMPRESA in papeis, !estado.carregando) { selecionar(PapelUsuario.EMPRESA, it) }, 10)
            adicionar(opcao(R.string.papel_trabalhador, R.string.papel_trabalhador_descricao, R.drawable.gastro_trabalhador,
                PapelUsuario.TRABALHADOR in papeis, !estado.carregando) { selecionar(PapelUsuario.TRABALHADOR, it) }, 8)
        }
    }

    private fun opcao(titulo: Int, descricao: Int, simbolo: Int, marcado: Boolean, habilitado: Boolean,
                      acao: (Boolean) -> Unit) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(76)
        setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(12))
        background = context.acaoVisual(if (marcado) R.color.gastro_terracota_clara else R.color.gastro_papel,
            14, if (marcado) R.color.gastro_terracota else R.color.gastro_linha)
        isEnabled = habilitado
        isFocusable = true
        val nome = context.getString(titulo)
        val detalhe = context.getString(descricao)
        contentDescription = "$nome. $detalhe"
        accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = CheckBox::class.java.name
                info.isCheckable = true
                info.isChecked = marcado
            }
        }
        addView(icone(simbolo), LayoutParams(context.dp(24), context.dp(24)))
        val texto = LinearLayout(context).apply {
            orientation = VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            adicionar(context.texto(nome, 16, R.color.gastro_oliva, Typeface.DEFAULT_BOLD))
            adicionar(context.texto(detalhe, 14, R.color.gastro_texto_suave), 3)
        }
        addView(texto, LayoutParams(0, -2, 1f).apply { marginStart = context.dp(12) })
        if (marcado) addView(icone(R.drawable.gastro_check),
            LayoutParams(context.dp(22), context.dp(22)).apply { marginStart = context.dp(10) })
        setOnClickListener { acao(!marcado) }
    }

    private fun icone(recurso: Int) = ImageView(context).apply {
        setImageResource(recurso)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}

internal class LocalizacaoHomeView(
    context: Context, situacao: LocalizacaoUi, rotuloAcao: Int, atualizar: () -> Unit
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        setPadding(context.dp(14), context.dp(12), context.dp(14), context.dp(8))
        background = context.superficie(R.color.gastro_papel, 14, R.color.gastro_linha)

        val titulo = context.texto(context.getString(R.string.localizacao_titulo), 15, R.color.gastro_oliva,
            Typeface.DEFAULT_BOLD).apply { isAccessibilityHeading = true }
        adicionar(LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageResource(R.drawable.gastro_localizacao)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(context.dp(20), context.dp(20)))
            addView(titulo, LayoutParams(0, -2, 1f).apply { marginStart = context.dp(8) })
        })
        val mensagem = when (situacao) {
            LocalizacaoUi.Obtendo -> context.getString(R.string.localizacao_obtendo)
            LocalizacaoUi.SemPermissao -> context.getString(R.string.localizacao_permissao)
            LocalizacaoUi.Indisponivel -> context.getString(R.string.localizacao_indisponivel)
            LocalizacaoUi.Erro -> context.getString(R.string.localizacao_erro)
            is LocalizacaoUi.Identificada -> context.getString(R.string.localizacao_cidade,
                situacao.municipio.nome, situacao.municipio.uf)
        }
        adicionar(context.texto(mensagem, 15, R.color.gastro_texto_suave)
            .apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }, 5)
        if (situacao != LocalizacaoUi.Obtendo) adicionar(context.botaoCadastro(rotuloAcao, true, false, atualizar), 8)
    }
}
