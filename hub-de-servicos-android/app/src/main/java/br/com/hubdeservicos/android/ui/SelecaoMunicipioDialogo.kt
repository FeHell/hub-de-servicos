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
import br.com.hubdeservicos.android.auth.Municipio

internal class SelecaoMunicipioDialogo(
    private val activity: Activity, private val model: AutenticacaoModel, private val idUsuario: String,
    private val usarLocalizacao: () -> Unit, private val aoSalvar: (Municipio) -> Unit,
    private val aoFechar: () -> Unit
) {
    private val principal = Handler(Looper.getMainLooper())
    private val campo = activity.entrada(android.text.InputType.TYPE_CLASS_TEXT,
        activity.getString(R.string.municipio_pesquisar)).apply {
        filters = arrayOf(InputFilter.LengthFilter(120))
        background = activity.superficie(R.color.gastro_papel, 12, R.color.gastro_campo_borda)
    }
    private val mensagem = activity.texto(activity.getString(R.string.municipio_minimo), 14, R.color.gastro_texto_suave)
    private val lista = ListView(activity).apply {
        choiceMode = ListView.CHOICE_MODE_SINGLE
        dividerHeight = activity.dp(1)
    }
    private var municipios = emptyList<Municipio>()
    private var escolhido: Municipio? = null
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
        dialogo = AlertDialog.Builder(activity).setTitle(R.string.municipio_selecionar).setView(conteudo)
            .setPositiveButton(R.string.municipio_confirmar, null)
            .setNegativeButton(R.string.cancelar, null)
            .setNeutralButton(R.string.municipio_usar_localizacao) { _, _ -> usarLocalizacao() }
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
            escolhido = municipios.getOrNull(posicao)
            botaoConfirmar().isEnabled = escolhido != null
        }
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = pesquisar(s.toString().trim())
            override fun afterTextChanged(s: Editable) = Unit
        })
    }

    fun fechar() = dialogo.dismiss()

    private fun botaoConfirmar() = dialogo.getButton(AlertDialog.BUTTON_POSITIVE)

    private fun pesquisar(termo: String) {
        val requisicao = ++versao
        consulta?.let(principal::removeCallbacks)
        lista.clearChoices()
        lista.adapter = null
        municipios = emptyList()
        escolhido = null
        botaoConfirmar().isEnabled = false
        if (termo.length < 2) {
            mensagem.setText(R.string.municipio_minimo)
            return
        }
        mensagem.setText(R.string.municipio_buscando)
        val tarefa = Runnable {
            model.pesquisarMunicipios(idUsuario, termo) { itens ->
                if (dialogo.isShowing && requisicao == versao) {
                    municipios = itens.orEmpty()
                    val rotulos = municipios.map { activity.getString(R.string.localizacao_cidade, it.nome, it.uf) }
                    lista.adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_single_choice, rotulos)
                    mensagem.text = when {
                        itens == null -> activity.getString(R.string.municipio_erro_busca)
                        itens.isEmpty() -> activity.getString(R.string.municipio_vazio)
                        else -> ""
                    }
                }
            }
        }
        consulta = tarefa
        principal.postDelayed(tarefa, 300)
    }

    private fun confirmar() {
        val selecionado = escolhido ?: return
        if (salvando) return
        atualizarSalvamento(true)
        mensagem.setText(R.string.municipio_salvando)
        model.selecionarMunicipio(idUsuario, selecionado) { salvo ->
            if (salvo != null) {
                aoSalvar(salvo)
                if (dialogo.isShowing) dialogo.dismiss()
            } else if (dialogo.isShowing) {
                atualizarSalvamento(false)
                mensagem.setText(R.string.municipio_erro_salvar)
            }
        }
    }

    private fun atualizarSalvamento(ocupado: Boolean) {
        salvando = ocupado
        campo.isEnabled = !ocupado
        lista.isEnabled = !ocupado
        botaoConfirmar().isEnabled = !ocupado
        dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !ocupado
        dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled = !ocupado
        dialogo.setCancelable(!ocupado)
        dialogo.setCanceledOnTouchOutside(!ocupado)
    }
}
