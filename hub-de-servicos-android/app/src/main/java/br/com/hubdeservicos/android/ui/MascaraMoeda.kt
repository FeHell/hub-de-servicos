package br.com.hubdeservicos.android.ui

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.TextKeyListener
import android.widget.EditText
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

internal object MascaraMoeda {
    private val brasil = Locale.forLanguageTag("pt-BR")
    private const val MAX_DIGITOS_CENTAVOS = 12
    private val caracteres = Regex("[0-9R$.,\\s\\u00a0\\u202f]*")

    fun centavos(texto: CharSequence) = texto.filter { it in '0'..'9' }.toString().trimStart('0')

    fun valor(texto: CharSequence): BigDecimal? {
        require(texto.matches(caracteres))
        val digitos = centavos(texto)
        if (digitos.isEmpty() && texto.none { it == '0' }) return null
        require(digitos.length <= MAX_DIGITOS_CENTAVOS)
        return BigDecimal(digitos.ifEmpty { "0" }).movePointLeft(2)
    }

    fun formato(valor: BigDecimal?) = valor?.let {
        NumberFormat.getCurrencyInstance(brasil).format(it)
    }.orEmpty()
}

internal fun EditText.aplicarMascaraMoeda(valor: BigDecimal?) {
    // Mantém teclado numérico sem descartar silenciosamente sinais em valores colados.
    keyListener = TextKeyListener.getInstance()
    setRawInputType(InputType.TYPE_CLASS_NUMBER)
    setText(MascaraMoeda.formato(valor))
    addTextChangedListener(object : TextWatcher {
        private var formatando = false
        private var anterior = ""

        override fun beforeTextChanged(texto: CharSequence, start: Int, count: Int, after: Int) {
            if (!formatando) anterior = texto.toString()
        }

        override fun onTextChanged(texto: CharSequence, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(texto: Editable) {
            if (formatando) return
            val atuais = MascaraMoeda.centavos(texto)
            val formatado = try {
                val apagouUltimo = atuais.isEmpty() && MascaraMoeda.centavos(anterior).length <= 1 &&
                    texto.length < anterior.length
                MascaraMoeda.formato(if (apagouUltimo) null else MascaraMoeda.valor(texto))
            } catch (_: IllegalArgumentException) {
                anterior
            }
            if (formatado.contentEquals(texto)) return

            val fim = selectionEnd.coerceIn(0, texto.length)
            val digitosDepois = texto.subSequence(fim, texto.length).count { it in '0'..'9' }
            val cursor = if (digitosDepois == 0) formatado.length else {
                MascaraTelefone.posicao(formatado, (formatado.count { it in '0'..'9' } - digitosDepois).coerceAtLeast(0))
            }
            formatando = true
            try {
                texto.replace(0, texto.length, formatado)
                setSelection(cursor.coerceAtMost(texto.length))
            } finally {
                formatando = false
            }
        }
    })
}
