package br.com.hubdeservicos.android.auth

import org.json.JSONObject

enum class ProvedorCredenciamento { STONE }
enum class ModalidadeCredenciamento { INDIVIDUAL, CORPORATION }

internal fun Municipio.camposEndereco(prefixo: String) = mapOf(
    "$prefixo.cidade" to nome, "$prefixo.uf" to uf
)

interface CredenciamentoApi {
    fun consultarCredenciamento(acesso: String, provedor: ProvedorCredenciamento): RascunhoCredenciamento?
    fun salvarCredenciamento(acesso: String, provedor: ProvedorCredenciamento, novo: Boolean,
                             dados: RascunhoCredenciamento, confirmarAlteracaoAprovada: Boolean? = null): RascunhoCredenciamento
    fun excluirCredenciamento(acesso: String, provedor: ProvedorCredenciamento, dados: RascunhoCredenciamento)
    fun validarCredenciamento(acesso: String, provedor: ProvedorCredenciamento,
                              dados: RascunhoCredenciamento): CompletudeCredenciamento
    fun consultarRecebedor(acesso: String, provedor: ProvedorCredenciamento): RecebedorCredenciamento
    fun enviarRecebedor(acesso: String, provedor: ProvedorCredenciamento, ambiente: AmbienteRecebedor,
                        dados: RascunhoCredenciamento): RecebedorCredenciamento
    fun reenviarRecebedor(acesso: String, provedor: ProvedorCredenciamento, recebedor: RecebedorCredenciamento): RecebedorCredenciamento
    fun atualizarRecebedor(acesso: String, provedor: ProvedorCredenciamento): RecebedorCredenciamento
}

data class PendenciaCredenciamento(val campo: String, val mensagem: String)

enum class AmbienteRecebedor { DEV, PROD }
enum class SituacaoRecebedor { NAO_ENVIADO, EM_ANALISE, FALHA, INDETERMINADO, CRIADO, AGUARDANDO_NOVA_SUBMISSAO }
enum class ErroRecebedor { DADOS_RECUSADOS, AUTENTICACAO, RESPOSTA_INDETERMINADA, ATUALIZACAO_RESTRITA }

data class RecebedorCredenciamento(
    val ambiente: AmbienteRecebedor,
    val situacao: SituacaoRecebedor,
    val statusProvedor: String?,
    val erro: ErroRecebedor?,
    val pendencias: List<PendenciaCredenciamento>,
    val versao: Long? = null,
    val podeEnviar: Boolean = false,
    val podeConsultar: Boolean = false,
    val orientacao: String? = null,
    val podeReenviar: Boolean = false,
    val emProcessamento: Boolean = false,
    val envioAceito: Boolean = false,
    val tentativa: String? = null
) {
    val aprovado: Boolean
        get() = situacao == SituacaoRecebedor.CRIADO && statusProvedor == "ACTIVE"

    companion object {
        internal fun de(json: JSONObject) = RecebedorCredenciamento(
            AmbienteRecebedor.valueOf(json.getString("ambiente")),
            SituacaoRecebedor.valueOf(json.getString("situacao")),
            json.optString("statusProvedor").takeIf { !json.isNull("statusProvedor") },
            ErroRecebedor.entries.firstOrNull { it.name == json.optString("erro") },
            json.pendencias("pendencias"),
            json.optLong("versao").takeUnless { json.isNull("versao") },
            json.optBoolean("podeEnviar"),
            json.optBoolean("podeConsultar"),
            json.optString("orientacao").takeUnless { json.isNull("orientacao") },
            json.optBoolean("podeReenviar"),
            json.optBoolean("emProcessamento", json.optString("situacao") == SituacaoRecebedor.EM_ANALISE.name),
            json.optBoolean("envioAceito"),
            json.optString("tentativa").takeUnless { json.isNull("tentativa") }
        )
    }
}

data class CompletudeCredenciamento(val apto: Boolean, val pendencias: List<PendenciaCredenciamento>) {
    companion object {
        internal fun de(json: JSONObject): CompletudeCredenciamento = CompletudeCredenciamento(
            json.getBoolean("apto"), listOf("faltantes", "invalidos", "confirmacoes").flatMap(json::pendencias))
    }
}

private fun JSONObject.pendencias(grupo: String): List<PendenciaCredenciamento> {
    val lista = getJSONArray(grupo)
    return List(lista.length()) { indice ->
        val item = lista.getJSONObject(indice)
        PendenciaCredenciamento(item.getString("campo"), item.getString("mensagem"))
    }
}

class RascunhoCredenciamento(
    campos: Map<String, String> = emptyMap(),
    val completude: CompletudeCredenciamento? = null,
    val alterado: Boolean = false,
    val versao: Long? = null,
    val recebedor: RecebedorCredenciamento? = null,
    val pendenteSalvar: Boolean = false,
    val id: String? = null
) {
    // Descarta o campo removido também ao recuperar rascunhos de versões anteriores.
    private val campos = campos.filterKeys { it.substringAfterLast('.') !in setOf("nomeMae", "mother_name") }

    operator fun get(campo: String): String = campos[campo].orEmpty()

    val modalidade: ModalidadeCredenciamento?
        get() = ModalidadeCredenciamento.entries.firstOrNull { it.name == get("modalidade") }

    fun atualizar(novos: Map<String, String>): RascunhoCredenciamento {
        val aceitos = novos.filterKeys { it.substringAfterLast('.') !in setOf("nomeMae", "mother_name") }
        val atualizado = campos + aceitos
        if (aceitos.all { (campo, valor) -> get(campo) == valor }) return this
        val mesmaModalidade = atualizado["modalidade"] == campos["modalidade"]
        return RascunhoCredenciamento(atualizado, completude.takeIf { mesmaModalidade }, true, versao, recebedor, true, id)
    }

    fun validar(resultado: CompletudeCredenciamento) = RascunhoCredenciamento(campos, resultado, false, versao, recebedor, pendenteSalvar, id)

    internal fun comTelefoneSugerido(telefone: String?): RascunhoCredenciamento =
        if (modalidade != ModalidadeCredenciamento.CORPORATION || campos.containsKey("empresa.telefone") ||
            telefone.isNullOrBlank()) this
        else atualizar(mapOf("empresa.telefone" to telefone))

    internal fun json(): JSONObject {
        val resultado = JSONObject()
        for ((campo, valor) in campos) {
            val partes = campo.split('.')
            val enviar = when (partes.first()) {
                "pessoaFisica" -> modalidade == ModalidadeCredenciamento.INDIVIDUAL
                "empresa", "representante" -> modalidade != ModalidadeCredenciamento.INDIVIDUAL
                else -> true
            }
            if (!enviar) continue
            val destino = partes.dropLast(1).fold(resultado) { objeto, parte ->
                objeto.optJSONObject(parte) ?: JSONObject().also { objeto.put(parte, it) }
            }
            destino.put(partes.last(), when {
                valor.isBlank() -> JSONObject.NULL
                campo == "representante.representanteLegal" || campo == "representante.socioQsa" -> valor == "true"
                else -> valor
            })
        }
        return resultado
    }

    companion object {
        internal fun de(resposta: JSONObject): RascunhoCredenciamento {
            val campos = mutableMapOf<String, String>()
            fun extrair(objeto: JSONObject, prefixo: String) {
                objeto.keys().forEach { chave ->
                    val valor = objeto.opt(chave)
                    val campo = prefixo + chave
                    if (valor is JSONObject) extrair(valor, "$campo.")
                    else if (valor != null && valor != JSONObject.NULL) campos[campo] = valor.toString()
                }
            }
            extrair(resposta.getJSONObject("dados"), "")
            if ("modalidade" !in campos && "empresa.cnpj" in campos) {
                campos["modalidade"] = ModalidadeCredenciamento.CORPORATION.name
            }
            return RascunhoCredenciamento(campos, resposta.optJSONObject("completude")?.let(CompletudeCredenciamento::de),
                versao = resposta.optLong("versao").takeUnless { resposta.isNull("versao") },
                recebedor = resposta.optJSONObject("recebedor")?.let(RecebedorCredenciamento::de),
                id = resposta.optString("id").takeUnless { resposta.isNull("id") })
        }
    }
}
