package br.com.hubdeservicos.android.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import br.com.hubdeservicos.android.HubApplication
import br.com.hubdeservicos.android.R

class MainActivity : Activity() {
    private val model by lazy { (application as HubApplication).autenticacao }
    private lateinit var conteudo: LinearLayout
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(245, 248, 246))
            setOnApplyWindowInsetsListener { view, insets ->
                @Suppress("DEPRECATION")
                val barras = intArrayOf(
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bordas = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    val teclado = insets.getInsets(WindowInsets.Type.ime())
                    view.setPadding(bordas.left, bordas.top, bordas.right, maxOf(bordas.bottom, teclado.bottom))
                } else {
                    view.setPadding(barras[0], barras[1], barras[2], barras[3])
                }
                insets
            }
        }
        val centro = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        scroll.addView(centro)
        conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            background = fundo(Color.WHITE, 18, Color.rgb(217, 229, 223))
        }
        centro.addView(conteudo, LinearLayout.LayoutParams(
            minOf(resources.displayMetrics.widthPixels - dp(48), dp(430)), LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        setContentView(scroll)
    }

    override fun onStart() {
        super.onStart()
        model.observar(::renderizar)
    }

    override fun onStop() {
        model.observar(null)
        super.onStop()
    }

    private fun renderizar(estado: Estado) {
        conteudo.removeAllViews()
        texto(getString(R.string.app_name), 18, verde, negrito = true)
        when (estado.tela) {
            Tela.RESTAURAR -> {
                if (estado.erro != null) {
                    texto(estado.erro, 16, vermelho, margem = 16)
                    botao(R.string.tentar_novamente, margem = 16, acao = model::restaurar)
                } else {
                    texto(getString(R.string.restaurando), 16, textoSecundario, margem = 16)
                    progresso()
                }
            }
            Tela.LOGIN, Tela.CADASTRO -> formulario(estado)
            Tela.CONTA -> {
                texto(getString(R.string.sessao_ativa), 30, textoPrincipal, margem = 14, negrito = true)
                texto(estado.usuario?.email.orEmpty(), 17, textoSecundario, margem = 10)
                estado.erro?.let { texto(it, 15, vermelho, margem = 12, anunciar = true) }
                botao(R.string.sair, margem = 24, habilitado = !estado.carregando, acao = model::sair)
                if (estado.carregando) progresso()
            }
        }
    }

    private fun formulario(estado: Estado) {
        val cadastro = estado.tela == Tela.CADASTRO
        val titulo = if (cadastro) R.string.criar_conta else R.string.entrar
        texto(getString(titulo), 30, textoPrincipal, margem = 14, negrito = true)
        estado.mensagem?.let { texto(it, 15, verde, margem = 12, anunciar = true) }
        val email = campo(R.string.email, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 20)
        email.setText(estado.email)
        email.isEnabled = !estado.carregando
        email.imeOptions = EditorInfo.IME_ACTION_NEXT
        email.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS)
        email.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                model.editarEmail(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val senha = campo(R.string.senha, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, 12)
        senha.isEnabled = !estado.carregando
        senha.imeOptions = EditorInfo.IME_ACTION_DONE
        senha.setAutofillHints(if (cadastro) "newPassword" else View.AUTOFILL_HINT_PASSWORD)
        if (cadastro) texto(getString(R.string.senha_regras), 14, textoSecundario, margem = 8)
        estado.erro?.let { texto(it, 15, vermelho, margem = 12, anunciar = true) }
        val enviar = {
            val conta = email.text.toString().trim()
            val segredo = senha.text.toString()
            val bytes = segredo.toByteArray(Charsets.UTF_8).size
            when {
                !android.util.Patterns.EMAIL_ADDRESS.matcher(conta).matches() || conta.length > 255 ->
                    email.error = "Informe um e-mail válido."
                segredo.isBlank() -> senha.error = "Informe a senha."
                bytes > 72 || (cadastro && segredo.length < 12) ->
                    senha.error = if (cadastro) getString(R.string.senha_regras) else "A senha deve ter até 72 bytes em UTF-8."
                else -> {
                    getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(senha.windowToken, 0)
                    if (cadastro) model.cadastrar(conta, segredo) else model.entrar(conta, segredo)
                }
            }
        }
        senha.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE && !estado.carregando) {
                enviar()
                true
            } else false
        }
        botao(titulo, margem = 20, habilitado = !estado.carregando, acao = enviar)
        botao(if (cadastro) R.string.voltar_login else R.string.criar_conta,
            margem = 6, habilitado = !estado.carregando, principal = false) {
            model.mudarTela(if (cadastro) Tela.LOGIN else Tela.CADASTRO)
        }
        if (estado.carregando) progresso()
    }

    private fun campo(rotulo: Int, tipo: Int, margem: Int): EditText {
        val idCampo = View.generateViewId()
        texto(getString(rotulo), 14, textoPrincipal, margem, negrito = true).labelFor = idCampo
        return EditText(this).apply {
            id = idCampo
            isSaveEnabled = false
            inputType = tipo
            setSingleLine(true)
            setTextColor(textoPrincipal)
            textSize = 16f
            minHeight = dp(52)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = fundo(Color.WHITE, 10, Color.rgb(125, 151, 140))
            adicionar(this, 6)
        }
    }

    private fun texto(
        valor: String, tamanho: Int, cor: Int, margem: Int = 0, negrito: Boolean = false, anunciar: Boolean = false
    ) = TextView(this).apply {
        text = valor
        textSize = tamanho.toFloat()
        setTextColor(cor)
        if (negrito) setTypeface(typeface, Typeface.BOLD)
        if (anunciar) accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        adicionar(this, margem)
    }

    private fun botao(
        rotulo: Int, margem: Int, habilitado: Boolean = true, principal: Boolean = true, acao: () -> Unit
    ) {
        Button(this).apply {
            setText(rotulo)
            textSize = 16f
            isAllCaps = false
            minimumHeight = dp(48)
            setTextColor(if (principal) Color.WHITE else verde)
            if (principal) backgroundTintList = ColorStateList.valueOf(verde)
            else backgroundTintList = ColorStateList.valueOf(Color.rgb(232, 242, 237))
            isEnabled = habilitado
            alpha = if (habilitado) 1f else 0.55f
            setOnClickListener { acao() }
            adicionar(this, margem)
        }
    }

    private fun progresso() {
        val indicador = ProgressBar(this)
        conteudo.addView(indicador, LinearLayout.LayoutParams(dp(28), dp(28)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(12)
        })
    }

    private fun fundo(cor: Int, raio: Int, borda: Int) = GradientDrawable().apply {
        setColor(cor)
        cornerRadius = dp(raio).toFloat()
        setStroke(dp(1), borda)
    }

    private fun adicionar(view: View, margem: Int) {
        conteudo.addView(view, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(margem) })
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density + 0.5f).toInt()

    private val verde = Color.rgb(18, 98, 78)
    private val textoPrincipal = Color.rgb(19, 43, 34)
    private val textoSecundario = Color.rgb(67, 91, 82)
    private val vermelho = Color.rgb(156, 34, 44)
}
