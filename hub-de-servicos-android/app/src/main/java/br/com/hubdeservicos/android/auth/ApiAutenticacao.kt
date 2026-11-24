package br.com.hubdeservicos.android.auth

import br.com.hubdeservicos.android.rede.comConexaoHttp
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Usuario(val id: String, val email: String)

class Tokens(val acesso: String, val atualizacao: String)
class Acesso(val tokens: Tokens, val usuario: Usuario)

class FalhaApi(
    val status: Int,
    val senhaForaDoLimite: Boolean = false,
    val mensagemPerfil: String? = null,
    val camposInvalidos: List<String> = emptyList(),
    val completude: CompletudeCredenciamento? = null,
    val codigo: String? = null,
    val aprovado: Boolean = false
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
    fun atualizarCadastro(acesso: String, nome: String, telefone: String, remuneracao: Remuneracao? = null): CadastroUsuario
    fun selecionarPapel(acesso: String, papel: PapelUsuario, selecionado: Boolean): CadastroUsuario
    fun registrarMunicipio(acesso: String, cidade: String, estado: String, confirmada: Boolean = false): CadastroUsuario
    fun pesquisarMunicipios(acesso: String, nome: String): List<Municipio>
    fun selecionarMunicipio(acesso: String, codigoIbge: Int): CadastroUsuario
    fun iniciarConfirmacao(acesso: String, telefone: String): DesafioTelefone
    fun reenviarConfirmacao(acesso: String): DesafioTelefone
    fun confirmarTelefone(acesso: String, idDesafio: String, codigo: String): CadastroUsuario
}

class ApiAutenticacao(origem: String) : AutenticacaoApi, PerfilApi, CredenciamentoApi {
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

    override fun atualizarCadastro(acesso: String, nome: String, telefone: String, remuneracao: Remuneracao?) =
        atualizarPerfil("", JSONObject().put("nome", nome).put("telefone", telefone)
            .put("remuneracao", remuneracao?.json()), acesso)

    override fun selecionarPapel(acesso: String, papel: PapelUsuario, selecionado: Boolean) =
        atualizarPerfil("/papeis/${papel.name}", JSONObject().put("selecionado", selecionado), acesso)

    override fun registrarMunicipio(acesso: String, cidade: String, estado: String, confirmada: Boolean) =
        atualizarPerfil("/localizacao",
            JSONObject().put("cidade", cidade).put("estado", estado).put("confirmada", confirmada), acesso)

    override fun pesquisarMunicipios(acesso: String, nome: String): List<Municipio> {
        val termo = URLEncoder.encode(nome, "UTF-8")
        val itens = JSONArray(resposta("GET", "/perfil/municipios?nome=$termo", acesso = acesso, perfil = true))
        return List(itens.length()) { Municipio.de(itens.getJSONObject(it)) }
    }

    override fun selecionarMunicipio(acesso: String, codigoIbge: Int) =
        atualizarPerfil("/localizacao/manual", JSONObject().put("codigoIbge", codigoIbge), acesso)

    private fun atualizarPerfil(sufixo: String, dados: JSONObject, acesso: String) =
        CadastroUsuario.de(requisicao("PUT", "/perfil$sufixo", dados, acesso, perfil = true))

    override fun iniciarConfirmacao(acesso: String, telefone: String) =
        DesafioTelefone.de(requisicao("POST", "/perfil/telefone/confirmacao",
            JSONObject().put("telefone", telefone), acesso, perfil = true))

    override fun reenviarConfirmacao(acesso: String) =
        DesafioTelefone.de(requisicao("POST", "/perfil/telefone/confirmacao/reenviar", acesso = acesso, perfil = true))

    override fun confirmarTelefone(acesso: String, idDesafio: String, codigo: String) =
        CadastroUsuario.de(requisicao("POST", "/perfil/telefone/confirmacao/validar",
            JSONObject().put("idDesafio", idDesafio).put("codigo", codigo), acesso, perfil = true))

    override fun consultarCredenciamento(acesso: String, provedor: ProvedorCredenciamento): RascunhoCredenciamento? =
        try {
            RascunhoCredenciamento.de(requisicao("GET", "/credenciamentos/${provedor.name}", acesso = acesso, perfil = true))
        } catch (falha: FalhaApi) {
            if (falha.status != 404) throw falha
            null
        }

    override fun salvarCredenciamento(acesso: String, provedor: ProvedorCredenciamento, novo: Boolean,
                                      dados: RascunhoCredenciamento, confirmarAlteracaoAprovada: Boolean?): RascunhoCredenciamento =
        RascunhoCredenciamento.de(requisicao(if (novo) "POST" else "PUT", "/credenciamentos/${provedor.name}",
            dados.json(), acesso, perfil = true, cabecalhos = cabecalhosCredenciamento(dados, confirmarAlteracaoAprovada)))

    override fun excluirCredenciamento(acesso: String, provedor: ProvedorCredenciamento, dados: RascunhoCredenciamento) {
        requisicao("DELETE", "/credenciamentos/${provedor.name}", acesso = acesso, perfil = true,
            cabecalhos = cabecalhosCredenciamento(dados))
    }

    override fun validarCredenciamento(acesso: String, provedor: ProvedorCredenciamento,
                                      dados: RascunhoCredenciamento): CompletudeCredenciamento =
        CompletudeCredenciamento.de(requisicao("POST", "/credenciamentos/${provedor.name}/validacao",
            dados.json(), acesso, perfil = true))

    override fun consultarRecebedor(acesso: String, provedor: ProvedorCredenciamento) =
        RecebedorCredenciamento.de(requisicao("GET", "/credenciamentos/${provedor.name}/recebedor", acesso = acesso, perfil = true))

    override fun enviarRecebedor(acesso: String, provedor: ProvedorCredenciamento, ambiente: AmbienteRecebedor,
                                 dados: RascunhoCredenciamento) =
        RecebedorCredenciamento.de(requisicao("POST", "/credenciamentos/${provedor.name}/recebedor",
            JSONObject().put("ambiente", ambiente.name).put("dados", dados.json()), acesso, perfil = true,
            cabecalhos = cabecalhosCredenciamento(dados)))

    override fun reenviarRecebedor(acesso: String, provedor: ProvedorCredenciamento, recebedor: RecebedorCredenciamento) =
        RecebedorCredenciamento.de(requisicao("POST", "/credenciamentos/${provedor.name}/recebedor/reenvio",
            JSONObject().put("ambiente", recebedor.ambiente.name).put("versao", requireNotNull(recebedor.versao))
                .put("tentativa", requireNotNull(recebedor.tentativa)), acesso, perfil = true))

    override fun atualizarRecebedor(acesso: String, provedor: ProvedorCredenciamento) =
        RecebedorCredenciamento.de(requisicao("POST", "/credenciamentos/${provedor.name}/recebedor/consulta", acesso = acesso, perfil = true))

    private fun cabecalhosCredenciamento(dados: RascunhoCredenciamento, confirmarAlteracaoAprovada: Boolean? = null) = buildMap {
        dados.versao?.let { put("X-Versao-Credenciamento", it.toString()) }
        dados.id?.let { put("X-Credenciamento-Id", it) }
        confirmarAlteracaoAprovada?.let { aprovada ->
            put("X-Confirmar-Alteracao-Credenciamento", if (aprovada) "APROVACAO" else "SUBMISSAO")
        }
    }

    private fun requisicao(
        metodo: String,
        rota: String,
        corpo: JSONObject? = null,
        acesso: String? = null,
        perfil: Boolean = false,
        cabecalhos: Map<String, String> = emptyMap()
    ): JSONObject {
        return JSONObject(resposta(metodo, rota, corpo, acesso, perfil, cabecalhos))
    }

    private fun resposta(
        metodo: String, rota: String, corpo: JSONObject? = null, acesso: String? = null, perfil: Boolean = false,
        cabecalhos: Map<String, String> = emptyMap()
    ): String {
        val url = base ?: throw IllegalStateException("Origem da API ausente")
        val tempoLeituraMs = if (metodo == "POST" && rota.startsWith("/credenciamentos/") &&
            (rota.endsWith("/recebedor") || rota.endsWith("/recebedor/consulta") || rota.endsWith("/recebedor/reenvio"))) 25_000 else 8_000
        return comConexaoHttp(url + (if (perfil) "" else "/autenticacao") + rota, tempoLeituraMs) { conexao ->
            conexao.requestMethod = metodo
            conexao.setRequestProperty("Accept", "application/json, application/problem+json")
            if (acesso != null) conexao.setRequestProperty("Authorization", "Bearer $acesso")
            cabecalhos.forEach(conexao::setRequestProperty)
            if (corpo != null) {
                conexao.doOutput = true
                conexao.setRequestProperty("Content-Type", "application/json")
                val bytes = corpo.toString().toByteArray(Charsets.UTF_8)
                conexao.setFixedLengthStreamingMode(bytes.size)
                conexao.outputStream.use { it.write(bytes) }
            }
            val status = conexao.responseCode
            if (status !in 200..299) {
                val problema = if (perfil || (status == 400 && rota == "/cadastro")) runCatching {
                    conexao.errorStream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        JSONObject(reader.readText())
                    }
                }.getOrNull() else null
                val detalhe = problema?.optString("detail")?.takeIf { it.isNotBlank() }
                val campos = if (rota.startsWith("/credenciamentos/"))
                    problema?.optJSONArray("camposInvalidos")?.let { lista ->
                        List(lista.length()) { indice -> lista.optString(indice) }
                    }.orEmpty() else emptyList()
                throw FalhaApi(status, !perfil && detalhe?.contains("72 bytes", ignoreCase = true) == true,
                    detalhe.takeIf { perfil && (status in setOf(400, 409, 410, 422, 429, 502)
                        || rota.startsWith("/credenciamentos/") && status in setOf(403, 503)) }, campos,
                    problema?.optJSONObject("completude")?.let(CompletudeCredenciamento::de),
                    problema?.optString("codigo")?.takeIf { it.isNotBlank() },
                    problema?.optBoolean("aprovado") == true)
            }
            if (status == HttpURLConnection.HTTP_NO_CONTENT) "{}"
            else conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }

    private fun credenciais(email: String, senha: String) =
        JSONObject().put("email", email).put("senha", senha)

    private fun usuario(json: JSONObject) = Usuario(json.texto("id"), json.texto("email"))

    private fun tokens(json: JSONObject) = Tokens(json.texto("tokenAcesso"), json.texto("tokenAtualizacao"))

    private fun JSONObject.texto(chave: String): String =
        getString(chave).takeIf { it.isNotBlank() } ?: throw JSONException("Campo ausente na resposta")
}
