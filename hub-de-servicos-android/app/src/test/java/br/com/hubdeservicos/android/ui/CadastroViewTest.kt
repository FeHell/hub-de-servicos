package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.auth.SituacaoTelefone
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CadastroViewTest {
    @Test
    fun statusConsideraRascunhoEOEstadoPersistidoDoMesmoNumero() {
        val salvo = "+5534999999999"
        val verificado = SituacaoTelefone.VERIFICADO
        assertEquals(SituacaoTelefone.NAO_INFORMADO, EstadoTelefone.apresentar("", salvo, verificado))
        assertEquals(verificado, EstadoTelefone.apresentar("(34) 99999-9999", salvo, verificado))
        assertEquals(SituacaoTelefone.PENDENTE, EstadoTelefone.apresentar("(34) 99999-9998", salvo, verificado))
        assertEquals(verificado, EstadoTelefone.apresentar(salvo, salvo, verificado))
        assertEquals(SituacaoTelefone.PENDENTE, EstadoTelefone.apresentar(salvo, salvo, SituacaoTelefone.PENDENTE))
        assertFalse(EstadoTelefone.valido("(34) 999"))
        assertFalse(EstadoTelefone.valido("(04) 99999-9999"))
        assertTrue(EstadoTelefone.valido("(34) 3333-3333"))
        assertTrue(EstadoTelefone.valido(salvo))
    }

    @Test
    fun mascaraMonetariaPreservaCentavosEAceitaLimpeza() {
        assertNull(MascaraMoeda.valor(""))
        assertEquals(BigDecimal("0.00"), MascaraMoeda.valor("R$ 0,00"))
        assertEquals(BigDecimal("25.05"), MascaraMoeda.valor("R$ 25,05"))
        assertEquals(BigDecimal("9999999999.99"), MascaraMoeda.valor("R$ 9.999.999.999,99"))
        assertEquals("R$ 1.025,05", MascaraMoeda.formato(BigDecimal("1025.05")).replace('\u00a0', ' '))
        assertThrows(IllegalArgumentException::class.java) { MascaraMoeda.valor("1000000000000") }
        assertThrows(IllegalArgumentException::class.java) { MascaraMoeda.valor("-10,01") }
        assertThrows(IllegalArgumentException::class.java) { MascaraMoeda.valor("−10,01") }
    }
}
