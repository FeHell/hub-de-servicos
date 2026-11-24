package br.com.hubdeservicos.android.ui

import android.os.Handler
import android.os.Looper
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.CadastroUsuario
import br.com.hubdeservicos.android.auth.CredenciamentoApi
import br.com.hubdeservicos.android.auth.ModalidadeCredenciamento
import br.com.hubdeservicos.android.auth.ProvedorCredenciamento
import br.com.hubdeservicos.android.auth.RascunhoCredenciamento
import br.com.hubdeservicos.android.auth.RecebedorCredenciamento
import br.com.hubdeservicos.android.auth.SituacaoRecebedor
import br.com.hubdeservicos.android.auth.DesafioTelefone
import br.com.hubdeservicos.android.auth.PerfilApi
import br.com.hubdeservicos.android.auth.Municipio
import br.com.hubdeservicos.android.auth.PapelUsuario
import br.com.hubdeservicos.android.auth.Remuneracao
import br.com.hubdeservicos.android.auth.FalhaApi
import br.com.hubdeservicos.android.auth.SessaoApp
import br.com.hubdeservicos.android.auth.Usuario
import br.com.hubdeservicos.android.rede.ResultadoCep
import br.com.hubdeservicos.android.rede.ViaCep
import java.io.IOException
import java.util.concurrent.Executors
import org.json.JSONException

enum class Tela(internal val papelNecessario: PapelUsuario? = null) {
    RESTAURAR, LOGIN, CADASTRO, HOME, PERFIL, CONFIRMACAO,
    CREDENCIAMENTO(PapelUsuario.TRABALHADOR), CARTAO_CREDITO(PapelUsuario.EMPRESA)
}

data class Estado(
    val tela: Tela = Tela.RESTAURAR,
    val email: String = "",
    val usuario: Usuario? = null,
    val carregando: Boolean = false,
    val mensagem: Int? = null,
    val erro: Int? = null,
    val erroPerfil: String? = null,
    val perfil: CadastroUsuario? = null,
    val nome: String = "",
    val telefone: String = "",
    val remuneracao: Remuneracao? = null,
    val desafio: DesafioTelefone? = null,
    val credenciamento: RascunhoCredenciamento? = null,
    val recebedor: RecebedorCredenciamento? = null,
    val novoCredenciamento: Boolean = true,
    val etapaCredenciamento: Int = 0,
    val camposInvalidos: List<String> = emptyList(),
    val confirmarAlteracaoAprovada: Boolean? = null
) {
    internal fun reiniciarCredenciamento(): Estado = copy(
        credenciamento = RascunhoCredenciamento(), recebedor = null, novoCredenciamento = true,
        etapaCredenciamento = 0, camposInvalidos = emptyList(), confirmarAlteracaoAprovada = null,
        carregando = false, erro = null, erroPerfil = null, mensagem = null
    )

    internal fun podeAcessar(destino: Tela): Boolean = destino.papelNecessario?.let { papel ->
        usuario != null && perfil?.papeis?.contains(papel) == true
    } ?: true

    internal fun protegerTelaFinanceira(): Estado {
        if (podeAcessar(tela)) return this
        if (usuario == null) return Estado(Tela.LOGIN, email = email)
        return copy(tela = Tela.HOME, credenciamento = null, recebedor = null, novoCredenciamento = true,
            etapaCredenciamento = 0, camposInvalidos = emptyList(), confirmarAlteracaoAprovada = null,
            mensagem = null, erro = null, erroPerfil = null)
    }
}

class AutenticacaoModel(private val sessao: SessaoApp, private val apiPerfil: PerfilApi,
                        private val apiCredenciamento: CredenciamentoApi) {
    private val fila = Executors.newSingleThreadExecutor()
    private val filaCep = Executors.newSingleThreadExecutor()
    private val viaCep = ViaCep()
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
        when (tela) {
            Tela.CREDENCIAMENTO -> abrirCredenciamento()
            Tela.CARTAO_CREDITO -> abrirCartaoCredito()
            else -> if (!estado.carregando) atualizar(Estado(tela, email = estado.email))
        }
    }

    fun voltar(): Boolean {
        when (estado.tela) {
            Tela.CADASTRO -> mudarTela(Tela.LOGIN)
            Tela.PERFIL -> voltarHome()
            Tela.CARTAO_CREDITO -> fecharCartaoCredito()
            Tela.CONFIRMACAO -> corrigirTelefone()
            Tela.CREDENCIAMENTO -> {
                if (!estado.carregando) {
                    if (estado.etapaCredenciamento == 0) fecharCredenciamento()
                    else etapaCredenciamento(estado.etapaCredenciamento - 1)
                }
            }
            else -> return false
        }
        return true
    }

    fun revalidarAoAbrir() {
        val tela = estado.tela
        if (tela == Tela.HOME || tela.papelNecessario != null) executar(cadastral = true) {
            val perfil = consultarPerfil()
            if (tela == Tela.HOME) estadoComPerfil(tela, perfil)
            else estado.copy(perfil = perfil, carregando = false)
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

    fun abrirCredenciamento() {
        if (estado.carregando || !estado.podeAcessar(Tela.CREDENCIAMENTO)) return
        atualizar(estado.copy(tela = Tela.CREDENCIAMENTO, etapaCredenciamento = 0, credenciamento = null, recebedor = null,
            confirmarAlteracaoAprovada = null))
        executar(cadastral = true) {
            val perfil = consultarPerfil()
            val atual = estado.copy(perfil = perfil, carregando = false)
            if (!atual.podeAcessar(Tela.CREDENCIAMENTO)) return@executar atual
            val registro = sessao.autenticado {
                apiCredenciamento.consultarCredenciamento(it, ProvedorCredenciamento.STONE)
            }
            val recebedor = registro?.recebedor ?: sessao.autenticado {
                apiCredenciamento.consultarRecebedor(it, ProvedorCredenciamento.STONE)
            }
            atual.copy(novoCredenciamento = registro == null,
                credenciamento = registro ?: RascunhoCredenciamento().comTelefoneSugerido(perfil.telefone),
                etapaCredenciamento = if (registro?.modalidade == null) 0 else 1, recebedor = recebedor)
        }
    }

    fun excluirCredenciamento() {
        if (!podeEditarCredenciamento()) return
        val dados = estado.credenciamento ?: return
        executar(cadastral = true, excluindoCredenciamento = true) {
            sessao.autenticado { apiCredenciamento.excluirCredenciamento(it, ProvedorCredenciamento.STONE, dados) }
            val limpo = estado.reiniciarCredenciamento().copy(mensagem = R.string.cred_formulario_excluido)
            try {
                limpo.copy(perfil = consultarPerfil())
            } catch (falha: FalhaApi) {
                if (falha.status == 401) Estado(Tela.LOGIN, erro = R.string.erro_sessao)
                else limpo.copy(erro = R.string.cred_excluido_perfil_indisponivel)
            } catch (_: IOException) {
                limpo.copy(erro = R.string.cred_excluido_perfil_indisponivel)
            } catch (_: JSONException) {
                limpo.copy(erro = R.string.cred_excluido_perfil_indisponivel)
            }
        }
    }

    fun abrirCartaoCredito() {
        if (estado.carregando || !estado.podeAcessar(Tela.CARTAO_CREDITO)) return
        executar(cadastral = true) { estadoComPerfil(Tela.CARTAO_CREDITO, consultarPerfil()) }
    }

    fun fecharCartaoCredito() {
        if (!estado.carregando && estado.tela == Tela.CARTAO_CREDITO) atualizar(estado.copy(tela = Tela.HOME))
    }

    private fun podeEditarCredenciamento() = estado.tela == Tela.CREDENCIAMENTO &&
        estado.podeAcessar(Tela.CREDENCIAMENTO) && !estado.carregando

    fun editarCredenciamento(valores: Map<String, String>) {
        if (podeEditarCredenciamento()) {
            estado = estado.copy(credenciamento = estado.credenciamento?.atualizar(valores))
        }
    }

    fun selecionarModalidadeCredenciamento(modalidade: ModalidadeCredenciamento) {
        if (!podeEditarCredenciamento()) return
        val atual = estado.credenciamento ?: return
        val atualizado = atual.atualizar(mapOf("modalidade" to modalidade.name))
            .comTelefoneSugerido(estado.perfil?.telefone)
        atualizar(estado.copy(credenciamento = atualizado, etapaCredenciamento = 1,
            mensagem = null, erro = null, erroPerfil = null, camposInvalidos = emptyList()))
    }

    fun validarCredenciamento() {
        if (!podeEditarCredenciamento()) return
        val dados = estado.credenciamento ?: return
        executar(cadastral = true) {
            val resultado = sessao.autenticado { apiCredenciamento.validarCredenciamento(it, ProvedorCredenciamento.STONE, dados) }
            estado.copy(carregando = false, credenciamento = dados.validar(resultado), camposInvalidos = emptyList())
        }
    }

    fun etapaCredenciamento(indice: Int) {
        if (podeEditarCredenciamento()) atualizar(estado.copy(etapaCredenciamento = indice))
    }

    fun prepararEnvioRecebedor(reenvio: Boolean): Boolean {
        if (!podeEditarCredenciamento()) return false
        val dados = estado.credenciamento ?: return false
        val recebedor = estado.recebedor ?: return false
        val erro = when {
            reenvio && dados.pendenteSalvar -> R.string.cred_salvar_antes_novo_envio
            !reenvio && (dados.alterado || dados.completude?.apto != true) -> R.string.cred_validar_antes_envio
            else -> null
        }
        if (erro != null) atualizar(estado.copy(erro = erro, erroPerfil = null, mensagem = null))
        return erro == null && if (reenvio) recebedor.podeReenviar && !recebedor.emProcessamento
            else recebedor.situacao == SituacaoRecebedor.NAO_ENVIADO && recebedor.podeEnviar
    }

    fun enviarRecebedor(reenvio: Boolean) {
        if (!podeEditarCredenciamento()) return
        val dados = estado.credenciamento ?: return
        val recebedor = estado.recebedor ?: return
        if (dados.modalidade != ModalidadeCredenciamento.INDIVIDUAL || !prepararEnvioRecebedor(reenvio)) return
        executar(cadastral = true, operacaoRecebedor = true, alteraSituacao = true) {
            val resultado = sessao.autenticado {
                if (reenvio) apiCredenciamento.reenviarRecebedor(it, ProvedorCredenciamento.STONE, recebedor)
                else apiCredenciamento.enviarRecebedor(it, ProvedorCredenciamento.STONE, recebedor.ambiente, dados)
            }
            val gravado = sessao.autenticado { apiCredenciamento.consultarCredenciamento(it, ProvedorCredenciamento.STONE) }
            if (gravado == null) return@executar estado.reiniciarCredenciamento()
            val atual = gravado.recebedor ?: resultado
            val aguardando = atual.envioAceito && !atual.aprovado && atual.statusProvedor in setOf(null, "REGISTRATION", "AFFILIATION")
            estado.copy(carregando = false, recebedor = atual, novoCredenciamento = false,
                mensagem = if (aguardando) R.string.cred_envio_em_analise else null,
                credenciamento = gravado, etapaCredenciamento = if (gravado.modalidade != dados.modalidade) 0
                    else estado.etapaCredenciamento)
        }
    }

    fun atualizarRecebedor() {
        if (!podeEditarCredenciamento() || estado.recebedor?.podeConsultar != true) return
        executar(cadastral = true, operacaoRecebedor = true, alteraSituacao = true) {
            val resultado = sessao.autenticado { apiCredenciamento.atualizarRecebedor(it, ProvedorCredenciamento.STONE) }
            if (estado.credenciamento?.versao != resultado.versao) {
                estado.copy(carregando = false, recebedor = null, erro = R.string.cred_versao_desatualizada)
            } else estado.copy(carregando = false, recebedor = resultado)
        }
    }

    fun fecharCredenciamento() {
        if (!estado.carregando) atualizar(estado.copy(tela = Tela.HOME, credenciamento = null,
            recebedor = null, camposInvalidos = emptyList(), confirmarAlteracaoAprovada = null, mensagem = null, erro = null, erroPerfil = null))
    }

    fun confirmarAlteracaoCredenciamento(confirmar: Boolean) {
        if (!podeEditarCredenciamento()) return
        val aprovada = estado.confirmarAlteracaoAprovada ?: return
        estado = estado.copy(confirmarAlteracaoAprovada = null)
        if (confirmar) salvarCredenciamento(confirmarAlteracaoAprovada = aprovada)
    }

    fun salvarCredenciamento(confirmarAlteracaoAprovada: Boolean? = null) {
        if (!podeEditarCredenciamento()) return
        val dados = estado.credenciamento ?: return
        executar(cadastral = true, alteraSituacao = true) {
            val gravado = sessao.autenticado {
                apiCredenciamento.salvarCredenciamento(it, ProvedorCredenciamento.STONE, estado.novoCredenciamento,
                    dados, confirmarAlteracaoAprovada)
            }
            estado.copy(carregando = false, credenciamento = gravado, novoCredenciamento = false,
                recebedor = gravado.recebedor, camposInvalidos = emptyList(), mensagem = R.string.credenciamento_salvo)
        }
    }

    fun abrirPerfil() = executar(cadastral = true) {
        val atual = consultarPerfil()
        estadoComPerfil(Tela.PERFIL, atual)
    }

    fun editarPerfil(nome: String, telefone: String, remuneracao: Remuneracao?) {
        estado = estado.copy(nome = nome, telefone = telefone, remuneracao = remuneracao)
    }

    fun selecionarPapel(papel: PapelUsuario, selecionado: Boolean) = executar(cadastral = true) {
        val atual = sessao.autenticado { apiPerfil.selecionarPapel(it, papel, selecionado) }
        estadoComPerfil(Tela.HOME, atual)
    }

    internal fun pesquisarMunicipios(idUsuario: String, termo: String, receber: (List<Municipio>?) -> Unit) {
        operarMunicipio(idUsuario, { apiPerfil.pesquisarMunicipios(it, termo) }, receber)
    }

    internal fun consultarCep(cep: String, receber: (ResultadoCep) -> Unit) {
        filaCep.execute {
            val resultado = viaCep.consultar(cep)
            principal.post { receber(resultado) }
        }
    }

    internal fun selecionarMunicipio(idUsuario: String, municipio: Municipio, receber: (Municipio?) -> Unit) {
        atualizarMunicipio(idUsuario, { apiPerfil.selecionarMunicipio(it, municipio.codigoIbge) }, receber)
    }

    internal fun registrarMunicipio(idUsuario: String, cidade: String, uf: String, confirmada: Boolean,
                                    receber: (Municipio?) -> Unit) {
        atualizarMunicipio(idUsuario, { apiPerfil.registrarMunicipio(it, cidade, uf, confirmada) }, receber)
    }

    private fun atualizarMunicipio(idUsuario: String, gravar: (String) -> CadastroUsuario, receber: (Municipio?) -> Unit) {
        operarMunicipio(idUsuario, gravar) { cadastro ->
            if (cadastro != null) {
                atualizar(estado.copy(perfil = estado.perfil?.copy(municipio = cadastro.municipio) ?: cadastro))
            }
            receber(cadastro?.municipio)
        }
    }

    private fun <T> operarMunicipio(idUsuario: String, operacao: (String) -> T, receber: (T?) -> Unit) {
        if (estado.usuario?.id != idUsuario) return
        fila.execute {
            // A identidade e o token são verificados na mesma fila das mudanças de sessão.
            val resultado = runCatching {
                if (sessao.restaurar()?.id == idUsuario) sessao.autenticado(operacao) else null
            }.getOrNull()
            principal.post { if (estado.usuario?.id == idUsuario) receber(resultado) }
        }
    }

    fun voltarHome() = executar(cadastral = true) {
        estadoComPerfil(Tela.HOME, consultarPerfil())
    }

    fun corrigirTelefone() {
        if (!estado.carregando) atualizar(estado.copy(tela = Tela.PERFIL, desafio = null, erroPerfil = null,
            erro = null, mensagem = null))
    }

    fun salvarPerfil() = executar(cadastral = true) {
        val atual = salvarDados()
        estadoComPerfil(Tela.PERFIL, atual).copy(mensagem = R.string.perfil_salvo)
    }

    fun iniciarConfirmacao() = executar(cadastral = true) {
        try {
            val resultado = sessao.autenticado { apiPerfil.iniciarConfirmacao(it, estado.telefone.trim()) }
            estadoComPerfil(Tela.CONFIRMACAO, resultado.cadastro)
                .copy(desafio = resultado, nome = estado.nome, remuneracao = estado.remuneracao)
        } catch (falha: FalhaApi) {
            if (falha.status != 502) throw falha
            // O backend pode ter persistido o novo telefone antes da falha do provedor.
            val atual = try {
                consultarPerfil()
            } catch (consulta: FalhaApi) {
                if (consulta.status == 401) throw consulta
                null
            } catch (_: IOException) {
                null
            } catch (_: JSONException) {
                null
            }
            estado.copy(carregando = false, perfil = atual, erroPerfil = falha.mensagemPerfil,
                erro = if (falha.mensagemPerfil == null) R.string.erro_servico else null)
        }
    }

    fun reenviar() = executar(cadastral = true) {
        val resultado = sessao.autenticado(apiPerfil::reenviarConfirmacao)
        estadoComPerfil(Tela.CONFIRMACAO, resultado.cadastro).copy(desafio = resultado, nome = estado.nome, remuneracao = estado.remuneracao)
    }

    fun confirmar(codigo: String) = executar(cadastral = true) {
        val id = requireNotNull(estado.desafio).id
        val atual = sessao.autenticado { apiPerfil.confirmarTelefone(it, id, codigo) }
        estadoComPerfil(Tela.PERFIL, atual).copy(mensagem = R.string.telefone_confirmado, nome = estado.nome,
            remuneracao = estado.remuneracao)
    }

    private fun consultarPerfil() = sessao.autenticado(apiPerfil::consultarCadastro)

    private fun salvarDados() = sessao.autenticado {
        apiPerfil.atualizarCadastro(it, estado.nome.trim(), estado.telefone.trim(), estado.remuneracao)
    }

    private fun estadoComPerfil(tela: Tela, perfil: CadastroUsuario) =
        Estado(tela, usuario = estado.usuario, perfil = perfil, nome = perfil.nome.orEmpty(),
            telefone = perfil.telefone.orEmpty(),
            remuneracao = perfil.remuneracao.takeIf { PapelUsuario.TRABALHADOR in perfil.papeis })

    fun sair() = executar {
        sessao.sair()
        Estado(Tela.LOGIN)
    }

    private fun home(usuario: Usuario): Estado = try {
        val perfil = consultarPerfil()
        Estado(Tela.HOME, usuario = usuario, perfil = perfil)
    } catch (falha: FalhaApi) {
        if (falha.status == 401) Estado(Tela.LOGIN)
        else Estado(Tela.HOME, usuario = usuario)
    } catch (_: IOException) {
        Estado(Tela.HOME, usuario = usuario)
    } catch (_: JSONException) {
        Estado(Tela.HOME, usuario = usuario)
    }

    private fun contaCriada(email: String, erro: Int) =
        Estado(Tela.LOGIN, email = email, mensagem = R.string.conta_criada_login, erro = erro)

    private fun executar(cadastral: Boolean = false, operacaoRecebedor: Boolean = false,
                         alteraSituacao: Boolean = false, excluindoCredenciamento: Boolean = false,
                         operacao: () -> Estado) {
        if (estado.carregando) return
        val anterior = estado
        atualizar(anterior.copy(carregando = true, mensagem = null, erro = null, erroPerfil = null))
        fila.execute {
            val novoEstado = try {
                operacao()
            } catch (falha: FalhaApi) {
                if (cadastral && falha.status == 401) Estado(Tela.LOGIN, erro = R.string.erro_sessao)
                else if (anterior.tela == Tela.CREDENCIAMENTO &&
                    falha.codigo == "CONFIRMAR_ALTERACAO_CREDENCIAMENTO") {
                    anterior.copy(confirmarAlteracaoAprovada = falha.aprovado)
                }
                else if (anterior.tela == Tela.CREDENCIAMENTO && falha.codigo == "VERSAO_CREDENCIAMENTO_DESATUALIZADA") {
                    anterior.copy(recebedor = null, erro = R.string.cred_versao_desatualizada)
                }
                else if (excluindoCredenciamento && falha.status >= 500) {
                    anterior.reiniciarCredenciamento().copy(credenciamento = null, erro = R.string.cred_exclusao_verificar)
                }
                else if (alteraSituacao && falha.status >= 500) {
                    anterior.copy(recebedor = null, erroPerfil = falha.mensagemPerfil,
                        erro = if (falha.mensagemPerfil == null) R.string.erro_servico else null)
                }
                else if (operacaoRecebedor && falha.completude != null) {
                    anterior.copy(credenciamento = anterior.credenciamento?.validar(falha.completude))
                }
                else if (operacaoRecebedor && falha.mensagemPerfil != null) {
                    anterior.copy(erroPerfil = falha.mensagemPerfil)
                }
                else if (anterior.tela == Tela.CREDENCIAMENTO && falha.mensagemPerfil == null &&
                    falha.status in setOf(403, 409, 503)) {
                    anterior.copy(erroPerfil = null, erro = when (falha.status) {
                        403 -> R.string.cred_acesso_indisponivel
                        409 -> R.string.cred_conta_ja_salva
                        else -> R.string.cred_conta_indisponivel
                    })
                }
                else if (cadastral && falha.mensagemPerfil != null) anterior.copy(erroPerfil = falha.mensagemPerfil,
                    camposInvalidos = falha.camposInvalidos,
                    etapaCredenciamento = falha.camposInvalidos.firstOrNull()?.let {
                        when (it.substringBefore('.')) {
                            "empresa", "pessoaFisica" -> 1
                            "representante" -> 2
                            "contaBancaria" -> if (anterior.credenciamento?.modalidade ==
                                ModalidadeCredenciamento.INDIVIDUAL) 2 else 3
                            else -> 0
                        }
                    } ?: anterior.etapaCredenciamento)
                else anterior.copy(erro = quandoFalha(falha, anterior.tela))
            } catch (_: IOException) {
                if (excluindoCredenciamento) anterior.reiniciarCredenciamento().copy(credenciamento = null, erro = R.string.cred_exclusao_verificar)
                else anterior.copy(erro = R.string.erro_conexao, recebedor = anterior.recebedor.takeUnless { alteraSituacao })
            } catch (_: JSONException) {
                if (excluindoCredenciamento) anterior.reiniciarCredenciamento().copy(credenciamento = null, erro = R.string.cred_exclusao_verificar)
                else anterior.copy(erro = R.string.erro_resposta, recebedor = anterior.recebedor.takeUnless { alteraSituacao })
            } catch (_: IllegalArgumentException) {
                anterior.copy(erro = R.string.erro_resposta, recebedor = anterior.recebedor.takeUnless { alteraSituacao })
            } catch (_: IllegalStateException) {
                anterior.copy(erro = R.string.erro_configuracao)
            }
            principal.post { atualizar(novoEstado) }
        }
    }

    private fun atualizar(novoEstado: Estado) {
        estado = novoEstado.protegerTelaFinanceira()
        observador?.invoke(estado)
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
