package br.com.hubdeservicos.android.auth

import java.io.IOException

interface ArmazenamentoSessao {
    fun ler(): String?
    fun gravar(atualizacao: String)
    fun apagar()
}

// As operações desta classe são chamadas em uma única fila de trabalho.
class SessaoApp(private val api: AutenticacaoApi, private val armazenamento: ArmazenamentoSessao) {
    private var tokenAcesso: String? = null

    fun cadastrar(email: String, senha: String) = api.cadastrar(email, senha)

    fun entrar(email: String, senha: String): Usuario {
        val acesso = api.entrar(email, senha)
        guardar(acesso.tokens)
        return acesso.usuario
    }

    fun restaurar(): Usuario? {
        tokenAcesso?.let { atual ->
            try {
                return api.consultar(atual)
            } catch (falha: FalhaApi) {
                if (falha.status != 401) throw falha
                tokenAcesso = null
            }
        }
        val acesso = renovar() ?: return null
        return try {
            api.consultar(acesso)
        } catch (falha: FalhaApi) {
            if (falha.status != 401) throw falha
            invalidar()
            null
        }
    }

    fun sair() {
        val atual = tokenAcesso ?: renovar() ?: return
        try {
            api.sair(atual)
        } catch (falha: FalhaApi) {
            if (falha.status != 401) throw falha
            val novo = renovar() ?: return
            try {
                api.sair(novo)
            } catch (segundaFalha: FalhaApi) {
                if (segundaFalha.status != 401) throw segundaFalha
            }
        }
        invalidar()
    }

    private fun renovar(): String? {
        val anterior = armazenamento.ler() ?: return null
        val tokens = try {
            api.renovar(anterior)
        } catch (falha: FalhaApi) {
            if (falha.status != 401) throw falha
            invalidar()
            return null
        }
        guardar(tokens)
        return tokens.acesso
    }

    private fun guardar(tokens: Tokens) {
        try {
            armazenamento.gravar(tokens.atualizacao)
        } catch (falha: IOException) {
            // O servidor já emitiu a sessão, mesmo se a gravação local falhar.
            runCatching { api.sair(tokens.acesso) }
            throw falha
        }
        tokenAcesso = tokens.acesso
    }

    private fun invalidar() {
        tokenAcesso = null
        armazenamento.apagar()
    }
}
