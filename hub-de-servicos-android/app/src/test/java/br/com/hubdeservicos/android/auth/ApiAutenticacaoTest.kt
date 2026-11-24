package br.com.hubdeservicos.android.auth

import java.math.BigDecimal
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test

class ApiAutenticacaoTest {
    @Test
    fun usaRotasJsonEBearerDaApiMovel() {
        ServerSocket(0, 8, InetAddress.getLoopbackAddress()).use { servidor ->
            val executor = Executors.newSingleThreadExecutor()
            val usuario = """{"id":"id-1","email":"pessoa@example.test"}"""
            val tokens = """{"tokenAcesso":"jwt","tokenAtualizacao":"refresh"}"""
            val login = """{"tokenAcesso":"jwt","tokenAtualizacao":"refresh","usuario":$usuario}"""
            val prefixo = "/api/v1/app/autenticacao"
            val recebedor = "/api/v1/app/credenciamentos/STONE/recebedor"
            val casos = listOf(
                Caso("POST $prefixo/cadastro", 201, usuario, campo = "email" to "pessoa@example.test"),
                Caso("POST $prefixo/login", 200, login, campo = "senha" to "senha-de-teste"),
                Caso("POST $prefixo/login", 401, "{}"),
                Caso("POST $prefixo/renovar", 200, tokens, campo = "tokenAtualizacao" to "refresh"),
                Caso("GET $prefixo/eu", 200, usuario, autenticado = true),
                Caso("POST $prefixo/logout", 204, "", autenticado = true),
                Caso("GET $recebedor", 200, """{"ambiente":"DEV","situacao":"NAO_ENVIADO","pendencias":[]}""",
                    autenticado = true),
                Caso("POST $recebedor", 422, """{"detail":"Resolva as pendências do credenciamento.","completude":{
                    "apto":false,"faltantes":[{"campo":"pessoaFisica.tipoTelefone","mensagem":"Selecione o tipo de telefone."}],
                    "invalidos":[],"confirmacoes":[]}}""", autenticado = true, validarCorpo = { corpo ->
                        assertEquals("DEV", corpo.getString("ambiente"))
                        val dados = corpo.getJSONObject("dados")
                        assertEquals("INDIVIDUAL", dados.getString("modalidade"))
                        assertEquals("001", dados.getJSONObject("contaBancaria").get("banco"))
                    }),
                Caso("POST $recebedor/consulta", 200, """{"ambiente":"DEV","situacao":"CRIADO",
                    "statusProvedor":"REGISTRATION","pendencias":[]}""", autenticado = true),
                Caso("POST $recebedor/reenvio", 200, """{"ambiente":"DEV","situacao":"EM_ANALISE",
                    "pendencias":[],"tentativa":"atual","versao":3,"emProcessamento":true,"podeReenviar":false,
                    "envioAceito":false}""", autenticado = true, validarCorpo = { corpo ->
                        assertEquals(setOf("ambiente", "versao", "tentativa"), corpo.keys().asSequence().toSet())
                        assertEquals("DEV", corpo.getString("ambiente"))
                        assertEquals(3L, corpo.getLong("versao"))
                        assertEquals("anterior", corpo.getString("tentativa"))
                    }),
                Caso("DELETE /api/v1/app/credenciamentos/STONE", 204, "", autenticado = true,
                    cabecalhosEsperados = mapOf("x-credenciamento-id" to "formulario-local", "x-versao-credenciamento" to "3"))
            )
            val requisicoes = executor.submit {
                for (caso in casos) {
                    servidor.accept().use { socket ->
                        val leitura = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                        val rota = leitura.readLine().substringBeforeLast(' ')
                        val cabecalhos = generateSequence { leitura.readLine()?.takeUnless { it.isEmpty() } }
                            .associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
                        val tamanho = cabecalhos["content-length"]?.toInt() ?: 0
                        val corpo = CharArray(tamanho)
                        for (indice in corpo.indices) corpo[indice] = leitura.read().toChar()
                        val resposta = caso.resposta.toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().apply {
                            write(("HTTP/1.1 ${caso.status} Teste\r\n" +
                                "Content-Type: application/json\r\nContent-Length: ${resposta.size}\r\n" +
                                "Connection: close\r\n\r\n").toByteArray())
                            write(resposta)
                            flush()
                        }
                        assertEquals(caso.rota, rota)
                        caso.campo?.let { (nome, valor) ->
                            val json = JSONObject(String(corpo))
                            assertEquals(valor, json.getString(nome))
                            if (rota.endsWith("/cadastro")) assertEquals(setOf("email", "senha"), json.keys().asSequence().toSet())
                        }
                        caso.validarCorpo?.invoke(JSONObject(String(corpo)))
                        if (caso.autenticado) assertEquals("Bearer jwt", cabecalhos["authorization"])
                        caso.cabecalhosEsperados.forEach { (nome, valor) -> assertEquals(valor, cabecalhos[nome]) }
                    }
                }
            }
            try {
                val api = ApiAutenticacao("http://127.0.0.1:${servidor.localPort}")
                val email = "pessoa@example.test"
                assertEquals("id-1", api.cadastrar(email, "senha-de-teste").id)
                assertEquals(email, api.entrar(email, "senha-de-teste").usuario.email)
                assertEquals(401, assertThrows(FalhaApi::class.java) { api.entrar(email, "incorreta") }.status)
                assertEquals("jwt", api.renovar("refresh").acesso)
                assertEquals(email, api.consultar("jwt").email)
                api.sair("jwt")
                val provedor = ProvedorCredenciamento.STONE
                assertEquals(AmbienteRecebedor.DEV, api.consultarRecebedor("jwt", provedor).ambiente)
                val dados = RascunhoCredenciamento(mapOf("modalidade" to "INDIVIDUAL", "contaBancaria.banco" to "001"))
                val falha = assertThrows(FalhaApi::class.java) {
                    api.enviarRecebedor("jwt", provedor, AmbienteRecebedor.DEV, dados)
                }
                assertEquals("pessoaFisica.tipoTelefone", falha.completude?.pendencias?.single()?.campo)
                val criado = api.atualizarRecebedor("jwt", provedor)
                assertEquals(SituacaoRecebedor.CRIADO, criado.situacao)
                assertEquals("REGISTRATION", criado.statusProvedor)
                val reenviado = api.reenviarRecebedor("jwt", provedor, criado.copy(versao = 3, tentativa = "anterior"))
                assertEquals(SituacaoRecebedor.EM_ANALISE, reenviado.situacao)
                assertEquals("atual", reenviado.tentativa)
                assertEquals(true, reenviado.emProcessamento)
                assertEquals(false, reenviado.envioAceito)
                assertEquals(false, reenviado.podeReenviar)
                api.excluirCredenciamento("jwt", provedor, RascunhoCredenciamento(
                    mapOf("modalidade" to "INDIVIDUAL"), versao = 3, id = "formulario-local"))
                requisicoes.get(5, TimeUnit.SECONDS)
            } finally {
                servidor.close()
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun integraClienteComBackendDoHub() {
        val origem = System.getenv("HUB_ANDROID_API_TESTE")
        assumeTrue("Use uma instância do backend com dados de teste.", !origem.isNullOrBlank())
        val api = ApiAutenticacao(requireNotNull(origem))
        val email = "android-${UUID.randomUUID()}@example.test"
        val senha = "Senha-de-teste!1996"
        val criado = api.cadastrar(email, senha)
        assertEquals(email, criado.email)
        assertEquals(409, assertThrows(FalhaApi::class.java) { api.cadastrar(email, senha) }.status)
        val armazenamento = object : ArmazenamentoSessao {
            private var valor: String? = null
            override fun ler() = valor
            override fun gravar(atualizacao: String) { valor = atualizacao }
            override fun apagar() { valor = null }
        }
        assertEquals(criado, SessaoApp(api, armazenamento).entrar(email, senha))
        fun perfil(acesso: String) = api.consultarCadastro(acesso)
        val sessao = SessaoApp(api, armazenamento)
        assertEquals(listOf("NOME", "TELEFONE"), sessao.autenticado(::perfil).pendencias)
        val telefone = "349" + java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000)
        val atualizado = sessao.autenticado { api.atualizarCadastro(it, "Ana Teste", telefone) }
        assertEquals("Ana Teste", atualizado.nome)
        assertEquals("+55" + telefone, atualizado.telefone)
        assertEquals(SituacaoTelefone.PENDENTE, atualizado.situacaoTelefone)
        assertEquals(listOf("CONFIRMACAO_TELEFONE"), atualizado.pendencias)
        assertEquals(409, assertThrows(FalhaApi::class.java) {
            sessao.autenticado { api.confirmarTelefone(it, UUID.randomUUID().toString(), "000000") }
        }.status)
        assertEquals(SituacaoTelefone.PENDENTE, sessao.autenticado(::perfil).situacaoTelefone)
        sessao.autenticado { api.selecionarPapel(it, PapelUsuario.EMPRESA, true) }
        val perfis = sessao.autenticado { api.selecionarPapel(it, PapelUsuario.TRABALHADOR, true) }
        assertEquals(setOf(PapelUsuario.EMPRESA, PapelUsuario.TRABALHADOR), perfis.papeis)
        sessao.autenticado { api.atualizarCadastro(it, "Ana Teste", telefone, Remuneracao(BigDecimal("25.15"))) }
        val municipio = sessao.autenticado { api.registrarMunicipio(it, "Uberaba", "Minas Gerais") }
        assertEquals(Municipio(3170107, "Uberaba", "MG"), municipio.municipio)
        assertEquals(BigDecimal("25.15"), municipio.remuneracao.valorHora)
        assertEquals(422, assertThrows(FalhaApi::class.java) {
            sessao.autenticado { api.registrarMunicipio(it, "Inexistente", "MG") }
        }.status)
        sessao.autenticado { api.selecionarPapel(it, PapelUsuario.EMPRESA, false) }
        val limpo = sessao.autenticado { api.atualizarCadastro(it, "Ana Teste", telefone, Remuneracao()) }
        assertEquals(Remuneracao(), limpo.remuneracao)
        assertEquals(setOf(PapelUsuario.TRABALHADOR), limpo.papeis)
        val sessaoRestaurada = SessaoApp(api, armazenamento)
        assertEquals(criado, sessaoRestaurada.restaurar())
        assertEquals(Municipio(3170107, "Uberaba", "MG"), sessaoRestaurada.autenticado(::perfil).municipio)
        assertEquals(setOf(PapelUsuario.TRABALHADOR), sessaoRestaurada.autenticado(::perfil).papeis)
        val municipios = sessaoRestaurada.autenticado { api.pesquisarMunicipios(it, "Uberlândia") }
        assertEquals(Municipio(3170206, "Uberlândia", "MG"), municipios.first())
        val manual = sessaoRestaurada.autenticado { api.selecionarMunicipio(it, municipios.first().codigoIbge) }
        assertEquals(Municipio(3170206, "Uberlândia", "MG", manual = true), manual.municipio)
        assertEquals(manual.municipio, sessaoRestaurada.autenticado {
            api.registrarMunicipio(it, "Uberaba", "MG")
        }.municipio)
        assertEquals(manual.municipio, sessaoRestaurada.autenticado(::perfil).municipio)
        val provedor = ProvedorCredenciamento.STONE
        assertEquals(SituacaoRecebedor.NAO_ENVIADO, sessaoRestaurada.autenticado { api.consultarRecebedor(it, provedor) }.situacao)
        assertEquals(null, sessaoRestaurada.autenticado { api.consultarCredenciamento(it, provedor) })
        val rascunhoPf = RascunhoCredenciamento(mapOf("modalidade" to "INDIVIDUAL", "pessoaFisica.nome" to "Pessoa de teste",
            "contaBancaria.agencia" to "0001"))
        val salvoPf = sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, true, rascunhoPf) }
        assertEquals(ModalidadeCredenciamento.INDIVIDUAL, salvoPf.modalidade)
        assertEquals(false, salvoPf.completude?.apto)
        assertEquals(0L, salvoPf.versao)
        assertEquals(SituacaoRecebedor.NAO_ENVIADO, salvoPf.recebedor?.situacao)
        assertEquals("Pessoa de teste", sessaoRestaurada.autenticado {
            api.consultarCredenciamento(it, provedor)
        }?.get("pessoaFisica.nome"))
        assertEquals(false, sessaoRestaurada.autenticado { api.validarCredenciamento(it, provedor, rascunhoPf) }.apto)
        val rascunhoEditado = salvoPf.atualizar(mapOf("pessoaFisica.nome" to "Nome revisado"))
        val pfEditado = sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, false, rascunhoEditado) }
        assertEquals(1L, pfEditado.versao)
        val conflito = assertThrows(FalhaApi::class.java) {
            sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, false, salvoPf) }
        }
        assertEquals(409, conflito.status)
        assertEquals("VERSAO_CREDENCIAMENTO_DESATUALIZADA", conflito.codigo)
        sessaoRestaurada.autenticado { api.selecionarPapel(it, PapelUsuario.EMPRESA, true) }

        val endereco = JSONObject("""{"cep":"00000000","logradouro":"Teste","numero":"0","bairro":"Teste","cidade":"Teste","uf":"MG"}""")
        val cpf = StringBuilder("900000001")
        for (tamanho in listOf(9, 10)) {
            val resto = (0 until tamanho).sumOf { (cpf[it] - '0') * (tamanho + 1 - it) } % 11
            cpf.append(if (resto < 2) 0 else 11 - resto)
        }
        val empresa = JSONObject("""{"razaoSocial":"Teste","cnpj":"12.ABC.345/01DE-35","email":"empresa@example.test","telefone":"34999999999","faturamento":"100.00"}""")
            .put("endereco", endereco)
        val representante = JSONObject("""{"nome":"Teste","nascimento":"2000-01-01","profissao":"Teste","renda":"1.00"}""")
            .put("cpf", cpf.toString()).put("endereco", endereco)
        val conta = JSONObject("""{"titular":"Teste","tipo":"CORRENTE","banco":"001","agencia":"0007","conta":"0000123"}""")
            .put("documento", cpf.toString())
        val dados = RascunhoCredenciamento.de(JSONObject().put("dados",
            JSONObject().put("empresa", empresa).put("representante", representante).put("contaBancaria", conta)))
        val gravado = sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, false, dados) }
        assertEquals(ModalidadeCredenciamento.CORPORATION, gravado.modalidade)
        assertEquals("0007", gravado["contaBancaria.agencia"])
        val editado = gravado.atualizar(mapOf("contaBancaria.conta" to "0000654"))
        sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, false, editado) }
        val lido = sessaoRestaurada.autenticado { api.consultarCredenciamento(it, provedor) }
        assertEquals("0000654", lido?.get("contaBancaria.conta"))
        requireNotNull(lido)
        sessaoRestaurada.autenticado { api.excluirCredenciamento(it, provedor, lido) }
        assertEquals(null, sessaoRestaurada.autenticado { api.consultarCredenciamento(it, provedor) })
        sessaoRestaurada.autenticado { api.excluirCredenciamento(it, provedor, lido) }
        val novo = sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, true,
            RascunhoCredenciamento(mapOf("modalidade" to "INDIVIDUAL", "pessoaFisica.nome" to "Novo processo"))) }
        org.junit.Assert.assertNotEquals(lido.id, novo.id)
        assertEquals("", novo["contaBancaria.conta"])
        assertEquals(409, assertThrows(FalhaApi::class.java) {
            sessaoRestaurada.autenticado { api.salvarCredenciamento(it, provedor, false, lido) }
        }.status)
        sessaoRestaurada.sair()
        assertEquals(null, SessaoApp(api, armazenamento).restaurar())
    }

    private data class Caso(
        val rota: String,
        val status: Int,
        val resposta: String,
        val campo: Pair<String, String>? = null,
        val autenticado: Boolean = false,
        val validarCorpo: ((JSONObject) -> Unit)? = null,
        val cabecalhosEsperados: Map<String, String> = emptyMap()
    )
}
