package br.com.hubdeservicos.android.rede

import br.com.hubdeservicos.android.auth.Municipio
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class ViaCepTest {
    @Test fun validaAntesDeConsultarEAproveitaOCacheParaOMesmoCep() {
        val consultas = mutableListOf<String>()
        val api = ViaCep { cep ->
            consultas.add(cep)
            """{"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé","complemento":"lado ímpar",
                "localidade":"São Paulo","uf":"SP","ibge":"3550308"}"""
        }

        assertEquals(ResultadoCep.Invalido, api.consultar("01001-00"))
        assertEquals(ResultadoCep.Invalido, api.consultar("01001A000"))
        assertEquals(emptyList<String>(), consultas)
        val esperado = ResultadoCep.Encontrado("Praça da Sé", "Sé", "lado ímpar",
            Municipio(3550308, "São Paulo", "SP"))
        assertEquals(esperado, api.consultar("01001-000"))
        assertEquals(esperado, api.consultar("01001000"))
        assertEquals(listOf("01001000"), consultas)
    }

    @Test fun diferenciaCepInexistenteFalhaDeRedeERetornoInvalido() {
        var consultas = 0
        val api = ViaCep {
            when (++consultas) {
                1 -> throw IOException()
                2 -> """{"logradouro":"Sem CEP"}"""
                else -> """{"erro":true}"""
            }
        }
        assertEquals(ResultadoCep.Indisponivel, api.consultar("99999-999"))
        assertEquals(ResultadoCep.Indisponivel, api.consultar("99999-999"))
        assertEquals(ResultadoCep.NaoEncontrado, api.consultar("99999-999"))
        assertEquals(ResultadoCep.NaoEncontrado, api.consultar("99999999"))
        assertEquals(3, consultas)
    }

    @Test fun separaEnderecoDisponivelDeMunicipioSemCodigoIbge() {
        val api = ViaCep {
            """{"cep":"38000-000","logradouro":"", "bairro":null,"localidade":"Uberaba","uf":"MG","ibge":""}"""
        }
        assertEquals(ResultadoCep.Encontrado("", "", "", null), api.consultar("38000-000"))
    }
}
