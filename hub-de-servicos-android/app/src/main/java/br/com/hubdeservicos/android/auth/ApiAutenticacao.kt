package br.com.hubdeservicos.android.auth

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import org.json.JSONException
import org.json.JSONObject

data class Usuario(val id: String, val email: String)

class Tokens(val acesso: String, val atualizacao: String)
class Acesso(val tokens: Tokens, val usuario: Usuario)

class FalhaApi(val status: Int) : IOException()

interface AutenticacaoApi {
    fun cadastrar(email: String, senha: String): Usuario
    fun entrar(email: String, senha: String): Acesso
    fun renovar(atualizacao: String): Tokens
    fun consultar(acesso: String): Usuario
    fun sair(acesso: String)
}

class ApiAutenticacao(origem: String) : AutenticacaoApi {
    private val base = origem.trimEnd('/').takeIf { it.isNotBlank() }?.plus("/api/v1/app/autenticacao")

    override fun cadastrar(email: String, senha: String): Usuario =
        usuario(requisicao("POST", "/cadastro", credenciais(email, senha)))

    override fun entrar(email: String, senha: String): Acesso {
        val resposta = requisicao("POST", "/login", credenciais(email, senha))
        return Acesso(tokens(resposta), usuario(resposta.getJSONObject("usuario")))
    }

    override fun renovar(atualizacao: String): Tokens =
        tokens(requisicao("POST", "/renovar", JSONObject().put("tokenAtualizacao", atualizacao)))

    override fun consultar(acesso: String): Usuario =
        usuario(requisicao("GET", "/eu", acesso = acesso))

    override fun sair(acesso: String) {
        requisicao("POST", "/logout", acesso = acesso)
    }

    private fun requisicao(
        metodo: String,
        rota: String,
        corpo: JSONObject? = null,
        acesso: String? = null
    ): JSONObject {
        val url = base ?: throw IllegalStateException("Origem da API ausente")
        val conexao = URI.create(url + rota).toURL().openConnection() as HttpURLConnection
        try {
            conexao.requestMethod = metodo
            conexao.connectTimeout = 8_000
            conexao.readTimeout = 8_000
            conexao.instanceFollowRedirects = false
            conexao.setRequestProperty("Accept", "application/json, application/problem+json")
            if (acesso != null) conexao.setRequestProperty("Authorization", "Bearer $acesso")
            if (corpo != null) {
                conexao.doOutput = true
                conexao.setRequestProperty("Content-Type", "application/json")
                val bytes = corpo.toString().toByteArray(Charsets.UTF_8)
                conexao.setFixedLengthStreamingMode(bytes.size)
                conexao.outputStream.use { it.write(bytes) }
            }
            val status = conexao.responseCode
            if (status !in 200..299) throw FalhaApi(status)
            if (status == HttpURLConnection.HTTP_NO_CONTENT) return JSONObject()
            return conexao.inputStream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
        } finally {
            conexao.disconnect()
        }
    }

    private fun credenciais(email: String, senha: String) =
        JSONObject().put("email", email).put("senha", senha)

    private fun usuario(json: JSONObject) = Usuario(json.texto("id"), json.texto("email"))

    private fun tokens(json: JSONObject) = Tokens(json.texto("tokenAcesso"), json.texto("tokenAtualizacao"))

    private fun JSONObject.texto(chave: String): String =
        getString(chave).takeIf { it.isNotBlank() } ?: throw JSONException("Campo ausente na resposta")
}
