package br.com.hubdeservicos.android.auth

import org.json.JSONObject

data class CadastroUsuario(
    val nome: String?,
    val telefone: String?,
    val situacaoTelefone: SituacaoTelefone,
    val pendencias: List<String>
) {
    companion object {
        internal fun de(json: JSONObject): CadastroUsuario {
            val pendencias = json.getJSONArray("pendencias")
            return CadastroUsuario(
                json.optString("nome").takeUnless { json.isNull("nome") },
                json.optString("telefone").takeUnless { json.isNull("telefone") },
                SituacaoTelefone.valueOf(json.getString("situacaoTelefone")),
                List(pendencias.length()) { pendencias.getString(it) }
            )
        }
    }
}

enum class SituacaoTelefone { NAO_INFORMADO, PENDENTE, VERIFICADO }

data class DesafioTelefone(val id: String, val telefoneMascarado: String, val cadastro: CadastroUsuario) {
    companion object {
        internal fun de(json: JSONObject) = DesafioTelefone(
            json.getString("idDesafio"), json.getString("telefoneMascarado"),
            CadastroUsuario.de(json.getJSONObject("cadastro"))
        )
    }
}
