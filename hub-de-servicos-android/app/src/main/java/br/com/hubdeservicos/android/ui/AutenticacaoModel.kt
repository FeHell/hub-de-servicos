package br.com.hubdeservicos.android.ui

import android.os.Handler
import android.os.Looper
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.FalhaApi
import br.com.hubdeservicos.android.auth.SessaoApp
import br.com.hubdeservicos.android.auth.Usuario
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONException

enum class Tela { RESTAURAR, LOGIN, CADASTRO, HOME }

data class Estado(
    val tela: Tela = Tela.RESTAURAR,
    val email: String = "",
    val usuario: Usuario? = null,
    val carregando: Boolean = false,
    val mensagem: Int? = null,
    val erro: Int? = null
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

    fun revalidarAoAbrir() {
        if (estado.tela == Tela.HOME && !estado.carregando) {
            atualizar(Estado())
            restaurar()
        }
    }

    fun restaurar() = executar {
        sessao.restaurar()?.let(::home) ?: Estado(Tela.LOGIN)
    }

    fun entrar(email: String, senha: String) = executar {
        home(sessao.entrar(email, senha))
    }

    fun cadastrar(email: String, senha: String) = executar {
        val conta = sessao.cadastrar(email, senha)
        try {
            home(sessao.entrar(conta.email, senha))
        } catch (falha: FalhaApi) {
            contaCriada(conta.email, quandoFalha(falha, Tela.LOGIN))
        } catch (_: IOException) {
            contaCriada(conta.email, R.string.erro_conexao)
        } catch (_: JSONException) {
            contaCriada(conta.email, R.string.erro_resposta)
        }
    }

    fun sair() = executar {
        sessao.sair()
        Estado(Tela.LOGIN)
    }

    private fun home(usuario: Usuario) = Estado(Tela.HOME, usuario = usuario)

    private fun contaCriada(email: String, erro: Int) =
        Estado(Tela.LOGIN, email = email, mensagem = R.string.conta_criada_login, erro = erro)

    private fun executar(operacao: () -> Estado) {
        if (estado.carregando) return
        val anterior = estado
        atualizar(anterior.copy(carregando = true, mensagem = null, erro = null))
        fila.execute {
            val novoEstado = try {
                operacao()
            } catch (falha: FalhaApi) {
                anterior.copy(erro = quandoFalha(falha, anterior.tela))
            } catch (_: IOException) {
                anterior.copy(erro = R.string.erro_conexao)
            } catch (_: JSONException) {
                anterior.copy(erro = R.string.erro_resposta)
            } catch (_: IllegalStateException) {
                anterior.copy(erro = R.string.erro_configuracao)
            }
            principal.post { atualizar(novoEstado) }
        }
    }

    private fun atualizar(novoEstado: Estado) {
        estado = novoEstado
        observador?.invoke(novoEstado)
    }

    private fun quandoFalha(falha: FalhaApi, tela: Tela): Int = when (falha.status) {
        400 -> when {
            falha.senhaForaDoLimite -> R.string.senha_regras
            tela == Tela.CADASTRO -> R.string.erro_dados_cadastro
            else -> R.string.erro_dados_login
        }
        401 -> if (tela == Tela.LOGIN) R.string.erro_credenciais else R.string.erro_sessao
        409 -> R.string.erro_cadastro_conflito
        429 -> R.string.erro_limite
        else -> R.string.erro_servico
    }
}
