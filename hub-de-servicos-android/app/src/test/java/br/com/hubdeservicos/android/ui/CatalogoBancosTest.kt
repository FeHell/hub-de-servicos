package br.com.hubdeservicos.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoBancosTest {
    private val catalogo = CatalogoBancos("""
        001	Banco do Brasil S.A.	BCO DO BRASIL S.A.
        077	Banco Inter S.A.	BANCO INTER
        104	Caixa Econômica Federal	CAIXA ECONOMICA FEDERAL
        341	ITAÚ UNIBANCO S.A.	ITAÚ UNIBANCO S.A.
    """.trimIndent().reader())

    @Test fun filtraPorNomeOuAbreviacaoSemDistinguirAcentosOuMaiusculas() {
        assertEquals(listOf("341"), catalogo.pesquisar("itau").map(Banco::codigo))
        assertEquals(listOf("104"), catalogo.pesquisar("CAIXA economica").map(Banco::codigo))
        assertEquals(listOf("001"), catalogo.pesquisar("bco brasil").map(Banco::codigo))
        assertEquals(listOf("001", "077", "341"), catalogo.pesquisar("Banco").map(Banco::codigo))
    }

    @Test fun buscaERecuperaCodigoComZerosAEsquerda() {
        assertEquals(Banco("001", "Banco do Brasil S.A."), catalogo.consultar("001"))
        assertEquals(listOf("077"), catalogo.pesquisar("077").map(Banco::codigo))
        assertNull(catalogo.consultar("77"))
    }

    @Test fun listaBancosAoAbrirETrataPesquisaSemResultados() {
        assertEquals(4, catalogo.pesquisar("").size)
        assertTrue(catalogo.pesquisar("nome inexistente").isEmpty())
        assertTrue(catalogo.pesquisar("007").isEmpty())
    }
}
