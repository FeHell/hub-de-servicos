package br.com.hubdeservicos.android.rede

import java.net.HttpURLConnection
import java.net.URI

internal inline fun <T> comConexaoHttp(url: String, tempoLeituraMs: Int = 8_000, usar: (HttpURLConnection) -> T): T {
    val conexao = URI.create(url).toURL().openConnection() as HttpURLConnection
    try {
        conexao.connectTimeout = 8_000
        conexao.readTimeout = tempoLeituraMs
        conexao.instanceFollowRedirects = false
        return usar(conexao)
    } finally {
        conexao.disconnect()
    }
}
