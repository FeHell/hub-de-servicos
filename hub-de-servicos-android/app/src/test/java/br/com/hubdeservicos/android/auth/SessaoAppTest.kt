package br.com.hubdeservicos.android.auth

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SessaoAppTest {
    @Test
    fun cadastroDeixaSessaoParaOLogin() {
        val api = ApiDeTeste()
        val armazenamento = Memoria()
        val sessao = SessaoApp(api, armazenamento)
        assertEquals(api.usuario, sessao.cadastrar(api.usuario.email, "senha"))
        assertNull(armazenamento.token)
    }

    @Test
    fun loginERestauracaoDeNovoProcessoRotacionamToken() {
        val api = ApiDeTeste()
        val armazenamento = Memoria()
        assertEquals(api.usuario, SessaoApp(api, armazenamento).entrar(api.usuario.email, "senha"))
        assertEquals("refresh-login", armazenamento.token)

        assertEquals(api.usuario, SessaoApp(api, armazenamento).restaurar())
        assertEquals("refresh-login", api.refreshRecebido)
        assertEquals("refresh-novo", armazenamento.token)
        assertEquals("jwt-novo", api.acessoConsultado)
    }

    @Test
    fun reabrirComTokenExpiradoRenovaSessao() {
        val api = ApiDeTeste()
        val armazenamento = Memoria()
        val sessao = SessaoApp(api, armazenamento)
        sessao.entrar(api.usuario.email, "senha")
        api.acessoExpirado = "jwt-login"
        assertEquals(api.usuario, sessao.restaurar())
        assertEquals("refresh-login", api.refreshRecebido)
        assertEquals("refresh-novo", armazenamento.token)
    }

    @Test
    fun refreshRevogadoRetornaAoLoginELimpaPersistencia() {
        val api = ApiDeTeste().apply { erroRenovacao = FalhaApi(401) }
        val armazenamento = Memoria("revogado")
        assertNull(SessaoApp(api, armazenamento).restaurar())
        assertNull(armazenamento.token)
    }

    @Test
    fun conexaoIndisponivelMantemRefreshParaNovaTentativa() {
        val api = ApiDeTeste().apply { erroRenovacao = IOException() }
        val armazenamento = Memoria("anterior")
        assertThrows(IOException::class.java) { SessaoApp(api, armazenamento).restaurar() }
        assertEquals("anterior", armazenamento.token)
    }

    @Test
    fun logoutComJwtExpiradoRenovaRevogaELimpaPersistencia() {
        val api = ApiDeTeste()
        val armazenamento = Memoria()
        val sessao = SessaoApp(api, armazenamento)
        sessao.entrar(api.usuario.email, "senha")
        api.acessoExpirado = "jwt-login"
        sessao.sair()
        assertEquals(listOf("jwt-login", "jwt-novo"), api.saidas)
        assertNull(armazenamento.token)
    }

    private class Memoria(var token: String? = null) : ArmazenamentoSessao {
        override fun ler() = token
        override fun gravar(atualizacao: String) { token = atualizacao }
        override fun apagar() { token = null }
    }

    private class ApiDeTeste : AutenticacaoApi {
        val usuario = Usuario("id", "pessoa@example.test")
        var erroRenovacao: IOException? = null
        var refreshRecebido: String? = null
        var acessoConsultado: String? = null
        var acessoExpirado: String? = null
        val saidas = mutableListOf<String>()

        override fun cadastrar(email: String, senha: String) = usuario
        override fun entrar(email: String, senha: String) = Acesso(Tokens("jwt-login", "refresh-login"), usuario)
        override fun renovar(atualizacao: String): Tokens {
            refreshRecebido = atualizacao
            erroRenovacao?.let { throw it }
            return Tokens("jwt-novo", "refresh-novo")
        }
        override fun consultar(acesso: String): Usuario {
            if (acesso == acessoExpirado) throw FalhaApi(401)
            acessoConsultado = acesso
            return usuario
        }
        override fun sair(acesso: String) {
            saidas.add(acesso)
            if (acesso == acessoExpirado) throw FalhaApi(401)
        }
    }
}
