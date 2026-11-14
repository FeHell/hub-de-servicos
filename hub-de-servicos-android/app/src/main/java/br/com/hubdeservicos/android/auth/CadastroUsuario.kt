package br.com.hubdeservicos.android.auth

import java.math.BigDecimal
import org.json.JSONObject

data class CadastroUsuario(
    val nome: String?,
    val telefone: String?,
    val situacaoTelefone: SituacaoTelefone,
    val pendencias: List<String>,
    val papeis: Set<PapelUsuario>,
    val remuneracao: Remuneracao,
    val municipio: Municipio?
) {
    companion object {
        internal fun de(json: JSONObject): CadastroUsuario {
            val pendencias = json.getJSONArray("pendencias")
            return CadastroUsuario(
                json.optString("nome").takeUnless { json.isNull("nome") },
                json.optString("telefone").takeUnless { json.isNull("telefone") },
                SituacaoTelefone.valueOf(json.getString("situacaoTelefone")),
                List(pendencias.length()) { pendencias.getString(it) },
                json.optJSONArray("papeis")?.let { lista ->
                    PapelUsuario.entries.filter { papel -> (0 until lista.length()).any { lista.getString(it) == papel.name } }.toSet()
                }.orEmpty(),
                json.optJSONObject("remuneracao").let {
                    Remuneracao(it?.decimal("valorHora"), it?.decimal("valorDiaria"))
                },
                json.optJSONObject("municipio")?.let {
                    Municipio(it.getInt("codigoIbge"), it.getString("nome"), it.getString("uf"))
                }
            )
        }
    }
}

enum class PapelUsuario { EMPRESA, TRABALHADOR }

data class Remuneracao(val valorHora: BigDecimal? = null, val valorDiaria: BigDecimal? = null) {
    internal fun json() = JSONObject()
        .put("valorHora", valorHora?.toPlainString() ?: JSONObject.NULL)
        .put("valorDiaria", valorDiaria?.toPlainString() ?: JSONObject.NULL)
}

// Os valores monetários são transportados como texto decimal em base 10.
private fun JSONObject.decimal(chave: String): BigDecimal? = if (isNull(chave)) null else getString(chave).toBigDecimal()

data class Municipio(val codigoIbge: Int, val nome: String, val uf: String)

enum class SituacaoTelefone { NAO_INFORMADO, PENDENTE, VERIFICADO }

data class DesafioTelefone(val id: String, val telefoneMascarado: String, val cadastro: CadastroUsuario) {
    companion object {
        internal fun de(json: JSONObject) = DesafioTelefone(
            json.getString("idDesafio"), json.getString("telefoneMascarado"),
            CadastroUsuario.de(json.getJSONObject("cadastro"))
        )
    }
}
