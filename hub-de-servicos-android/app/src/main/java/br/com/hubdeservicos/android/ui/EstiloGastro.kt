package br.com.hubdeservicos.android.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import br.com.hubdeservicos.android.R
import kotlin.math.roundToInt

internal fun Context.dp(valor: Int) = (valor * resources.displayMetrics.density).roundToInt()

internal fun Context.superficie(cor: Int, raio: Int, borda: Int? = null) =
    GradientDrawable().apply {
        setColor(getColor(cor))
        cornerRadius = dp(raio).toFloat()
        if (borda != null) setStroke(dp(1), getColor(borda))
    }

internal fun Context.acaoVisual(cor: Int, raio: Int, borda: Int? = null) =
    RippleDrawable(ColorStateList.valueOf(0x227B6246), superficie(cor, raio, borda), null)

internal fun Context.texto(valor: CharSequence, tamanho: Int, cor: Int, fonte: Typeface? = null) =
    TextView(this).apply {
        text = valor
        textSize = tamanho.toFloat()
        setTextColor(context.getColor(cor))
        fonte?.let { typeface = it }
    }

internal fun Context.entrada(tipo: Int, dica: String? = null) = EditText(this).apply {
    id = android.view.View.generateViewId()
    isSaveEnabled = false
    isSingleLine = true
    inputType = tipo
    setTextColor(context.getColor(R.color.gastro_texto))
    setHintTextColor(context.getColor(R.color.gastro_texto_suave))
    background = null
    hint = dica
    textSize = 16f
    minHeight = context.dp(52)
    setPaddingRelative(context.dp(14), context.dp(8), context.dp(14), context.dp(8))
}

internal fun LinearLayout.adicionar(view: android.view.View, margem: Int = 0) {
    addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(margem) })
}
