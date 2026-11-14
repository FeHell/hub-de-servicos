package br.com.hubdeservicos.android.auth

import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test

class ApiAutenticacaoTest {
    @Test
    fun usaRotasJsonEBearerDaApiMovel() {
        ServerSocket(0, 8, InetAddress.getLoopbackAddress()).use { servidor ->
            val executor = Executors.newSingleThreadExecutor()
            val usuario = """{"id":"id-1","email":"pessoa@example.test"}"""
            val tokens = """{"tokenAcesso":"jwt","tokenAtualizacao":"refresh"}"""
            val login = """{"tokenAcesso":"jwt","tokenAtualizacao":"refresh","usuario":$usuario}"""
            val prefixo = "/api/v1/app/autenticacao"
            val casos = listOf(
                Caso("POST $prefixo/cadastro", 201, usuario, campo = "email" to "pessoa@example.test"),
                Caso("POST $prefixo/login", 200, login, campo = "senha" to "senha-de-teste"),
                Caso("POST $prefixo/login", 401, "{}"),
                Caso("POST $prefixo/renovar", 200, tokens, campo = "tokenAtualizacao" to "refresh"),
                Caso("GET $prefixo/eu", 200, usuario, autenticado = true),
                Caso("POST $prefixo/logout", 204, "", autenticado = true)
            )
            val requisicoes = executor.submit {
                for (caso in casos) {
                    servidor.accept().use { socket ->
                        val leitura = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                        val rota = leitura.readLine().substringBeforeLast(' ')
                        val cabecalhos = generateSequence { leitura.readLine()?.takeUnless { it.isEmpty() } }
                            .associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
                        val tamanho = cabecalhos["content-length"]?.toInt() ?: 0
                        val corpo = CharArray(tamanho)
                        for (indice in corpo.indices) corpo[indice] = leitura.read().toChar()
                        val resposta = caso.resposta.toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().apply {
                            write(("HTTP/1.1 ${caso.status} Teste\r\n" +
                                "Content-Type: application/json\r\nContent-Length: ${resposta.size}\r\n" +
                                "Connection: close\r\n\r\n").toByteArray())
                            write(resposta)
                            flush()
                        }
                        assertEquals(caso.rota, rota)
                        caso.campo?.let { (nome, valor) ->
                            assertEquals(valor, JSONObject(String(corpo)).getString(nome))
                        }
                        if (caso.autenticado) assertEquals("Bearer jwt", cabecalhos["authorization"])
                    }
                }
            }
            try {
                val api = ApiAutenticacao("http://127.0.0.1:${servidor.localPort}")
                val email = "pessoa@example.test"
                assertEquals("id-1", api.cadastrar(email, "senha-de-teste").id)
                assertEquals(email, api.entrar(email, "senha-de-teste").usuario.email)
                assertEquals(401, assertThrows(FalhaApi::class.java) { api.entrar(email, "incorreta") }.status)
                assertEquals("jwt", api.renovar("refresh").acesso)
                assertEquals(email, api.consultar("jwt").email)
                api.sair("jwt")
                requisicoes.get(5, TimeUnit.SECONDS)
            } finally {
                servidor.close()
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun integraClienteComBackendDoHub() {
        val origem = System.getenv("HUB_ANDROID_API_TESTE")
        assumeTrue("Use uma instância do backend com dados de teste.", !origem.isNullOrBlank())
        val api = ApiAutenticacao(requireNotNull(origem))
        val email = "android-${UUID.randomUUID()}@example.test"
        val senha = "Senha-de-teste!1996"
        val criado = api.cadastrar(email, senha)
        assertEquals(email, criado.email)
        assertEquals(409, assertThrows(FalhaApi::class.java) { api.cadastrar(email, senha) }.status)
        val armazenamento = object : ArmazenamentoSessao {
            private var valor: String? = null
            override fun ler() = valor
            override fun gravar(atualizacao: String) { valor = atualizacao }
            override fun apagar() { valor = null }
        }
        assertEquals(criado, SessaoApp(api, armazenamento).entrar(email, senha))
        val sessaoRestaurada = SessaoApp(api, armazenamento)
        assertEquals(criado, sessaoRestaurada.restaurar())
        sessaoRestaurada.sair()
        assertEquals(null, SessaoApp(api, armazenamento).restaurar())
    }

    private data class Caso(
        val rota: String,
        val status: Int,
        val resposta: String,
        val campo: Pair<String, String>? = null,
        val autenticado: Boolean = false
    )
}
