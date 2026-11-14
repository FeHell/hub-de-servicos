package br.com.hubdeservicos.android.ui

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import br.com.hubdeservicos.android.R
import br.com.hubdeservicos.android.auth.Municipio
import java.util.Locale
import java.util.concurrent.Executors

internal sealed interface LocalizacaoUi {
    data object Obtendo : LocalizacaoUi
    data object SemPermissao : LocalizacaoUi
    data object Indisponivel : LocalizacaoUi
    data object Erro : LocalizacaoUi
    data object Identificada : LocalizacaoUi
}

internal class LocalizacaoHome(
    private val activity: Activity,
    private val model: AutenticacaoModel,
    private val apresentar: (LocalizacaoUi) -> Unit
) {
    private val localizador = activity.getSystemService(LocationManager::class.java)
    private val principal = Handler(Looper.getMainLooper())
    private val geocodificacao = Executors.newSingleThreadExecutor()
    private val preferencias = activity.getPreferences(Activity.MODE_PRIVATE)
    private var usuario: String? = null
    private var ultimaConsulta: Long? = null
    private var ultimaSituacao: LocalizacaoUi = LocalizacaoUi.SemPermissao
    private var explicacao: AlertDialog? = null
    private var consulta: CancellationSignal? = null
    private var prazo: Runnable? = null
    private var solicitouNaTela = false
    private var municipio: Municipio? = null
    private var selecao: SelecaoMunicipioDialogo? = null
    private var confirmarTroca: AlertDialog? = null
    private var retomadaExplicita = false

    fun exibir(id: String, salvo: Municipio?) {
        if (usuario != id) {
            cancelar()
            usuario = id
            ultimaConsulta = null
            ultimaSituacao = LocalizacaoUi.SemPermissao
        }
        municipio = salvo
        if (retomadaExplicita && !solicitouNaTela) {
            retomadaExplicita = false
            if (temPermissao()) {
                buscar(explicita = true)
                return
            }
        }
        if (salvo?.manual == true) {
            mostrar(LocalizacaoUi.Identificada)
            return
        }
        if (temPermissao()) {
            val ult = ultimaConsulta
            if (consulta == null && (ult == null || SystemClock.elapsedRealtime() - ult >= INTERVALO)) buscar()
        } else {
            ultimaConsulta = null
            cancelar()
            mostrar(LocalizacaoUi.SemPermissao)
            if (!solicitouNaTela && !preferencias.getBoolean(EXPLICADO, false)) explicar()
        }
    }

    fun atualizar() {
        val id = usuario ?: return
        if (selecao != null) return
        if (consulta != null) {
            cancelar()
            mostrar(if (municipio == null) LocalizacaoUi.Indisponivel else LocalizacaoUi.Identificada)
        }
        selecao = SelecaoMunicipioDialogo(activity, model, id, ::usarLocalizacao,
            aoSalvar = { salvo ->
                municipio = salvo
                mostrar(LocalizacaoUi.Identificada, forcar = true)
            }, aoFechar = { selecao = null })
    }

    private fun usarLocalizacao() {
        if (temPermissao()) buscar(explicita = true)
        else if (preferencias.getBoolean(SOLICITADO, false) &&
            !activity.shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION)) {
            retomadaExplicita = true
            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null)))
        } else explicar(explicita = true)
    }

    fun resultadoPermissao() {
        solicitouNaTela = false
        ultimaConsulta = null
        if (usuario != null) {
            if (temPermissao()) buscar(explicita = retomadaExplicita) else mostrar(LocalizacaoUi.SemPermissao)
            retomadaExplicita = false
        }
    }

    fun parar() {
        if (consulta != null) {
            cancelar()
            ultimaConsulta = null
        }
        explicacao?.dismiss()
        selecao?.fechar()
        confirmarTroca?.dismiss()
    }

    fun encerrarSessao() {
        parar()
        usuario = null
        municipio = null
        retomadaExplicita = false
        ultimaConsulta = null
        ultimaSituacao = LocalizacaoUi.SemPermissao
        apresentar(ultimaSituacao)
    }

    fun fechar() {
        parar()
        geocodificacao.shutdownNow()
    }

    private fun explicar(explicita: Boolean = false) {
        if (explicacao != null || solicitouNaTela) return
        preferencias.edit().putBoolean(EXPLICADO, true).apply()
        explicacao = AlertDialog.Builder(activity)
            .setTitle(R.string.localizacao_titulo)
            .setMessage(R.string.localizacao_explicacao)
            .setNegativeButton(R.string.localizacao_agora_nao, null)
            .setPositiveButton(R.string.localizacao_permitir) { _, _ ->
                solicitouNaTela = true
                retomadaExplicita = explicita
                preferencias.edit().putBoolean(SOLICITADO, true).apply()
                activity.requestPermissions(arrayOf(ACCESS_COARSE_LOCATION), PEDIDO_PERMISSAO)
            }
            .setOnDismissListener { explicacao = null }
            .show()
    }

    @Suppress("MissingPermission")
    private fun buscar(explicita: Boolean = false) {
        val id = usuario ?: return
        if (!temPermissao() || consulta != null) return
        ultimaConsulta = SystemClock.elapsedRealtime()
        val provedores = listOf("fused", LocationManager.NETWORK_PROVIDER).filter(localizador::isProviderEnabled)
        if (provedores.isEmpty() || (Build.VERSION.SDK_INT >= 28 && !localizador.isLocationEnabled)) {
            mostrar(LocalizacaoUi.Indisponivel)
            return
        }

        val sinal = CancellationSignal()
        consulta = sinal
        mostrar(LocalizacaoUi.Obtendo)
        val limite = Runnable {
            if (consulta === sinal) terminar(LocalizacaoUi.Indisponivel)
        }
        prazo = limite
        principal.postDelayed(limite, 12_000)
        try {
            val recente = provedores.mapNotNull(localizador::getLastKnownLocation)
                .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..RECENTE }
                .maxByOrNull { it.elapsedRealtimeNanos }
            when {
                recente != null -> identificar(id, sinal, recente, explicita)
                Build.VERSION.SDK_INT >= 30 -> localizador.getCurrentLocation(provedores.first(), sinal,
                    activity.mainExecutor) { local -> identificar(id, sinal, local, explicita) }
                else -> pedirUmaLocalizacaoLegada(provedores.first(), sinal) { local ->
                    identificar(id, sinal, local, explicita)
                }
            }
        } catch (_: SecurityException) {
            terminar(if (temPermissao()) LocalizacaoUi.Indisponivel else LocalizacaoUi.SemPermissao)
        } catch (_: IllegalArgumentException) {
            terminar(LocalizacaoUi.Indisponivel)
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun pedirUmaLocalizacaoLegada(provedor: String, sinal: CancellationSignal, receber: (Location) -> Unit) {
        val listener = android.location.LocationListener { local -> receber(local) }
        localizador.requestSingleUpdate(provedor, listener, Looper.getMainLooper())
        sinal.setOnCancelListener { localizador.removeUpdates(listener) }
    }

    @Suppress("DEPRECATION")
    private fun identificar(id: String, sinal: CancellationSignal, local: Location?, explicita: Boolean) {
        if (consulta !== sinal) return
        if (local == null) {
            terminar(LocalizacaoUi.Indisponivel)
            return
        }
        if (!Geocoder.isPresent()) {
            terminar(LocalizacaoUi.Erro)
            return
        }
        prazo?.let(principal::removeCallbacks)
        val limite = Runnable { if (consulta === sinal) terminar(LocalizacaoUi.Erro) }
        prazo = limite
        principal.postDelayed(limite, 15_000)
        val geocoder = Geocoder(activity.applicationContext, Locale.forLanguageTag("pt-BR"))
        val resposta: (List<Address>) -> Unit = { enderecos ->
            principal.post {
                if (consulta === sinal) {
                    val endereco = enderecos.firstOrNull {
                        it.countryCode.equals("BR", ignoreCase = true) &&
                            !it.locality.isNullOrBlank() && !it.adminArea.isNullOrBlank()
                    }
                    if (endereco == null) terminar(LocalizacaoUi.Erro)
                    else {
                        prazo?.let(principal::removeCallbacks)
                        escolherLocalizacao(id, sinal, endereco.locality, endereco.adminArea, explicita)
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                geocoder.getFromLocation(local.latitude, local.longitude, 3, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = resposta(addresses)
                    override fun onError(errorMessage: String?) = resposta(emptyList())
                })
            } catch (_: IllegalArgumentException) {
                terminar(LocalizacaoUi.Erro)
            }
        } else geocodificacao.execute {
            resposta(runCatching { geocoder.getFromLocation(local.latitude, local.longitude, 3).orEmpty() }
                .getOrDefault(emptyList()))
        }
    }

    private fun escolherLocalizacao(id: String, sinal: CancellationSignal, cidade: String, estado: String,
                                      explicita: Boolean) {
        fun gravar(confirmada: Boolean) {
            model.registrarMunicipio(id, cidade, estado, confirmada) { salvo ->
                if (consulta === sinal) {
                    if (salvo != null) municipio = salvo
                    terminar(if (salvo != null) LocalizacaoUi.Identificada else LocalizacaoUi.Erro)
                }
            }
        }
        if (explicita && municipio != null) {
            confirmarTroca = AlertDialog.Builder(activity)
                .setTitle(R.string.municipio_trocar_automatico)
                .setMessage(activity.getString(R.string.municipio_trocar_confirmar, cidade, estado))
                .setNegativeButton(R.string.cancelar) { _, _ -> terminar(LocalizacaoUi.Identificada) }
                .setPositiveButton(R.string.municipio_confirmar) { _, _ -> gravar(true) }
                .setOnCancelListener { terminar(LocalizacaoUi.Identificada) }
                .setOnDismissListener { confirmarTroca = null }
                .show()
        } else gravar(explicita)
    }

    private fun cancelar() {
        val sinal = consulta
        consulta = null
        sinal?.cancel()
        prazo?.let(principal::removeCallbacks)
        prazo = null
    }

    private fun terminar(situacao: LocalizacaoUi) {
        cancelar()
        mostrar(situacao)
    }

    private fun mostrar(situacao: LocalizacaoUi, forcar: Boolean = false) {
        if (!forcar && situacao == ultimaSituacao) return
        ultimaSituacao = situacao
        apresentar(situacao)
    }

    private fun temPermissao() =
        activity.checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val PEDIDO_PERMISSAO = 51
        private const val EXPLICADO = "localizacao_explicada"
        private const val SOLICITADO = "localizacao_solicitada"
        private const val INTERVALO = 15 * 60 * 1_000L
        private const val RECENTE = 2 * 60 * 1_000_000_000L
    }
}
