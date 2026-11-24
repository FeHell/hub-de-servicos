package br.com.hubdeservicos.android.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Typeface
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.AdapterView
import android.text.method.TextKeyListener
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.Municipio
import br.com.hubdeservicos.android.auth.ModalidadeCredenciamento
import br.com.hubdeservicos.android.auth.AmbienteRecebedor
import br.com.hubdeservicos.android.auth.RecebedorCredenciamento
import br.com.hubdeservicos.android.auth.SituacaoRecebedor
import br.com.hubdeservicos.android.auth.camposEndereco
import br.com.hubdeservicos.android.rede.ResultadoCep
import br.com.hubdeservicos.android.rede.cepParaConsulta
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class CredenciamentoView(
    private val activity: Activity, private val estado: Estado, private val model: AutenticacaoModel
) : LinearLayout(activity) {
    private enum class Formato {
        TEXTO, EMAIL, URL, CPF, CNPJ, DOCUMENTO, CEP, TELEFONE, VALOR, DATA, NUMEROS, CONTA, TITULAR,
        TIPO_TELEFONE, BANCO, CIDADE, DECLARACAO
    }
    private data class Campo(val chave: String, val rotulo: Int, val formato: Formato = Formato.TEXTO,
                             val opcional: Boolean = false, val limite: Int = 160, val ajuda: Int? = null)
    private data class Secao(val chave: String, val titulo: Int, val campos: List<Campo>)

    private val camposEndereco = listOf(
        Campo("cep", R.string.cred_cep, Formato.CEP, limite = 9),
        Campo("logradouro", R.string.cred_logradouro),
        Campo("numero", R.string.cred_numero, limite = 20),
        Campo("complemento", R.string.cred_complemento, limite = 100),
        Campo("bairro", R.string.cred_bairro, limite = 100),
        Campo("cidade", R.string.cred_cidade, Formato.CIDADE, limite = 120),
        Campo("pontoReferencia", R.string.cred_ponto_referencia, ajuda = R.string.cred_ponto_referencia_ajuda)
    ).map { it.copy(chave = "endereco.${it.chave}") }

    private val contato = listOf(
        Campo("email", R.string.email, Formato.EMAIL, limite = 254),
        Campo("telefone", R.string.perfil_telefone, Formato.TELEFONE, limite = 24),
        Campo("tipoTelefone", R.string.cred_tipo_telefone, Formato.TIPO_TELEFONE)
    )
    private val site = Campo("siteUrl", R.string.cred_site, Formato.URL, limite = 2048, ajuda = R.string.cred_site_ajuda)
    private val pessoa = listOf(
        Campo("nome", R.string.cred_nome),
        Campo("cpf", R.string.cred_cpf, Formato.CPF, limite = 14),
        Campo("nascimento", R.string.cred_nascimento, Formato.DATA),
        Campo("profissao", R.string.cred_profissao, limite = 100),
        Campo("rendaMensal", R.string.cred_renda_mensal, Formato.VALOR)
    ) + contato

    private val individual = Secao("pessoaFisica", R.string.cred_pf, pessoa + site + camposEndereco)
    private val empresa = Secao("empresa", R.string.cred_empresa, listOf(
        Campo("razaoSocial", R.string.cred_razao),
        Campo("nomeFantasia", R.string.cred_fantasia),
        Campo("cnpj", R.string.cred_cnpj, Formato.CNPJ, limite = 18),
        Campo("faturamentoAnual", R.string.cred_faturamento_anual, Formato.VALOR),
        Campo("tipoSocietario", R.string.cred_tipo_societario, opcional = true, limite = 100),
        Campo("dataFundacao", R.string.cred_fundacao, Formato.DATA, opcional = true)
    ) + contato + site + camposEndereco)
    private val representante = Secao("representante", R.string.cred_representante, pessoa + camposEndereco + listOf(
        Campo("representanteLegal", R.string.cred_declara_representacao, Formato.DECLARACAO),
        Campo("socioQsa", R.string.cred_declara_qsa, Formato.DECLARACAO)
    ))
    private val conta = Secao("contaBancaria", R.string.cred_dados_banco, listOf(
        Campo("titular", R.string.cred_titular),
        Campo("documento", R.string.cred_documento, Formato.DOCUMENTO, limite = 18),
        Campo("tipoTitular", R.string.cred_tipo_titular, Formato.TITULAR),
        Campo("tipo", R.string.cred_tipo, Formato.CONTA),
        Campo("banco", R.string.cred_banco, Formato.BANCO),
        Campo("agencia", R.string.cred_agencia, Formato.NUMEROS, limite = 10),
        Campo("digitoAgencia", R.string.cred_digito_agencia, opcional = true, limite = 2),
        Campo("conta", R.string.cred_conta, Formato.NUMEROS, limite = 20),
        Campo("digitoConta", R.string.cred_digito_conta, limite = 2)
    ))
    private val habilitado = !estado.carregando
    private val dataBrasileira = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))
    private val entradas = mutableMapOf<String, EditText>()
    private var coletarEdicao: () -> Unit = {}
    private var nomeCidade = ""
    private var ufCidade = ""
    private var revisaoCidade = 0
    private var botaoCidade: Button? = null
    private var seletorCidade: SelecaoMunicipioDialogo? = null
    private var seletorBanco: SelecaoPesquisavelDialogo<Banco>? = null
    private val catalogoBancos by lazy {
        CatalogoBancos(resources.openRawResource(R.raw.catalogo_bancos).reader())
    }

    private val feedback: android.widget.TextView by lazy {
        context.texto("", 14, R.color.gastro_erro).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
    }

    init {
        orientation = VERTICAL
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        background = context.superficie(R.color.gastro_papel, 18, R.color.gastro_linha)
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
        titulo(context.getString(R.string.credenciamento_titulo), 27)
        (estado.erroPerfil ?: estado.erro?.let(context::getString))?.let {
            adicionar(context.texto(it, 15, R.color.gastro_erro).apply {
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }, 10)
        }
        estado.mensagem?.let { adicionar(context.texto(context.getString(it), 15, R.color.gastro_oliva), 10) }
        if (estado.carregando) adicionar(ProgressBar(context), 12)
        val recebedor = estado.recebedor
        if (recebedor != null) {
            adicionar(SituacaoCredenciamentoView(context, recebedor, habilitado,
                reenviar = {
                    coletarEdicao()
                    if (model.prepararEnvioRecebedor(reenvio = true)) confirmarEnvio(recebedor.ambiente, reenvio = true)
                },
                consultar = {
                    coletarEdicao()
                    model.atualizarRecebedor()
                }), 14)
        }
        if (estado.credenciamento != null) {
            if (recebedor == null && habilitado) {
                adicionar(context.botaoCadastro(R.string.cred_recarregar_dados, true, false) {
                    AlertDialog.Builder(activity).setMessage(R.string.cred_recarregar_aviso)
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.cred_recarregar_dados) { _, _ -> model.abrirCredenciamento() }
                        .show()
                }, 12)
            }
            formulario()
            adicionar(context.botaoCadastro(R.string.cred_excluir_formulario, habilitado, false) {
                AlertDialog.Builder(activity).setTitle(R.string.cred_excluir_formulario)
                    .setMessage(R.string.cred_excluir_confirmacao)
                    .setNegativeButton(R.string.cancelar, null)
                    .setPositiveButton(R.string.cred_excluir_formulario) { _, _ -> model.excluirCredenciamento() }
                    .show().getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getColor(R.color.gastro_erro))
            }.apply {
                setTextColor(context.getColor(R.color.gastro_erro))
                background = context.acaoVisual(R.color.cred_status_erro_fundo, 12, R.color.gastro_erro)
            }, 18)
        } else if (habilitado) {
            adicionar(context.botaoCadastro(R.string.tentar_novamente, true, acao = model::abrirCredenciamento), 12)
            adicionar(context.botaoCadastro(R.string.perfil_voltar, true, false, model::fecharCredenciamento), 8)
        }
        estado.confirmarAlteracaoAprovada?.let(::confirmarAlteracao)
    }

    private fun formulario() {
        val dados = requireNotNull(estado.credenciamento)
        val indice = estado.etapaCredenciamento
        if (indice == 0 || dados.modalidade == null) {
            escolherModalidade(dados.modalidade)
            return
        }

        val secoes = if (dados.modalidade == ModalidadeCredenciamento.INDIVIDUAL)
            listOf(individual, conta) else listOf(empresa, representante, conta)
        val secao = secoes[indice - 1]
        titulo(context.getString(R.string.cred_etapa, indice, secoes.size, context.getString(secao.titulo)), 20, 16)
        adicionar(context.texto(context.getString(R.string.cred_obrigatorios), 14, R.color.gastro_texto_suave), 6)
        if (secao == representante) {
            adicionar(context.texto(context.getString(R.string.cred_orientacao_qsa), 14, R.color.gastro_texto_suave), 8)
        }
        if (secao != conta) {
            nomeCidade = dados["${secao.chave}.endereco.cidade"]
            ufCidade = dados["${secao.chave}.endereco.uf"]
        }
        val leitores = secao.campos.associateWith { campo ->
            val chave = "${secao.chave}.${campo.chave}"
            entrada(campo, dados[chave], chave)
        }
        if (secao != conta) configurarEndereco(secao.chave)

        fun coletar(): Map<String, String> = leitores.mapKeys { (campo, _) -> "${secao.chave}.${campo.chave}" }
            .mapValues { (_, leitor) -> leitor() }.also(model::editarCredenciamento)
        coletarEdicao = { coletar() }

        adicionar(feedback, 8)
        val ultima = indice == secoes.size
        if (ultima) {
            adicionar(context.botaoCadastro(R.string.cred_verificar, habilitado) {
                coletar()
                model.validarCredenciamento()
            }, 18)
            val recebedor = estado.recebedor
            if (dados.modalidade == ModalidadeCredenciamento.INDIVIDUAL && recebedor?.situacao == SituacaoRecebedor.NAO_ENVIADO
                && recebedor.podeEnviar && dados.completude?.apto == true && !dados.alterado) {
                adicionar(context.botaoCadastro(R.string.cred_enviar_recebedor, habilitado) {
                    coletar()
                    if (model.prepararEnvioRecebedor(reenvio = false)) confirmarEnvio(recebedor.ambiente, reenvio = false)
                }, 8)
            }
        } else {
            adicionar(context.botaoCadastro(R.string.cred_seguinte, habilitado) {
                val atuais = coletar()
                val documento = secao.campos.firstOrNull { it.formato in setOf(Formato.CPF, Formato.CNPJ) }
                val chave = documento?.let { "${secao.chave}.${it.chave}" }
                val valor = atuais[chave].orEmpty()
                if (valor.isNotBlank() && (!documentoCredenciamentoValido(valor) ||
                        (documento?.formato == Formato.CNPJ && valor.length != 14))) {
                    val mensagem = context.getString(if (documento?.formato == Formato.CNPJ)
                        R.string.cred_cnpj_invalido else R.string.cred_cpf_invalido)
                    entradas[chave]?.error = mensagem
                    feedback.text = mensagem
                    feedback.visibility = View.VISIBLE
                } else model.etapaCredenciamento(indice + 1)
            }, 18)
        }
        adicionar(context.botaoCadastro(R.string.cred_salvar_dados, habilitado, false) {
            coletar()
            model.salvarCredenciamento()
        }, 8)
        adicionar(context.botaoCadastro(R.string.cred_anterior, habilitado, false) {
            coletar()
            model.etapaCredenciamento(indice - 1)
        }, 8)
        mostrarPendencias(secao, secoes)
    }

    private fun confirmarAlteracao(aprovada: Boolean) {
        AlertDialog.Builder(activity).setTitle(R.string.cred_confirmar_alteracao)
            .setMessage(if (aprovada) R.string.cred_confirmar_alteracao_aprovada else R.string.cred_confirmar_alteracao_pendente)
            .setNegativeButton(R.string.cancelar) { _, _ -> model.confirmarAlteracaoCredenciamento(false) }
            .setPositiveButton(R.string.cred_confirmar_alteracao_acao) { _, _ -> model.confirmarAlteracaoCredenciamento(true) }
            .setOnCancelListener { model.confirmarAlteracaoCredenciamento(false) }
            .show()
    }

    private fun confirmarEnvio(ambiente: AmbienteRecebedor, reenvio: Boolean) {
        val mensagem = if (reenvio) {
            val texto = context.getString(R.string.cred_reenvio_confirmar)
            if (ambiente == AmbienteRecebedor.DEV) "$texto\n\n${context.getString(R.string.cred_ambiente_teste)}" else texto
        } else context.getString(if (ambiente == AmbienteRecebedor.DEV) R.string.cred_envio_confirmar_dev
            else R.string.cred_envio_confirmar_prod)
        AlertDialog.Builder(activity)
            .setTitle(if (reenvio) R.string.cred_reenvio_titulo else R.string.cred_enviar_recebedor)
            .setMessage(mensagem)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.cred_enviar) { _, _ -> model.enviarRecebedor(reenvio) }
            .show()
    }

    private fun escolherModalidade(modalidade: ModalidadeCredenciamento?) {
        titulo(context.getString(R.string.cred_modalidade), 20, 16)
        for ((tipo, rotulo) in listOf(ModalidadeCredenciamento.INDIVIDUAL to R.string.cred_pf,
            ModalidadeCredenciamento.CORPORATION to R.string.cred_pj)) {
            adicionar(context.botaoCadastro(rotulo, habilitado, modalidade == tipo) {
                if (modalidade != null && modalidade != tipo) {
                    AlertDialog.Builder(activity).setTitle(R.string.cred_modalidade)
                        .setMessage(R.string.cred_trocar_modalidade)
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.cred_continuar) { _, _ -> model.selecionarModalidadeCredenciamento(tipo) }
                        .show()
                } else model.selecionarModalidadeCredenciamento(tipo)
            }, 10)
        }
        adicionar(context.botaoCadastro(R.string.perfil_voltar, habilitado, false, model::fecharCredenciamento), 8)
    }

    private fun mostrarPendencias(secao: Secao, secoes: List<Secao>) {
        val dados = requireNotNull(estado.credenciamento)
        val completude = dados.completude ?: return
        val pendencias = completude.pendencias + estado.recebedor?.pendencias.orEmpty()
            .filter { dados.modalidade == ModalidadeCredenciamento.INDIVIDUAL }
        if (completude.apto && !dados.alterado && pendencias.isEmpty()) {
            val mensagem = if (dados.modalidade == ModalidadeCredenciamento.INDIVIDUAL && estado.recebedor?.podeEnviar == true)
                R.string.cred_dados_prontos_envio else R.string.cred_validado
            adicionar(context.texto(context.getString(mensagem), 14, R.color.gastro_oliva), 12)
            return
        }
        if (pendencias.isEmpty()) return
        titulo(context.getString(R.string.cred_pendencias), 18, 12)
        for (pendencia in pendencias) {
            val grupo = secoes.firstOrNull { pendencia.campo.substringBefore('.') == it.chave }
            val chave = pendencia.campo.substringAfter('.')
            val chaveVisivel = if (chave == "endereco.uf") "endereco.cidade" else chave
            val campo = grupo?.campos?.firstOrNull { it.chave == chaveVisivel }
            val rotulo = listOfNotNull(
                grupo?.takeIf { it != secao || campo == null }?.let { context.getString(it.titulo) },
                campo?.let { context.getString(it.rotulo) }
            ).joinToString(" · ")
            if (grupo == secao) entradas[pendencia.campo]?.error = pendencia.mensagem
            val mensagem = pendencia.mensagem.replace("do recebedor", "de quem vai receber")
            adicionar(context.texto(if (rotulo.isEmpty()) mensagem else "$rotulo · $mensagem",
                14, R.color.gastro_texto_suave), 4)
        }
    }

    private fun titulo(texto: String, tamanho: Int, margem: Int = 0) {
        adicionar(context.texto(texto, tamanho, R.color.gastro_oliva,
            Typeface.create("serif", Typeface.BOLD)).apply { isAccessibilityHeading = true }, margem)
    }

    private fun entrada(campo: Campo, valor: String, chave: String): () -> String {
        val rotulo = context.getString(campo.rotulo) + if (campo.opcional) "" else " *"
        if (campo.formato == Formato.BANCO) return entradaBanco(rotulo, valor, chave)
        if (campo.formato == Formato.TIPO_TELEFONE) return entradaTipoTelefone(rotulo, valor, chave)
        if (campo.formato == Formato.CIDADE) {
            val botao = context.botaoCadastro(R.string.cred_escolher_cidade, habilitado, false) {
                abrirSelecaoCidade(chave.substringBeforeLast('.'))
            }.apply { id = View.generateViewId() }
            botaoCidade = botao
            adicionar(context.texto(rotulo, 14, R.color.gastro_texto).apply { labelFor = botao.id }, 14)
            adicionar(botao, 6)
            return { nomeCidade }
        }
        if (campo.formato == Formato.DECLARACAO) {
            val caixa = CheckBox(context).apply {
                text = rotulo
                isEnabled = habilitado
                isChecked = valor == "true"
                setOnCheckedChangeListener { _, marcado ->
                    model.editarCredenciamento(mapOf(chave to marcado.toString()))
                }
            }
            adicionar(caixa, 12)
            return { caixa.isChecked.toString() }
        }
        val opcoes = when (campo.formato) {
            Formato.CONTA -> listOf("" to R.string.cred_selecione, "CORRENTE" to R.string.cred_corrente,
                "POUPANCA" to R.string.cred_poupanca)
            Formato.TITULAR -> listOf("" to R.string.cred_selecione_opcao, "INDIVIDUAL" to R.string.cred_pf,
                "COMPANY" to R.string.cred_pj)
            else -> null
        }
        if (opcoes != null) {
            val codigos = opcoes.map { it.first }
            val seletor = Spinner(context).apply {
                id = View.generateViewId()
                isEnabled = habilitado
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                    opcoes.map { context.getString(it.second) })
                setSelection(codigos.indexOf(valor).coerceAtLeast(0))
            }
            adicionar(context.texto(rotulo, 14, R.color.gastro_texto).apply { labelFor = seletor.id }, 14)
            adicionar(seletor, 6)
            seletor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    model.editarCredenciamento(mapOf(chave to codigos[position]))
                }
                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
            return { codigos[seletor.selectedItemPosition] }
        }
        val tipo = when (campo.formato) {
            Formato.EMAIL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            Formato.URL -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            Formato.TELEFONE -> InputType.TYPE_CLASS_PHONE
            Formato.CPF, Formato.CEP, Formato.NUMEROS, Formato.VALOR -> InputType.TYPE_CLASS_NUMBER
            else -> InputType.TYPE_CLASS_TEXT
        }
        val texto = context.entrada(tipo).apply {
            isEnabled = habilitado
            background = context.superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
            filters = arrayOf(InputFilter.LengthFilter(campo.limite))
            setText(valor)
            if (chave in estado.camposInvalidos) error = context.getString(R.string.cred_campo_invalido)
        }
        entradas[chave] = texto
        adicionar(context.texto(rotulo, 14, R.color.gastro_texto).apply { labelFor = texto.id }, 14)
        adicionar(texto, 6)
        campo.ajuda?.let { adicionar(context.texto(context.getString(it), 14, R.color.gastro_texto_suave), 4) }
        if (campo.formato == Formato.CPF || campo.formato == Formato.CEP) {
            texto.keyListener = TextKeyListener.getInstance()
            texto.setRawInputType(InputType.TYPE_CLASS_NUMBER)
        }
        val lerData = if (campo.formato == Formato.DATA) data(texto, valor) else null
        when (campo.formato) {
            Formato.TELEFONE -> texto.aplicarMascaraTelefone(valor)
            Formato.VALOR -> texto.aplicarMascaraMoeda(valor.toBigDecimalOrNull())
            Formato.CPF, Formato.CNPJ, Formato.DOCUMENTO, Formato.CEP -> mascararDocumento(texto, campo.formato)
            else -> Unit
        }
        val lerFormatado = {
            val digitado = texto.text.toString().trim()
            when {
                lerData != null -> lerData()
                campo.formato == Formato.VALOR -> runCatching { MascaraMoeda.valor(digitado)?.toPlainString() }.getOrNull().orEmpty()
                campo.formato in setOf(Formato.CPF, Formato.CNPJ, Formato.DOCUMENTO) -> normalizarDocumento(digitado)
                else -> digitado
            }
        }
        val formatadoAoAbrir = lerFormatado()
        val ler = { lerFormatado().let { atual -> if (atual == formatadoAoAbrir) valor else atual } }
        texto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                model.editarCredenciamento(mapOf(chave to ler()))
            }
        })
        return ler
    }

    private fun entradaBanco(rotulo: String, valor: String, chave: String): () -> String {
        var codigo = valor
        val botao = context.botaoCadastro(R.string.cred_escolher_banco, habilitado, false) {}.apply {
            id = View.generateViewId()
            if (valor.isNotBlank()) {
                text = catalogoBancos.consultar(valor)?.let(::rotuloBanco)
                    ?: context.getString(R.string.cred_banco_codigo, valor)
            }
            setOnClickListener {
                abrirSelecaoBanco { banco ->
                    codigo = banco.codigo
                    text = rotuloBanco(banco)
                    model.editarCredenciamento(mapOf(chave to codigo))
                }
            }
        }
        adicionar(context.texto(rotulo, 14, R.color.gastro_texto).apply { labelFor = botao.id }, 14)
        adicionar(botao, 6)
        return { codigo }
    }

    private fun abrirSelecaoBanco(selecionar: (Banco) -> Unit) {
        if (seletorBanco != null) return
        seletorBanco = SelecaoPesquisavelDialogo(
            activity = activity,
            textos = TextosSelecao(R.string.cred_escolher_banco, R.string.cred_banco_pesquisar,
                R.string.cred_banco_vazio, buscando = R.string.cred_banco_buscando),
            rotuloItem = ::rotuloBanco,
            buscarItens = { termo, receber -> receber(catalogoBancos.pesquisar(termo)) },
            confirmarItem = { banco, confirmar ->
                selecionar(banco)
                confirmar(true)
            },
            aoFechar = { seletorBanco = null },
            minimoCaracteres = 0,
            atrasoBuscaMs = 0
        )
    }

    private fun rotuloBanco(banco: Banco): String = context.getString(R.string.cred_banco_nome_codigo,
        banco.codigo, banco.nome)

    private fun entradaTipoTelefone(rotulo: String, valor: String, chave: String): () -> String {
        val grupo = RadioGroup(context).apply { id = View.generateViewId() }
        val opcoes = listOf("CELULAR" to R.string.cred_celular, "FIXO" to R.string.cred_fixo).associate { (tipo, texto) ->
            val opcao = RadioButton(context).apply {
                id = View.generateViewId()
                setText(texto)
                isEnabled = habilitado
                minHeight = context.dp(48)
            }
            grupo.addView(opcao)
            if (valor == tipo) grupo.check(opcao.id)
            opcao.id to tipo
        }
        adicionar(context.texto(rotulo, 14, R.color.gastro_texto).apply { labelFor = grupo.id }, 14)
        adicionar(context.texto(context.getString(R.string.cred_tipo_telefone_ajuda), 14, R.color.gastro_texto_suave), 4)
        adicionar(grupo, 4)
        val ler = { opcoes[grupo.checkedRadioButtonId].orEmpty() }
        grupo.setOnCheckedChangeListener { _, _ -> model.editarCredenciamento(mapOf(chave to ler())) }
        return ler
    }

    private fun configurarEndereco(secao: String) {
        val prefixo = "$secao.endereco"
        val cep = entradas.getValue("$prefixo.cep")
        val status = context.texto("", 14, R.color.gastro_texto_suave).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val margem = LayoutParams(-1, -2).apply { topMargin = context.dp(6) }
        addView(status, indexOfChild(cep) + 1, margem)
        atualizarRotuloCidade()
        var emConsulta: String? = null
        var ultimoCep = cepParaConsulta(cep.text.toString()).takeIf {
            nomeCidade.isNotBlank() && !entradas.getValue("$prefixo.logradouro").text.isNullOrBlank()
        }
        fun informar(mensagem: Int) {
            status.setText(mensagem)
            status.visibility = View.VISIBLE
        }
        cep.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (cepParaConsulta(s.toString()) != emConsulta) status.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable) = Unit
        })
        cep.setOnFocusChangeListener { _, emFoco ->
            if (emFoco || !habilitado || !isAttachedToWindow || cep.text.isBlank()) return@setOnFocusChangeListener
            val consultado = cepParaConsulta(cep.text.toString())
            if (consultado == null) {
                informar(R.string.cred_cep_invalido)
                return@setOnFocusChangeListener
            }
            if (consultado == emConsulta || consultado == ultimoCep) return@setOnFocusChangeListener
            emConsulta = consultado
            informar(R.string.cred_cep_buscando)
            val camposOriginais = entradas.mapValues { (_, campo) -> campo.text.toString() }
            val revisaoOriginal = revisaoCidade
            model.consultarCep(consultado) consulta@{ resultado ->
                if (!isAttachedToWindow || emConsulta != consultado) return@consulta
                emConsulta = null
                if (cepParaConsulta(cep.text.toString()) != consultado) return@consulta
                when (resultado) {
                    is ResultadoCep.Encontrado -> {
                        ultimoCep = consultado
                        fun preencher(chave: String, valor: String, somenteVazio: Boolean = false) {
                            val campo = entradas.getValue("$prefixo.$chave")
                            if (valor.isNotBlank() && campo.text.toString() == camposOriginais["$prefixo.$chave"] &&
                                (!somenteVazio || campo.text.isBlank())) campo.setText(valor)
                        }
                        preencher("logradouro", resultado.logradouro)
                        preencher("bairro", resultado.bairro)
                        preencher("complemento", resultado.complemento, somenteVazio = true)
                        if (revisaoCidade == revisaoOriginal && resultado.municipio != null) {
                            aplicarMunicipio(prefixo, resultado.municipio)
                        }
                        if (resultado.municipio == null) informar(R.string.cred_cep_selecionar_cidade)
                        else status.visibility = View.GONE
                    }
                    ResultadoCep.NaoEncontrado -> {
                        ultimoCep = consultado
                        informar(R.string.cred_cep_nao_encontrado)
                    }
                    ResultadoCep.Indisponivel -> informar(R.string.cred_cep_indisponivel)
                    ResultadoCep.Invalido -> informar(R.string.cred_cep_invalido)
                }
            }
        }
    }

    private fun abrirSelecaoCidade(prefixo: String) {
        val usuario = estado.usuario ?: return
        if (seletorCidade != null) return
        entradas["$prefixo.cep"]?.clearFocus()
        seletorCidade = SelecaoMunicipioDialogo(activity,
            pesquisarMunicipios = { termo, receber -> model.pesquisarMunicipios(usuario.id, termo, receber) },
            confirmarMunicipio = { municipio, confirmar ->
                aplicarMunicipio(prefixo, municipio)
                confirmar(true)
            }, aoFechar = { seletorCidade = null })
    }

    private fun aplicarMunicipio(prefixo: String, municipio: Municipio) {
        revisaoCidade++
        nomeCidade = municipio.nome
        ufCidade = municipio.uf
        model.editarCredenciamento(municipio.camposEndereco(prefixo))
        atualizarRotuloCidade()
    }

    private fun atualizarRotuloCidade() {
        botaoCidade?.text = when {
            nomeCidade.isBlank() -> context.getString(R.string.cred_escolher_cidade)
            ufCidade.isBlank() -> nomeCidade
            else -> context.getString(R.string.localizacao_cidade, nomeCidade, ufCidade)
        }
    }

    override fun onDetachedFromWindow() {
        seletorCidade?.fechar()
        seletorBanco?.fechar()
        super.onDetachedFromWindow()
    }

    private fun data(campo: EditText, valor: String): () -> String {
        var escolhida = valor.takeIf(String::isNotBlank)?.let(LocalDate::parse)
        campo.keyListener = null
        campo.hint = "DD/MM/AAAA"
        campo.setText(escolhida?.format(dataBrasileira).orEmpty())
        campo.setOnClickListener {
            val inicial = escolhida ?: LocalDate.now()
            DatePickerDialog(context, { _, ano, mes, dia ->
                escolhida = LocalDate.of(ano, mes + 1, dia)
                campo.setText(escolhida?.format(dataBrasileira))
            }, inicial.year, inicial.monthValue - 1, inicial.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
        return { escolhida?.toString().orEmpty() }
    }

    private fun mascararDocumento(campo: EditText, formato: Formato) {
        fun exibir(texto: CharSequence): String {
            val alfanumerico = formato == Formato.DOCUMENTO || formato == Formato.CNPJ
            val valor = texto.toString().uppercase(Locale.ROOT)
                .filter { it in '0'..'9' || alfanumerico && it in 'A'..'Z' }
            val marcas = when {
                formato == Formato.CEP -> mapOf(5 to '-')
                formato == Formato.CPF || (formato == Formato.DOCUMENTO && valor.length <= 11 && valor.all(Char::isDigit)) ->
                    mapOf(3 to '.', 6 to '.', 9 to '-')
                else -> mapOf(2 to '.', 5 to '.', 8 to '/', 12 to '-')
            }
            val limite = if (formato == Formato.CEP) 8 else if (formato == Formato.CPF) 11 else 14
            return buildString {
                valor.take(limite).forEachIndexed { i, caractere ->
                    marcas[i]?.let { append(it) }
                    append(caractere)
                }
            }
        }
        campo.setText(exibir(campo.text))
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                val mascarado = exibir(s)
                if (mascarado != s.toString()) {
                    val cursor = (campo.selectionStart + mascarado.length - s.length).coerceIn(0, mascarado.length)
                    s.replace(0, s.length, mascarado)
                    campo.setSelection(cursor)
                }
            }
        })
    }
}
