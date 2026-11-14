package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.R

internal fun erroSenha(senha: String, cadastro: Boolean): Int? = when {
    senha.isBlank() -> R.string.senha_vazia
    senha.toByteArray(Charsets.UTF_8).size > 72 -> R.string.senha_longa
    cadastro && senha.length < 12 -> R.string.senha_curta
    else -> null
}

internal fun erroConfirmacao(senha: String, confirmacao: String): Int? = when {
    confirmacao.isEmpty() -> R.string.confirmacao_vazia
    senha != confirmacao -> R.string.senhas_diferentes
    else -> null
}
