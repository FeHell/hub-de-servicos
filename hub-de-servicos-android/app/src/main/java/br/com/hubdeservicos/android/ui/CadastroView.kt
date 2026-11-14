package br.com.hubdeservicos.android.ui

import android.content.Context
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.SituacaoTelefone

internal class CadastroView(context: Context, private val estado: Estado, private val model: AutenticacaoModel) :
    LinearLayout(context) {
    private val habilitado = !estado.carregando

    init {
        orientation = VERTICAL
        background = context.superficie(R.color.gastro_papel, 18, R.color.gastro_linha)
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(18))
        val confirmacao = estado.tela == Tela.CONFIRMACAO
        val titulo = if (confirmacao) R.string.sms_titulo else R.string.perfil_titulo
        adicionar(context.texto(context.getString(titulo), 27, R.color.gastro_oliva,
            Typeface.create("serif", Typeface.BOLD)).apply { isAccessibilityHeading = true })
        estado.mensagem?.let { mensagem(context.getString(it), R.color.gastro_oliva) }
        (estado.erroPerfil ?: estado.erro?.let(context::getString))?.let { mensagem(it, R.color.gastro_erro) }
        if (confirmacao) confirmar() else perfil()
        if (estado.carregando) adicionar(ProgressBar(context), 10)
    }

    private fun perfil() {
        val nome = campo(R.string.perfil_nome, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS, estado.nome, 150)
        val telefone = campo(R.string.perfil_telefone, InputType.TYPE_CLASS_PHONE, "", 32).apply {
            aplicarMascaraTelefone(estado.telefone)
        }
        val situacao = when (estado.perfil?.situacaoTelefone) {
            SituacaoTelefone.VERIFICADO -> R.string.telefone_verificado
            SituacaoTelefone.PENDENTE -> R.string.telefone_pendente
            else -> R.string.telefone_ausente
        }
        mensagem(context.getString(situacao), R.color.gastro_texto_suave)
        val feedback = context.texto("", 14, R.color.gastro_erro).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        adicionar(feedback, 8)

        fun preencher(acao: () -> Unit) {
            model.editarPerfil(nome.text.toString(), MascaraTelefone.nacional(telefone.text))
            esconderTeclado()
            acao()
        }

        adicionar(context.botaoCadastro(R.string.perfil_salvar, habilitado) { preencher(model::salvarPerfil) }, 18)
        if (estado.perfil?.situacaoTelefone != SituacaoTelefone.VERIFICADO) {
            adicionar(context.botaoCadastro(R.string.sms_iniciar, habilitado, false) {
                if (telefone.text.isBlank()) {
                    feedback.text = context.getString(R.string.pendencia_telefone)
                    feedback.visibility = View.VISIBLE
                    telefone.requestFocus()
                } else preencher(model::iniciarConfirmacao)
            }, 8)
        }
        adicionar(context.botaoCadastro(R.string.perfil_voltar, habilitado, false, model::voltarHome), 8)
    }

    private fun confirmar() {
        val desafio = requireNotNull(estado.desafio)
        mensagem(context.getString(R.string.sms_destino, desafio.telefoneMascarado), R.color.gastro_texto_suave)
        val codigo = campo(R.string.sms_codigo, InputType.TYPE_CLASS_NUMBER, "", 6)
        adicionar(context.botaoCadastro(R.string.sms_confirmar, habilitado) {
            if (!codigo.text.matches(Regex("[0-9]{6}"))) {
                codigo.error = context.getString(R.string.sms_codigo_invalido)
                codigo.requestFocus()
            } else {
                esconderTeclado()
                model.confirmar(codigo.text.toString())
            }
        }, 18)
        adicionar(context.botaoCadastro(R.string.sms_reenviar, habilitado, false, model::reenviar), 8)
        adicionar(context.botaoCadastro(R.string.sms_corrigir, habilitado, false, model::corrigirTelefone), 8)
    }

    private fun campo(rotulo: Int, tipo: Int, valor: String, limite: Int): EditText {
        val campo = context.entrada(tipo).apply {
            background = context.superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
            setText(valor)
            filters = arrayOf(InputFilter.LengthFilter(limite))
            isEnabled = habilitado
        }
        adicionar(context.texto(context.getString(rotulo), 14, R.color.gastro_texto, Typeface.DEFAULT_BOLD)
            .apply { labelFor = campo.id }, 14)
        adicionar(campo, 6)
        return campo
    }

    private fun mensagem(conteudo: String, cor: Int) {
        adicionar(context.texto(conteudo, 15, cor).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }, 10)
    }

    private fun esconderTeclado() {
        context.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(windowToken, 0)
    }
}

internal fun Context.botaoCadastro(rotulo: Int, habilitado: Boolean, principal: Boolean = true, acao: () -> Unit) =
    Button(this).apply {
        setText(rotulo)
        isAllCaps = false
        minimumHeight = dp(48)
        setTextColor(getColor(if (principal) R.color.gastro_papel else R.color.gastro_oliva))
        background = acaoVisual(if (principal) R.color.gastro_oliva else R.color.gastro_terracota_clara, 12)
        isEnabled = habilitado
        alpha = if (habilitado) 1f else 0.55f
        setOnClickListener { acao() }
    }
