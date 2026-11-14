package br.com.hubdeservicos.android.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import br.com.hubdeservicos.android.HubApplication
import br.com.hubdeservicos.android.R

class MainActivity : Activity() {
    private val model by lazy { (application as HubApplication).autenticacao }
    private lateinit var conteudo: LinearLayout
    private lateinit var scroll: ScrollView
    private var ultimaTela: Tela? = null
    private var confirmacaoSaida: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(getColor(R.color.gastro_fundo))
            setOnApplyWindowInsetsListener { view, insets ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bordas = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    val teclado = insets.getInsets(WindowInsets.Type.ime())
                    view.setPadding(bordas.left, bordas.top, bordas.right, maxOf(bordas.bottom, teclado.bottom))
                } else {
                    @Suppress("DEPRECATION")
                    view.setPadding(
                        insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                    )
                }
                insets
            }
        }
        val centro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
            isFocusableInTouchMode = true
        }
        scroll.addView(centro)
        conteudo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        centro.addView(conteudo, LinearLayout.LayoutParams(-1, -2))
        centro.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val largura = (right - left - centro.paddingLeft - centro.paddingRight).coerceIn(0, dp(510))
            if (largura > 0 && conteudo.layoutParams.width != largura) {
                conteudo.layoutParams = conteudo.layoutParams.apply { width = largura }
            }
        }
        setContentView(scroll)
        centro.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        model.revalidarAoAbrir()
        model.observar(::renderizar)
    }

    override fun onStop() {
        confirmacaoSaida?.dismiss()
        model.observar(null)
        ultimaTela = null
        conteudo.animate().cancel()
        super.onStop()
    }

    private fun renderizar(estado: Estado) {
        val anterior = ultimaTela
        val mudouTela = anterior != estado.tela
        val animar = anterior != null && mudouTela && estado.tela != Tela.RESTAURAR
        ultimaTela = estado.tela
        conteudo.removeAllViews()
        if (mudouTela) scroll.scrollTo(0, 0)
        when (estado.tela) {
            Tela.RESTAURAR -> restauracao(estado)
            Tela.LOGIN, Tela.CADASTRO -> formulario(estado)
            Tela.HOME -> {
                val usuario = requireNotNull(estado.usuario)
                conteudo.adicionar(marca())
                conteudo.adicionar(HomeView(this, usuario, estado, ::confirmarSaida), 22)
            }
        }
        if (mudouTela) animarEntrada(animar)
    }

    private fun animarEntrada(animar: Boolean) {
        conteudo.animate().cancel()
        conteudo.alpha = 1f
        conteudo.translationY = 0f
        if (animar && ValueAnimator.areAnimatorsEnabled()) {
            conteudo.alpha = 0.65f
            conteudo.translationY = dp(8).toFloat()
            conteudo.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(160)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun confirmarSaida() {
        if (confirmacaoSaida != null) return
        confirmacaoSaida = AlertDialog.Builder(this)
            .setTitle(R.string.confirmar_saida_titulo)
            .setMessage(R.string.confirmar_saida_mensagem)
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.sair) { _, _ -> model.sair() }
            .setOnDismissListener { confirmacaoSaida = null }
            .show()
    }

    private fun restauracao(estado: Estado) {
        conteudo.adicionar(marca())
        val painel = painel()
        conteudo.adicionar(painel, 22)
        painel.adicionar(texto(getString(estado.erro ?: R.string.restaurando), 16, R.color.gastro_texto))
        if (estado.erro == null) painel.adicionar(ProgressBar(this), 16)
        else painel.adicionar(botao(R.string.tentar_novamente, acao = model::restaurar), 16)
    }

    private fun formulario(estado: Estado) {
        val cadastro = estado.tela == Tela.CADASTRO
        val titulo = if (cadastro) R.string.criar_conta else R.string.entrar
        val habilitado = !estado.carregando
        conteudo.adicionar(marca())
        if (!cadastro) {
            val frase = texto(getString(R.string.frase_apresentacao), 28, R.color.gastro_oliva, serifa)
            frase.setPadding(dp(18), dp(16), dp(18), dp(18))
            frase.background = superficie(R.color.gastro_terracota_clara, 18)
            conteudo.adicionar(frase, 20)
        }

        val painel = painel()
        conteudo.adicionar(painel, 18)
        painel.adicionar(texto(getString(titulo), 30, R.color.gastro_oliva, serifa)
            .apply { isAccessibilityHeading = true })
        estado.mensagem?.let { painel.aviso(it, R.color.gastro_oliva) }
        estado.erro?.let { painel.aviso(it, R.color.gastro_erro) }

        val email = entrada(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            getString(R.string.email_hint)).apply {
            background = superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
            setText(estado.email)
            isEnabled = habilitado
            imeOptions = EditorInfo.IME_ACTION_NEXT
            setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        }
        painel.adicionar(texto(getString(R.string.email), 14, R.color.gastro_texto, Typeface.DEFAULT_BOLD)
            .apply { labelFor = email.id }, 18)
        painel.adicionar(email, 6)
        val senha = CampoSenha(this, R.string.senha, cadastro, habilitado)
        painel.adicionar(senha, 14)
        email.nextFocusForwardId = senha.campo.id
        val confirmacao = if (cadastro) CampoSenha(this, R.string.confirmar_senha, true, habilitado) else null
        if (confirmacao != null) {
            painel.adicionar(texto(getString(R.string.senha_regras), 14, R.color.gastro_texto_suave), 8)
            painel.adicionar(confirmacao, 14)
            senha.campo.imeOptions = EditorInfo.IME_ACTION_NEXT
            senha.campo.nextFocusForwardId = confirmacao.campo.id
        }

        val ultimoCampo = confirmacao?.campo ?: senha.campo
        val feedback = texto("", 14, R.color.gastro_erro).apply {
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(0, dp(10), 0, 0)
        }
        painel.adicionar(feedback)
        val enviar = { enviarFormulario(email, senha.campo, confirmacao?.campo, feedback) }
        ultimoCampo.imeOptions = EditorInfo.IME_ACTION_DONE
        ultimoCampo.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE && habilitado) {
                enviar()
                true
            } else false
        }
        painel.adicionar(botao(titulo, habilitado, acao = enviar), 18)
        painel.adicionar(botao(if (cadastro) R.string.voltar_login else R.string.criar_conta, habilitado, false) {
            model.editarEmail(email.text.toString())
            model.mudarTela(if (cadastro) Tela.LOGIN else Tela.CADASTRO)
        }, 8)
        if (estado.carregando) painel.adicionar(ProgressBar(this), 12)
    }

    private fun enviarFormulario(email: EditText, senha: EditText, confirmacao: EditText?, feedback: android.widget.TextView) {
        val conta = email.text.toString().trim()
        val segredo = senha.text.toString()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(conta).matches() || conta.length > 255) {
            erroCampo(email, feedback, R.string.email_invalido)
            return
        }
        erroSenha(segredo, confirmacao != null)?.let {
            erroCampo(senha, feedback, it)
            return
        }
        if (confirmacao != null) {
            erroConfirmacao(segredo, confirmacao.text.toString())?.let {
                erroCampo(confirmacao, feedback, it)
                return
            }
        }
        model.editarEmail(conta)
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(senha.windowToken, 0)
        if (confirmacao == null) model.entrar(conta, segredo) else model.cadastrar(conta, segredo)
    }

    private fun erroCampo(campo: EditText, feedback: android.widget.TextView, mensagem: Int) {
        feedback.text = getString(mensagem)
        feedback.visibility = View.VISIBLE
        campo.requestFocus()
    }

    private fun marca() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val simbolo = ImageView(context).apply {
            setImageResource(R.drawable.gastro_simbolo)
            background = superficie(R.color.gastro_oliva, 14)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(simbolo, LinearLayout.LayoutParams(dp(48), dp(48)))
        addView(texto(getString(R.string.app_name), 25, R.color.gastro_oliva, serifa),
            LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
    }

    private fun painel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = superficie(R.color.gastro_papel, 18, R.color.gastro_linha)
    }

    private fun LinearLayout.aviso(mensagem: Int, cor: Int) {
        adicionar(texto(getString(mensagem), 15, cor).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }, 10)
    }

    private fun botao(
        rotulo: Int, habilitado: Boolean = true, principal: Boolean = true, acao: () -> Unit
    ) = Button(this).apply {
        setText(rotulo)
        textSize = 16f
        isAllCaps = false
        minimumHeight = dp(48)
        setTextColor(getColor(if (principal) R.color.gastro_papel else R.color.gastro_oliva))
        background = acaoVisual(if (principal) R.color.gastro_oliva else R.color.gastro_terracota_clara, 12)
        isEnabled = habilitado
        alpha = if (habilitado) 1f else 0.55f
        setOnClickListener { acao() }
    }

    private val serifa = Typeface.create("serif", Typeface.BOLD)
}
