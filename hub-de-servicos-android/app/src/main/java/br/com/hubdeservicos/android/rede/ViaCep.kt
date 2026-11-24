package br.com.hubdeservicos.android.rede

import br.com.hubdeservicos.android.auth.Municipio
import java.io.IOException
import java.net.HttpURLConnection
import org.json.JSONException
import org.json.JSONObject

internal fun cepParaConsulta(valor: String): String? =
    valor.takeIf { it.matches(Regex("[0-9]{5}-?[0-9]{3}")) }?.replace("-", "")

internal sealed interface ResultadoCep {
    data class Encontrado(
        val logradouro: String, val bairro: String, val complemento: String, val municipio: Municipio?
    ) : ResultadoCep
    data object Invalido : ResultadoCep
    data object NaoEncontrado : ResultadoCep
    data object Indisponivel : ResultadoCep
}

internal class ViaCep(private val buscar: (String) -> String = ::consultarViaCep) {
    private val recentes = linkedMapOf<String, ResultadoCep>()

    // Chamado na fila de consulta de endereços.
    fun consultar(valor: String): ResultadoCep {
        val cep = cepParaConsulta(valor) ?: return ResultadoCep.Invalido
        recentes[cep]?.let { return it }
        val resultado = try {
            interpretar(cep, JSONObject(buscar(cep)))
        } catch (_: IOException) {
            ResultadoCep.Indisponivel
        } catch (_: JSONException) {
            ResultadoCep.Indisponivel
        }
        if (resultado is ResultadoCep.Encontrado || resultado == ResultadoCep.NaoEncontrado) {
            if (recentes.size >= 16) recentes.remove(recentes.keys.first())
            recentes[cep] = resultado
        }
        return resultado
    }

    private fun interpretar(cep: String, resposta: JSONObject): ResultadoCep {
        if (resposta.optBoolean("erro")) return ResultadoCep.NaoEncontrado
        fun texto(chave: String) = if (resposta.isNull(chave)) "" else resposta.optString(chave).trim()
        if (cepParaConsulta(texto("cep")) != cep) return ResultadoCep.Indisponivel
        val nome = texto("localidade")
        val uf = texto("uf")
        val codigoIbge = texto("ibge").takeIf { it.matches(Regex("[1-9][0-9]{6}")) }?.toIntOrNull()
        val municipio = if (codigoIbge != null && nome.isNotBlank() && uf.matches(Regex("[A-Z]{2}")))
            Municipio(codigoIbge, nome, uf) else null
        return ResultadoCep.Encontrado(texto("logradouro"), texto("bairro"), texto("complemento"), municipio)
    }
}

private fun consultarViaCep(cep: String): String = comConexaoHttp("https://viacep.com.br/ws/$cep/json/") { conexao ->
    conexao.setRequestProperty("Accept", "application/json")
    if (conexao.responseCode != HttpURLConnection.HTTP_OK) throw IOException()
    conexao.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
