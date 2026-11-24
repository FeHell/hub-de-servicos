package br.com.hubdeservicos.android.ui

import br.com.hubdeservicos.android.auth.CompletudeCredenciamento
import br.com.hubdeservicos.android.auth.Municipio
import br.com.hubdeservicos.android.auth.ModalidadeCredenciamento
import org.json.JSONObject
import br.com.hubdeservicos.android.auth.RascunhoCredenciamento
import br.com.hubdeservicos.android.auth.SituacaoRecebedor
import br.com.hubdeservicos.android.auth.camposEndereco
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredenciamentoFormularioTest {
    @Test fun descartaNomeDaMaeEmRascunhosLegadosEEdicoes() {
        for ((modalidade, secao) in listOf("INDIVIDUAL" to "pessoaFisica", "CORPORATION" to "representante")) {
            val carregado = RascunhoCredenciamento.de(JSONObject("""
                {"dados":{"modalidade":"$modalidade","$secao":{"nome":"Nome de teste","nomeMae":"Valor legado","mother_name":"Valor legado"}}}
            """))
            assertEquals("", carregado["$secao.nomeMae"])
            assertFalse(carregado.json().toString().contains("nomeMae"))
            assertFalse(carregado.json().toString().contains("mother_name"))
            val editado = carregado.atualizar(mapOf("$secao.nomeMae" to "Novo valor"))
            assertFalse(editado.alterado)
            assertFalse(editado.json().getJSONObject(secao).has("nomeMae"))
            assertEquals("Nome de teste", editado.json().getJSONObject(secao).getString("nome"))
        }
    }

    @Test fun mantemCodigoBancarioETiposDeTelefoneNoContrato() {
        for ((modalidade, contatos) in listOf(
            "INDIVIDUAL" to mapOf("pessoaFisica" to "CELULAR"),
            "CORPORATION" to mapOf("empresa" to "CELULAR", "representante" to "FIXO")
        )) {
            val campos = contatos.mapKeys { (secao, _) -> "$secao.tipoTelefone" } +
                mapOf("modalidade" to modalidade, "contaBancaria.banco" to "001")
            val json = RascunhoCredenciamento(campos).json()
            assertEquals("001", json.getJSONObject("contaBancaria").get("banco"))
            for ((secao, tipo) in contatos) assertEquals(tipo, json.getJSONObject(secao).getString("tipoTelefone"))
        }
    }

    @Test fun preservaCidadeEUfDoRascunhoEGravaSelecaoParaPfPjERepresentante() {
        for ((modalidade, secao) in listOf("INDIVIDUAL" to "pessoaFisica",
            "CORPORATION" to "empresa", "CORPORATION" to "representante")) {
            val carregado = RascunhoCredenciamento.de(JSONObject("""
                {"dados":{"modalidade":"$modalidade","$secao":{"endereco":{"cidade":"Curitiba","uf":"PR"}}}}
            """))
            assertEquals("Curitiba", carregado["$secao.endereco.cidade"])
            assertEquals("PR", carregado.json().getJSONObject(secao).getJSONObject("endereco").getString("uf"))
            val atualizado = carregado.atualizar(Municipio(3550308, "São Paulo", "SP").camposEndereco("$secao.endereco"))
            val endereco = atualizado.json().getJSONObject(secao).getJSONObject("endereco")
            assertEquals("São Paulo", endereco.getString("cidade"))
            assertEquals("SP", endereco.getString("uf"))
        }
    }

    @Test fun preservaCamposDeSiteEPontoDeReferenciaNoContrato() {
        for ((modalidade, secao) in listOf("INDIVIDUAL" to "pessoaFisica", "CORPORATION" to "empresa")) {
            val dados = RascunhoCredenciamento(mapOf("modalidade" to modalidade,
                "$secao.siteUrl" to "https://example.test/profissional", "$secao.endereco.pontoReferencia" to "Em frente à praça"))
            val secaoJson = dados.json().getJSONObject(secao)
            assertEquals("https://example.test/profissional", secaoJson.getString("siteUrl"))
            assertEquals("Em frente à praça", secaoJson.getJSONObject("endereco").getString("pontoReferencia"))
        }
        val representante = RascunhoCredenciamento(mapOf("modalidade" to "CORPORATION",
            "representante.endereco.pontoReferencia" to "Próximo ao mercado"))
        assertEquals("Próximo ao mercado", representante.json().getJSONObject("representante")
            .getJSONObject("endereco").getString("pontoReferencia"))
    }

    @Test fun validaCpfECnpjPelosDigitosVerificadores() {
        assertTrue(documentoCredenciamentoValido(normalizarDocumento("529.982.247-25")))
        assertTrue(documentoCredenciamentoValido(normalizarDocumento("11.222.333/0001-81")))
        assertTrue(documentoCredenciamentoValido(normalizarDocumento("12.abc.345/01de-35")))
        assertFalse(documentoCredenciamentoValido("52998224726"))
        assertFalse(documentoCredenciamentoValido("11222333000182"))
        assertFalse(documentoCredenciamentoValido("11111111111"))
        assertFalse(documentoCredenciamentoValido("11222333"))
    }

    @Test fun mantemOContratoDoDocumentoSemPontuacao() {
        val dados = RascunhoCredenciamento(mapOf("empresa.cnpj" to normalizarDocumento("11.222.333/0001-81")))
        assertEquals("11222333000181", dados.json().getJSONObject("empresa").getString("cnpj"))
    }

    @Test fun enviaApenasAModalidadeSelecionadaMantendoAContaEDeclaracoesExplicitas() {
        val dados = RascunhoCredenciamento(mapOf("modalidade" to "CORPORATION",
            "empresa.cnpj" to "11222333000181", "pessoaFisica.cpf" to "52998224725",
            "representante.representanteLegal" to "true", "contaBancaria.agencia" to "0001"))
        val pj = dados.json()
        assertFalse(pj.has("pessoaFisica"))
        val representante = pj.getJSONObject("representante")
        assertTrue(representante.getBoolean("representanteLegal"))
        assertFalse(representante.has("socioQsa"))
        val pf = dados.atualizar(mapOf("modalidade" to "INDIVIDUAL")).json()
        assertFalse(pf.has("empresa"))
        assertFalse(pf.has("representante"))
        assertEquals("52998224725", pf.getJSONObject("pessoaFisica").getString("cpf"))
        assertEquals("0001", pf.getJSONObject("contaBancaria").getString("agencia"))
    }

    @Test fun recuperaModalidadeEPendenciasDoBackend() {
        val dados = RascunhoCredenciamento.de(JSONObject("""
            {"dados":{"modalidade":"INDIVIDUAL","pessoaFisica":{"nome":"Pessoa de teste"}},
             "completude":{"apto":false,"faltantes":[{"campo":"pessoaFisica.cpf","mensagem":"Informe o CPF."}],
              "invalidos":[],"confirmacoes":[{"campo":"contaBancaria.documento","mensagem":"Confirme a titularidade."}]}}
        """))
        assertEquals(ModalidadeCredenciamento.INDIVIDUAL, dados.modalidade)
        assertEquals("Pessoa de teste", dados["pessoaFisica.nome"])
        assertEquals(false, dados.completude?.apto)
        assertEquals(listOf("pessoaFisica.cpf", "contaBancaria.documento"), dados.completude?.pendencias?.map { it.campo })
    }

    @Test fun carregaEEditaDadosIndependentementeDaSituacaoDoRecebedor() {
        for (situacao in SituacaoRecebedor.entries) {
            val aprovado = situacao == SituacaoRecebedor.CRIADO
            val reenvio = situacao == SituacaoRecebedor.AGUARDANDO_NOVA_SUBMISSAO
            val dados = RascunhoCredenciamento.de(JSONObject("""
                {"versao":3,"dados":{"modalidade":"INDIVIDUAL","pessoaFisica":{"nome":"Pessoa de teste"}},
                 "recebedor":{"ambiente":"DEV","versao":3,"situacao":"${situacao.name}",
                  "statusProvedor":${if (aprovado) "\"ACTIVE\"" else "null"},"pendencias":[],
                  "podeEnviar":$reenvio,"podeConsultar":$aprovado}}
            """))
            assertEquals(situacao, dados.recebedor?.situacao)
            assertEquals(aprovado, dados.recebedor?.aprovado)
            assertEquals(reenvio, dados.recebedor?.podeEnviar)
            assertEquals(3L, dados.versao)
            assertFalse(dados.atualizar(mapOf("pessoaFisica.nome" to "Pessoa de teste")).pendenteSalvar)
            val editado = dados.atualizar(mapOf("pessoaFisica.nome" to "Novo nome"))
                .validar(CompletudeCredenciamento(true, emptyList()))
            assertEquals("Novo nome", editado["pessoaFisica.nome"])
            assertEquals(3L, editado.versao)
            assertEquals(dados.recebedor, editado.recebedor)
            assertFalse(editado.alterado)
            assertTrue(editado.pendenteSalvar)
            assertFalse(editado.json().has("versao"))
            assertFalse(editado.json().has("recebedor"))
        }
    }

    @Test fun distingueProcessamentoEsperaAprovacaoRecusaEEdicoesNoCartao() {
        val estados = listOf(
            Triple("NAO_ENVIADO", null, ApresentacaoCredenciamento.NAO_INICIADO),
            Triple("EM_ANALISE", null, ApresentacaoCredenciamento.PROCESSANDO),
            Triple("INDETERMINADO", null, ApresentacaoCredenciamento.INDETERMINADO),
            Triple("CRIADO", "REGISTRATION", ApresentacaoCredenciamento.AGUARDANDO),
            Triple("CRIADO", "AFFILIATION", ApresentacaoCredenciamento.AGUARDANDO),
            Triple("CRIADO", "ACTIVE", ApresentacaoCredenciamento.APROVADO),
            Triple("CRIADO", "REFUSED", ApresentacaoCredenciamento.RECUSADO),
            Triple("CRIADO", "BLOCKED", ApresentacaoCredenciamento.RESTRITO),
            Triple("FALHA", null, ApresentacaoCredenciamento.ERRO),
            Triple("AGUARDANDO_NOVA_SUBMISSAO", null, ApresentacaoCredenciamento.NOVA_SUBMISSAO)
        )
        for ((situacao, status, esperado) in estados) {
            val dados = JSONObject().put("ambiente", "DEV").put("situacao", situacao)
                .put("statusProvedor", status).put("pendencias", org.json.JSONArray()).put("podeReenviar", false)
            val recebedor = br.com.hubdeservicos.android.auth.RecebedorCredenciamento.de(dados)
            assertEquals(esperado, ApresentacaoCredenciamento.de(recebedor))
            assertFalse(recebedor.podeReenviar)
        }
    }

    @Test fun excluidoVoltaAPrimeiraEtapaSemDadosStatusOuIdentificadoresAnteriores() {
        for (situacao in SituacaoRecebedor.entries) {
            val dados = RascunhoCredenciamento.de(JSONObject("""
                {"id":"anterior","versao":3,"dados":{"modalidade":"INDIVIDUAL","pessoaFisica":{"nome":"Pessoa de teste"},
                 "contaBancaria":{"conta":"000123"}},"recebedor":{"ambiente":"DEV","situacao":"${situacao.name}",
                 "statusProvedor":"ACTIVE","pendencias":[]}}
            """))
            assertEquals("anterior", dados.atualizar(mapOf("pessoaFisica.nome" to "Editado"))
                .validar(CompletudeCredenciamento(true, emptyList())).id)
            val estado = Estado(tela = Tela.CREDENCIAMENTO, credenciamento = dados, recebedor = dados.recebedor,
                novoCredenciamento = false, etapaCredenciamento = 2, confirmarAlteracaoAprovada = true,
                camposInvalidos = listOf("pessoaFisica.nome")).reiniciarCredenciamento()
            assertEquals(Tela.CREDENCIAMENTO, estado.tela)
            assertEquals(0, estado.etapaCredenciamento)
            assertTrue(estado.novoCredenciamento)
            assertEquals(null, estado.recebedor)
            assertEquals(null, estado.confirmarAlteracaoAprovada)
            assertTrue(estado.camposInvalidos.isEmpty())
            val novo = requireNotNull(estado.credenciamento)
            assertEquals(null, novo.id)
            assertEquals(null, novo.versao)
            assertEquals(null, novo.modalidade)
            assertEquals(null, novo.completude)
            assertEquals("", novo["pessoaFisica.nome"])
            assertEquals("", novo["contaBancaria.conta"])
            assertEquals("{}", novo.json().toString())
        }
    }

    @Test fun sugereTelefoneSemSubstituirTelefoneSalvoEditadoOuApagado() {
        val telefone = "+553433334444"
        val rascunho = RascunhoCredenciamento(mapOf("modalidade" to "CORPORATION")).comTelefoneSugerido(telefone)
        assertEquals("", RascunhoCredenciamento(mapOf("modalidade" to "INDIVIDUAL"))
            .comTelefoneSugerido(telefone)["pessoaFisica.telefone"])
        assertEquals(telefone, rascunho["empresa.telefone"])
        assertEquals("", RascunhoCredenciamento().comTelefoneSugerido(null)["empresa.telefone"])
        for (valor in listOf("+5534999991111", "")) {
            val editado = rascunho.atualizar(mapOf("empresa.telefone" to valor))
            assertEquals(valor, editado.comTelefoneSugerido(telefone)["empresa.telefone"])
        }
    }
}
