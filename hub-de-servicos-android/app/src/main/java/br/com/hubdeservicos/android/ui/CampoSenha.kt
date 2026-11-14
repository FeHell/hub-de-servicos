package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import br.com.hubdeservicos.android.R

internal class CampoSenha(context: Context, rotulo: Int, nova: Boolean, habilitado: Boolean) :
    LinearLayout(context) {

    val campo = context.entrada(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
    private val nome = context.resources.getString(rotulo).lowercase()
    private val olho = ImageButton(context)
    private var visivel = false

    init {
        orientation = VERTICAL
        adicionar(context.texto(context.getString(rotulo), 14, R.color.gastro_texto, Typeface.DEFAULT_BOLD)
            .apply { labelFor = campo.id })
        val caixa = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = context.superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
        }
        adicionar(caixa, 6)
        campo.typeface = Typeface.DEFAULT
        campo.isEnabled = habilitado
        campo.setAutofillHints(if (nova) "newPassword" else View.AUTOFILL_HINT_PASSWORD)
        caixa.addView(campo, LayoutParams(0, -2, 1f))

        olho.apply {
            isEnabled = habilitado
            scaleType = ImageView.ScaleType.CENTER
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x227B6246), null,
                context.superficie(android.R.color.white, 12)
            )
            setOnClickListener {
                visivel = !visivel
                atualizarVisibilidade()
            }
        }
        caixa.addView(olho, LayoutParams(context.dp(52), context.dp(52)))
        atualizarVisibilidade()
    }

    private fun atualizarVisibilidade() {
        val inicio = campo.selectionStart
        val fim = campo.selectionEnd
        // Altera só a representação. O tipo de entrada e o texto digitado ficam no mesmo EditText.
        campo.transformationMethod = if (visivel) null else PasswordTransformationMethod.getInstance()
        if (inicio >= 0 && fim >= 0) campo.setSelection(inicio, fim)
        olho.setImageResource(if (visivel) R.drawable.senha_visivel else R.drawable.senha_oculta)
        olho.contentDescription = context.getString(if (visivel) R.string.ocultar_senha else R.string.mostrar_senha, nome)
    }
}
