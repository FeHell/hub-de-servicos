package br.com.hubdeservicos.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MascaraTelefoneTest {
    @Test
    fun aplicaMascaraProgressivaParaCelularEFixo() {
        assertEquals("(3", MascaraTelefone.formatar("3"))
        assertEquals("(34)", MascaraTelefone.formatar("34"))
        assertEquals("(34) 99999", MascaraTelefone.formatar("3499999"))
        assertEquals("(34) 99999-9999", MascaraTelefone.formatar("34999999999"))
        assertEquals("(34) 3333-3333", MascaraTelefone.formatar("3433333333"))
    }

    @Test
    fun preparaTelefoneParaApiSemMascaraPreservandoDdd55() {
        assertEquals("34999999999", MascaraTelefone.nacional("+55 (34) 99999-9999"))
        assertEquals("34999999999", MascaraTelefone.nacional("(34) 99999-9999"))
        assertEquals("55999999999", MascaraTelefone.nacional("(55) 99999-9999"))
        assertEquals("34999999999", MascaraTelefone.nacional("5534999999999"))
        assertEquals("559999999990", MascaraTelefone.nacional("(55) 99999-99990"))
        assertEquals("", MascaraTelefone.nacional(""))
    }

    @Test
    fun posicionaCursorPelosDigitosEApagaAoCruzarSeparadores() {
        assertEquals(1, MascaraTelefone.posicao("(34) 99999-9999", 0))
        assertEquals(3, MascaraTelefone.posicao("(34) 99999-9999", 2))
        assertEquals(10, MascaraTelefone.posicao("(34) 99999-9999", 7))
        assertEquals("3412347890" to 6, MascaraTelefone.apagarSeparador("34123457890", 7, true))
        assertEquals("3412345890" to 7, MascaraTelefone.apagarSeparador("34123457890", 7, false))
    }
}
