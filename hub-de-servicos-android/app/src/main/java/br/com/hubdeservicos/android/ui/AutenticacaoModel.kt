package br.com.hubdeservicos.android.ui

import android.os.Handler
import android.os.Looper
import br.com.hubdeservicos.android.auth.FalhaApi
import br.com.hubdeservicos.android.auth.SessaoApp
import br.com.hubdeservicos.android.auth.Usuario
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONException

enum class Tela { RESTAURAR, LOGIN, CADASTRO, CONTA }

data class Estado(
    val tela: Tela = Tela.RESTAURAR,
    val email: String = "",
    val usuario: Usuario? = null,
    val carregando: Boolean = false,
    val mensagem: String? = null,
    val erro: String? = null
)

class AutenticacaoModel(private val sessao: SessaoApp) {
    private val fila = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())
    private var observador: ((Estado) -> Unit)? = null
    private var estado = Estado()

    init {
        restaurar()
    }

    fun observar(novoObservador: ((Estado) -> Unit)?) {
        observador = novoObservador
        novoObservador?.invoke(estado)
    }

    fun editarEmail(email: String) {
        estado = estado.copy(email = email)
    }

    fun mudarTela(tela: Tela) {
        if (!estado.carregando) atualizar(Estado(tela, email = estado.email))
    }

    fun restaurar() = executar {
        val usuario = sessao.restaurar()
        if (usuario == null) Estado(Tela.LOGIN) else Estado(Tela.CONTA, usuario = usuario)
    }

    fun entrar(email: String, senha: String) = executar {
        Estado(Tela.CONTA, usuario = sessao.entrar(email, senha))
    }

    fun cadastrar(email: String, senha: String) = executar {
        val usuario = sessao.cadastrar(email, senha)
        Estado(Tela.LOGIN, email = usuario.email, mensagem = "Conta criada. Entre com seu e-mail e senha.")
    }

    fun sair() = executar {
        sessao.sair()
        Estado(Tela.LOGIN)
    }

    private fun executar(operacao: () -> Estado) {
        if (estado.carregando) return
        val anterior = estado
        atualizar(anterior.copy(carregando = true, mensagem = null, erro = null))
        fila.execute {
            val novoEstado = try {
                operacao()
            } catch (falha: FalhaApi) {
                anterior.copy(mensagem = null, erro = quandoFalha(falha, anterior.tela))
            } catch (_: IOException) {
                anterior.copy(mensagem = null, erro = "Não foi possível concluir. Verifique a conexão e tente novamente.")
            } catch (_: JSONException) {
                anterior.copy(mensagem = null, erro = "O serviço retornou uma resposta inesperada.")
            } catch (_: IllegalStateException) {
                anterior.copy(mensagem = null, erro = "Confira o endereço da API nesta versão do app.")
            }
            principal.post { atualizar(novoEstado) }
        }
    }

    private fun atualizar(novoEstado: Estado) {
        estado = novoEstado
        observador?.invoke(novoEstado)
    }

    private fun quandoFalha(falha: FalhaApi, tela: Tela): String = when (falha.status) {
        400 -> if (tela == Tela.CADASTRO) "Confira o e-mail e os requisitos da senha." else "Confira os dados informados."
        401 -> "E-mail ou senha inválidos."
        409 -> "Não foi possível criar a conta com esses dados. Se já tiver uma conta, tente entrar."
        429 -> "Muitas tentativas. Aguarde um minuto e tente novamente."
        else -> "O serviço está indisponível no momento. Tente novamente."
    }
}
