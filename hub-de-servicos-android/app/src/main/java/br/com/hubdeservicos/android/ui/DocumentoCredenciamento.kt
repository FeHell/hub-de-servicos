package br.com.hubdeservicos.android.ui

import java.util.Locale

internal fun normalizarDocumento(valor: String): String =
    valor.uppercase(Locale.ROOT).filter { it in '0'..'9' || it in 'A'..'Z' }

internal fun documentoCredenciamentoValido(valor: String): Boolean {
    if (valor.toSet().size < 2) return false
    return when {
        valor.matches(Regex("[0-9]{11}")) ->
            digitoDocumento(valor, 9, 10, 1) == valor[9] - '0' &&
                digitoDocumento(valor, 10, 11, 1) == valor[10] - '0'
        valor.matches(Regex("[0-9A-Z]{12}[0-9]{2}")) ->
            digitoDocumento(valor, 12, 5, 9) == valor[12] - '0' &&
                digitoDocumento(valor, 13, 6, 9) == valor[13] - '0'
        else -> false
    }
}

private fun digitoDocumento(valor: String, tamanho: Int, primeiroPeso: Int, reinicio: Int): Int {
    var peso = primeiroPeso
    var soma = 0
    repeat(tamanho) { indice ->
        soma += (valor[indice] - '0') * peso--
        if (peso == 1) peso = reinicio
    }
    val resto = soma % 11
    return if (resto < 2) 0 else 11 - resto
}
