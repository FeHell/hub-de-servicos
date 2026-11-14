package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacaoSenhaTest {
    @Test
    fun cadastroMantemLimitesDeSenha() {
        assertEquals(R.string.senha_curta, erroSenha("a".repeat(11), cadastro = true))
        assertNull(erroSenha("a".repeat(12), cadastro = true))
        assertNull(erroSenha("á".repeat(36), cadastro = true))
        assertEquals(R.string.senha_longa, erroSenha("á".repeat(37), cadastro = true))
    }

    @Test
    fun loginMantemRegrasExistentes() {
        assertEquals(R.string.senha_vazia, erroSenha(" ", cadastro = false))
        assertNull(erroSenha("curta", cadastro = false))
        assertEquals(R.string.senha_longa, erroSenha("á".repeat(37), cadastro = false))
    }

    @Test
    fun exigeConfirmacaoExatamenteIgualSemRemoverEspacos() {
        assertEquals(R.string.confirmacao_vazia, erroConfirmacao("SenhaForte123", ""))
        assertEquals(R.string.senhas_diferentes, erroConfirmacao("SenhaForte123", "senhaForte123"))
        assertEquals(R.string.senhas_diferentes, erroConfirmacao("SenhaForte123 ", "SenhaForte123"))
        assertNull(erroConfirmacao("SenhaForte123 ", "SenhaForte123 "))
    }
}
