package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.Usuario

internal class HomeView(
    context: Context, usuario: Usuario, estado: Estado, abrirPerfil: () -> Unit, sair: () -> Unit
) :
    LinearLayout(context) {

    init {
        orientation = VERTICAL
        val serifa = Typeface.create("serif", Typeface.BOLD)
        adicionar(context.texto(context.getString(R.string.saudacao, estado.perfil?.nome ?: usuario.email), 30, R.color.gastro_oliva, serifa)
            .apply { isAccessibilityHeading = true })

        adicionar(PendenciasCadastroView(context, estado.perfil, !estado.carregando, abrirPerfil), 16)

        val apresentacao = LinearLayout(context).apply {
            orientation = VERTICAL
            clipToOutline = true
            background = context.superficie(R.color.gastro_oliva_escuro, 18)
            addView(ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(R.drawable.gastro_cozinha)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(-1, context.dp(188)))
            adicionar(context.texto(context.getString(R.string.home_chamada), 24, R.color.gastro_papel, serifa).apply {
                setPadding(context.dp(18), context.dp(14), context.dp(18), context.dp(16))
            })
        }
        adicionar(apresentacao, 20)
        adicionar(context.texto(context.getString(R.string.home_oportunidades), 23, R.color.gastro_oliva, serifa)
            .apply { isAccessibilityHeading = true }, 24)

        val vazio = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(14), context.dp(16), context.dp(14), context.dp(16))
            background = context.superficie(R.color.gastro_papel, 14, R.color.gastro_linha)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.gastro_oportunidades)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(context.dp(26), context.dp(26)))
            addView(context.texto(context.getString(R.string.home_oportunidades_vazio), 15, R.color.gastro_texto_suave),
                LayoutParams(0, -2, 1f).apply { marginStart = context.dp(12) })
        }
        adicionar(vazio, 10)

        estado.erro?.let { erro ->
            adicionar(context.texto(context.getString(erro), 15, R.color.gastro_erro)
                .apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }, 12)
        }
        adicionar(Button(context).apply {
            setText(R.string.sair)
            minimumHeight = context.dp(48)
            isAllCaps = false
            setTextColor(context.getColor(R.color.gastro_oliva))
            background = context.acaoVisual(R.color.gastro_terracota_clara, 12)
            isEnabled = !estado.carregando
            alpha = if (isEnabled) 1f else 0.55f
            setOnClickListener { sair() }
        }, 20)
    }
}
