package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.auth.SituacaoTelefone

internal object EstadoTelefone {
    private val brasileiro = Regex("[1-9][0-9]([2-5][0-9]{7}|9[0-9]{8})")

    fun valido(entrada: CharSequence) = MascaraTelefone.nacional(entrada).matches(brasileiro)

    fun apresentar(entrada: CharSequence, salvo: String?, situacao: SituacaoTelefone?): SituacaoTelefone {
        val numero = MascaraTelefone.nacional(entrada)
        return when {
            numero.isEmpty() -> SituacaoTelefone.NAO_INFORMADO
            situacao == SituacaoTelefone.VERIFICADO && numero == MascaraTelefone.nacional(salvo.orEmpty()) ->
                SituacaoTelefone.VERIFICADO
            else -> SituacaoTelefone.PENDENTE
        }
    }
}
