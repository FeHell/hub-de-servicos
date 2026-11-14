package br.com.hubdeservicos.android.ui

internal object MascaraTelefone {
    fun digitos(valor: CharSequence): String = valor.filter { it in '0'..'9' }.toString()

    fun nacional(valor: CharSequence): String {
        val digitos = digitos(valor)
        val semEspacos = valor.trim()
        val temCodigoPais = semEspacos.startsWith("+55") ||
            (semEspacos.length in 12..13 && semEspacos == digitos)
        return if (temCodigoPais && digitos.startsWith("55")) digitos.drop(2) else digitos
    }

    fun formatar(numero: String): String {
        if (numero.isEmpty()) return ""
        if (numero.length < 2) return "($numero"
        val ddd = numero.take(2)
        val restante = numero.drop(2)
        if (restante.isEmpty()) return "($ddd)"
        val corte = if (restante.startsWith('9') || restante.length == 9) 5 else 4
        val prefixo = restante.take(corte)
        val sufixo = restante.drop(corte)
        return if (sufixo.isEmpty()) "($ddd) $prefixo" else "($ddd) $prefixo-$sufixo"
    }

    fun apagarSeparador(numero: String, digitosAntes: Int, paraTras: Boolean): Pair<String, Int> {
        val indice = if (paraTras) digitosAntes - 1 else digitosAntes
        if (indice !in numero.indices) return numero to digitosAntes
        return numero.removeRange(indice, indice + 1) to indice
    }

    fun posicao(texto: String, quantidade: Int): Int {
        if (quantidade <= 0) return texto.indexOfFirst { it in '0'..'9' }.coerceAtLeast(0)
        var digitos = 0
        for (indice in texto.indices) {
            if (texto[indice] in '0'..'9' && ++digitos == quantidade) return indice + 1
        }
        return texto.length
    }
}
