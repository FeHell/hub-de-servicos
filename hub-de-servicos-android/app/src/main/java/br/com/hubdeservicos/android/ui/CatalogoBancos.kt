package br.com.hubdeservicos.android.ui

import java.io.Reader
import java.text.Normalizer
import java.util.Locale

internal data class Banco(val codigo: String, val nome: String)

internal class CatalogoBancos(arquivo: Reader) {
    private val indice = arquivo.buffered().useLines { linhas ->
        linhas.map { linha ->
            val (codigo, nome, nomeReduzido) = linha.split('\t')
            Banco(codigo, nome) to normalizarBusca("$codigo $nome $nomeReduzido")
        }.toList()
    }
    private val porCodigo = indice.associate { (banco, _) -> banco.codigo to banco }

    fun consultar(codigo: String): Banco? = porCodigo[codigo]

    fun pesquisar(termo: String): List<Banco> {
        val consulta = normalizarBusca(termo)
        if (consulta.matches(codigoBancario)) return listOfNotNull(consultar(consulta))
        val palavras = consulta.split(' ').filter(String::isNotEmpty)
        return indice.filter { (_, texto) -> palavras.all { it in texto } }.map { (banco, _) -> banco }
    }

    private companion object {
        val codigoBancario = Regex("[0-9]{3}")
        val diacriticos = Regex("\\p{M}")
        val separadores = Regex("[^a-z0-9]+")

        fun normalizarBusca(texto: String): String = Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(diacriticos, "")
            .replace(separadores, " ")
            .trim()
    }
}
