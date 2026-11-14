package br.com.hubdeservicos.android.auth

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import org.json.JSONException
import org.json.JSONObject

data class Usuario(val id: String, val email: String)

class Tokens(val acesso: String, val atualizacao: String)
class Acesso(val tokens: Tokens, val usuario: Usuario)

class FalhaApi(
    val status: Int,
    val senhaForaDoLimite: Boolean = false,
    val mensagemPerfil: String? = null
) : IOException()

interface AutenticacaoApi {
    fun cadastrar(email: String, senha: String): Usuario
    fun entrar(email: String, senha: String): Acesso
    fun renovar(atualizacao: String): Tokens
    fun consultar(acesso: String): Usuario
    fun sair(acesso: String)
}

interface PerfilApi {
    fun consultarCadastro(acesso: String): CadastroUsuario
    fun atualizarCadastro(acesso: String, nome: String, telefone: String): CadastroUsuario
    fun iniciarConfirmacao(acesso: String, telefone: String): DesafioTelefone
    fun reenviarConfirmacao(acesso: String): DesafioTelefone
    fun confirmarTelefone(acesso: String, idDesafio: String, codigo: String): CadastroUsuario
}

class ApiAutenticacao(origem: String) : AutenticacaoApi, PerfilApi {
    private val base = origem.trimEnd('/').takeIf { it.isNotBlank() }?.plus("/api/v1/app")

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

    override fun consultarCadastro(acesso: String) =
        CadastroUsuario.de(requisicao("GET", "/perfil", acesso = acesso, perfil = true))

    override fun atualizarCadastro(acesso: String, nome: String, telefone: String) =
        CadastroUsuario.de(requisicao("PUT", "/perfil", JSONObject().put("nome", nome).put("telefone", telefone),
            acesso, perfil = true))

    override fun iniciarConfirmacao(acesso: String, telefone: String) =
        DesafioTelefone.de(requisicao("POST", "/perfil/telefone/confirmacao",
            JSONObject().put("telefone", telefone), acesso, perfil = true))

    override fun reenviarConfirmacao(acesso: String) =
        DesafioTelefone.de(requisicao("POST", "/perfil/telefone/confirmacao/reenviar", acesso = acesso, perfil = true))

    override fun confirmarTelefone(acesso: String, idDesafio: String, codigo: String) =
        CadastroUsuario.de(requisicao("POST", "/perfil/telefone/confirmacao/validar",
            JSONObject().put("idDesafio", idDesafio).put("codigo", codigo), acesso, perfil = true))

    private fun requisicao(
        metodo: String,
        rota: String,
        corpo: JSONObject? = null,
        acesso: String? = null,
        perfil: Boolean = false
    ): JSONObject {
        val url = base ?: throw IllegalStateException("Origem da API ausente")
        val conexao = URI.create(url + (if (perfil) "" else "/autenticacao") + rota).toURL().openConnection() as HttpURLConnection
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
            if (status !in 200..299) {
                val detalhe = if (perfil || (status == 400 && rota == "/cadastro")) runCatching {
                    conexao.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        JSONObject(reader.readText()).optString("detail").takeIf { it.isNotBlank() }
                    }
                }.getOrNull() else null
                throw FalhaApi(status, !perfil && detalhe?.contains("72 bytes", ignoreCase = true) == true,
                    detalhe.takeIf { perfil && status in setOf(400, 409, 410, 429, 502) })
            }
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
