package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.auth.CadastroUsuario
import br.com.hubdeservicos.android.auth.PapelUsuario
import br.com.hubdeservicos.android.auth.RascunhoCredenciamento
import br.com.hubdeservicos.android.auth.Remuneracao
import br.com.hubdeservicos.android.auth.SituacaoTelefone
import br.com.hubdeservicos.android.auth.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FinancasPorPerfilTest {
    @Test fun aplicaAVisibilidadeEAProtecaoDeRotaAosQuatroPerfis() {
        val trabalhador = PapelUsuario.TRABALHADOR
        val empresa = PapelUsuario.EMPRESA
        val destinos = listOf(Tela.CREDENCIAMENTO, Tela.CARTAO_CREDITO)
        val casos = listOf(
            setOf(trabalhador) to listOf(Tela.CREDENCIAMENTO),
            setOf(empresa) to listOf(Tela.CARTAO_CREDITO),
            setOf(trabalhador, empresa) to destinos,
            emptySet<PapelUsuario>() to emptyList()
        )
        for ((papeis, permitidos) in casos) {
            val estado = estadoCom(papeis)
            assertEquals(permitidos, destinos.filter(estado::podeAcessar))
            for (destino in destinos) {
                assertEquals(if (destino in permitidos) destino else Tela.HOME,
                    estado.copy(tela = destino).protegerTelaFinanceira().tela)
            }
        }
    }

    @Test fun exigeUsuarioEPerfilEfetivoAoProtegerRota() {
        val perfilTrabalhador = estadoCom(setOf(PapelUsuario.TRABALHADOR))
        assertEquals(Tela.LOGIN, perfilTrabalhador.copy(tela = Tela.CREDENCIAMENTO, usuario = null)
            .protegerTelaFinanceira().tela)
        val carregandoPerfil = perfilTrabalhador.copy(perfil = null)
        assertFalse(carregandoPerfil.podeAcessar(Tela.CREDENCIAMENTO))
        assertFalse(carregandoPerfil.podeAcessar(Tela.CARTAO_CREDITO))
    }

    @Test fun fechaContaELimpaEstadoDoFormularioSeOPapelForRevogado() {
        val estado = estadoCom(setOf(PapelUsuario.EMPRESA)).copy(tela = Tela.CREDENCIAMENTO,
            credenciamento = RascunhoCredenciamento(), etapaCredenciamento = 2,
            camposInvalidos = listOf("pessoaFisica.siteUrl"))
        val protegido = estado.protegerTelaFinanceira()
        assertEquals(Tela.HOME, protegido.tela)
        assertNull(protegido.credenciamento)
        assertEquals(0, protegido.etapaCredenciamento)
        assertEquals(emptyList<String>(), protegido.camposInvalidos)
    }

    @Test fun resumoDeCartaoAceitaApenasOsQuatroDigitosFinais() {
        assertEquals("0012", ResumoCartaoTokenizado("Bandeira de teste", "0012", true).ultimosQuatro)
        assertThrows(IllegalArgumentException::class.java) {
            ResumoCartaoTokenizado("Bandeira de teste", "0000000000000000", false)
        }
    }

    private fun estadoCom(papeis: Set<PapelUsuario>) = Estado(
        tela = Tela.HOME, usuario = Usuario("usuario-test", "teste@example.test"),
        perfil = CadastroUsuario(null, null, SituacaoTelefone.NAO_INFORMADO, emptyList(), papeis, Remuneracao(), null)
    )
}
