package br.com.hubdeservicos.android.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import br.com.hubdeservicos.android.R

internal data class TextosSelecao(
    val titulo: Int,
    val pesquisar: Int,
    val vazio: Int,
    val minimo: Int = R.string.municipio_minimo,
    val buscando: Int = R.string.municipio_buscando,
    val erroBusca: Int = R.string.municipio_erro_busca,
    val salvando: Int = R.string.municipio_salvando,
    val erroSalvar: Int = R.string.municipio_erro_salvar
)

internal class SelecaoPesquisavelDialogo<T : Any>(
    private val activity: Activity,
    private val textos: TextosSelecao,
    private val rotuloItem: (T) -> String,
    private val buscarItens: (String, (List<T>?) -> Unit) -> Unit,
    private val confirmarItem: (T, (Boolean) -> Unit) -> Unit,
    private val aoFechar: () -> Unit,
    acaoSecundaria: Pair<Int, () -> Unit>? = null,
    private val minimoCaracteres: Int = 2,
    private val atrasoBuscaMs: Long = 300
) {
    private val principal = Handler(Looper.getMainLooper())
    private val campo = activity.entrada(android.text.InputType.TYPE_CLASS_TEXT,
        activity.getString(textos.pesquisar)).apply {
        filters = arrayOf(InputFilter.LengthFilter(120))
        background = activity.superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
    }
    private val mensagem = activity.texto("", 14, R.color.gastro_texto_suave)
    private val lista = ListView(activity).apply {
        choiceMode = ListView.CHOICE_MODE_SINGLE
        dividerHeight = activity.dp(1)
    }
    private var resultados = emptyList<T>()
    private var escolhido: T? = null
    private var consulta: Runnable? = null
    private var versao = 0
    private var salvando = false
    private val dialogo: AlertDialog

    init {
        val conteudo = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(18), activity.dp(8), activity.dp(18), activity.dp(4))
            adicionar(campo)
            adicionar(mensagem, 8)
            val altura = (activity.resources.displayMetrics.heightPixels / 4).coerceAtMost(activity.dp(240))
            addView(lista, LinearLayout.LayoutParams(-1, altura))
        }
        mensagem.accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
        dialogo = AlertDialog.Builder(activity).setTitle(textos.titulo).setView(conteudo)
            .setPositiveButton(R.string.municipio_confirmar, null)
            .setNegativeButton(R.string.cancelar, null)
            .apply {
                acaoSecundaria?.let { (rotulo, acao) ->
                    setNeutralButton(rotulo) { _, _ -> acao() }
                }
            }
            .setOnDismissListener {
                versao++
                consulta?.let(principal::removeCallbacks)
                aoFechar()
            }
            .create()
        dialogo.show()
        dialogo.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        botaoConfirmar().isEnabled = false
        botaoConfirmar().setOnClickListener { confirmar() }
        lista.setOnItemClickListener { _, _, posicao, _ ->
            escolhido = resultados.getOrNull(posicao)
            botaoConfirmar().isEnabled = escolhido != null
        }
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = pesquisar(s.toString().trim())
            override fun afterTextChanged(s: Editable) = Unit
        })
        pesquisar("")
    }

    fun fechar() = dialogo.dismiss()

    private fun botaoConfirmar() = dialogo.getButton(AlertDialog.BUTTON_POSITIVE)

    private fun pesquisar(termo: String) {
        val requisicao = ++versao
        consulta?.let(principal::removeCallbacks)
        lista.clearChoices()
        lista.adapter = null
        resultados = emptyList()
        escolhido = null
        botaoConfirmar().isEnabled = false
        if (termo.length < minimoCaracteres) {
            mensagem.setText(textos.minimo)
            return
        }
        mensagem.setText(textos.buscando)
        val tarefa = Runnable {
            buscarItens(termo) { itens ->
                if (dialogo.isShowing && requisicao == versao) {
                    resultados = itens.orEmpty()
                    val rotulos = resultados.map(rotuloItem)
                    lista.adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_single_choice, rotulos)
                    mensagem.text = when {
                        itens == null -> activity.getString(textos.erroBusca)
                        itens.isEmpty() -> activity.getString(textos.vazio)
                        else -> ""
                    }
                }
            }
        }
        consulta = tarefa
        principal.postDelayed(tarefa, atrasoBuscaMs)
    }

    private fun confirmar() {
        val selecionado = escolhido ?: return
        if (salvando) return
        atualizarSalvamento(true)
        mensagem.setText(textos.salvando)
        confirmarItem(selecionado) { aceito ->
            if (dialogo.isShowing && aceito) {
                dialogo.dismiss()
            } else if (dialogo.isShowing) {
                atualizarSalvamento(false)
                mensagem.setText(textos.erroSalvar)
            }
        }
    }

    private fun atualizarSalvamento(ocupado: Boolean) {
        salvando = ocupado
        campo.isEnabled = !ocupado
        lista.isEnabled = !ocupado
        botaoConfirmar().isEnabled = !ocupado
        dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !ocupado
        dialogo.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = !ocupado
        dialogo.setCancelable(!ocupado)
        dialogo.setCanceledOnTouchOutside(!ocupado)
    }
}
