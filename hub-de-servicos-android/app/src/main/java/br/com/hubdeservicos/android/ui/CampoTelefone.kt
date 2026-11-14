package br.com.hubdeservicos.android.ui

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

internal fun EditText.aplicarMascaraTelefone(valor: String) {
    setText(MascaraTelefone.formatar(MascaraTelefone.nacional(valor)))
    addTextChangedListener(object : TextWatcher {
        private var formatando = false
        private var anterior = ""
        private var inicio = 0
        private var removidos = 0
        private var inseridos = 0
        private var selecaoAnterior = 0
        private var fimSelecaoAnterior = 0

        override fun beforeTextChanged(texto: CharSequence, start: Int, count: Int, after: Int) {
            if (formatando) return
            anterior = texto.toString()
            inicio = start
            removidos = count
            inseridos = after
            selecaoAnterior = selectionStart
            fimSelecaoAnterior = selectionEnd
        }

        override fun onTextChanged(texto: CharSequence, start: Int, before: Int, count: Int) = Unit

        override fun afterTextChanged(texto: Editable) {
            if (formatando) return
            val brutos = MascaraTelefone.digitos(texto)
            var numero = MascaraTelefone.nacional(texto)
            val fimEdicao = (inicio + inseridos).coerceAtMost(texto.length)
            var digitosAntes = MascaraTelefone.digitos(texto.subSequence(0, fimEdicao)).length -
                (brutos.length - numero.length)
            if (inseridos == 0 && removidos == 1 && selecaoAnterior == fimSelecaoAnterior &&
                anterior[inicio] !in '0'..'9') {
                val (restante, posicao) = MascaraTelefone.apagarSeparador(numero, digitosAntes,
                    paraTras = selecaoAnterior > inicio)
                numero = restante
                digitosAntes = posicao
            }
            val excedeu = numero.length > 11
            val formatado = if (excedeu) anterior else MascaraTelefone.formatar(numero)
            if (formatado.contentEquals(texto)) return
            val cursor = if (excedeu) selecaoAnterior.coerceIn(0, formatado.length)
                else MascaraTelefone.posicao(formatado, digitosAntes)
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
