package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.UnidadeMonetaria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.ErrorResponseException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PagarmeRecebedorTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final String CPF = "529.982.247-25";
    private static final String CNPJ = "11.222.333/0001-81";
    private static final UUID USUARIO = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final String CODIGO = "22222222-2222-4222-8222-222222222222";
    private static final EnderecoBean ENDERECO;
    static {
        EnderecoBean enderecoPreparado = new EnderecoBean();
        enderecoPreparado.setCep("01001-000");
        enderecoPreparado.setLogradouro("Logradouro de teste");
        enderecoPreparado.setNumero("1");
        enderecoPreparado.setComplemento("Bloco A");
        enderecoPreparado.setBairro("Centro");
        enderecoPreparado.setCidade("Cidade de teste");
        enderecoPreparado.setUf("SP");
        enderecoPreparado.setPontoReferencia("Referência de teste");
        ENDERECO = enderecoPreparado;
    }
    private final ValidacaoCredenciamento validador = new ValidacaoCredenciamento(RELOGIO, FACTORY.getValidator());
    private final MapeadorPagarme mapeador = new MapeadorPagarme(validador);
    private final ObjectMapper json;
    {
        var objectMapperPreparado = new ObjectMapper();
        json = objectMapperPreparado.findAndRegisterModules();
    }

    @AfterAll
    static void fechar() {
        FACTORY.close();
    }

    @Test
    void montaPfComValoresNumericosEContaEmTexto() {
        DadosBean dadosPreparado = new DadosBean();
        dadosPreparado.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado.setPessoaFisica(pessoa(CPF, ENDERECO, "pessoa@example.invalid"));
        dadosPreparado.setEmpresa(null);
        dadosPreparado.setRepresentante(null);
        dadosPreparado.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
        dadosPreparado.normalizarLegado();
        var dados = dadosPreparado;
        assertThat(validador.avaliar(dados).getApto()).isTrue();
        var payload = mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO);
        JsonNode corpo = json.valueToTree(payload);
        var registro = corpo.path("register_information");
        var conta = corpo.path("default_bank_account");
        assertThat(registro.path("type").asText()).isEqualTo("individual");
        assertThat(registro.path("document").asText()).isEqualTo("52998224725");
        assertThat(registro.path("birthdate").asText()).isEqualTo("10/03/1990");
        assertThat(registro.path("monthly_income").isNumber()).isTrue();
        assertThat(registro.path("monthly_income").intValue()).isEqualTo(123_456);
        assertThat(registro.path("address").path("zip_code").asText()).isEqualTo("01001000");
        assertThat(registro.path("phone_numbers").path(0).path("ddd").asText()).isEqualTo("34");
        assertThat(registro.path("phone_numbers").path(0).path("number").asText()).isEqualTo("999999999");
        assertThat(conta.path("branch_number").asText()).isEqualTo("0001");
        assertThat(conta.path("account_number").asText()).isEqualTo("000045");
        assertThat(conta.path("account_check_digit").asText()).isEqualTo("X");
        assertThat(conta.path("holder_type").asText()).isEqualTo("individual");
        assertThat(registro.has("managing_partners")).isFalse();
        conferirPadroes(corpo, USUARIO);
        assertThat(registro.has("mother_name")).isFalse();
        assertThat(payload.toString()).doesNotContain("000045", "pessoa@", "529");
    }

    @Test
    void montaPjComUmSocioEMapeamentoDosNomes() {
        DadosBean dadosPreparado2 = new DadosBean();
        dadosPreparado2.setModalidade(Modalidade.CORPORATION);
        dadosPreparado2.setPessoaFisica(null);
        var bigDecimalPreparado = new BigDecimal("40000.50");
        dadosPreparado2.setEmpresa(empresa(bigDecimalPreparado));
        dadosPreparado2.setRepresentante(representante(true, true));
        dadosPreparado2.setContaBancaria(conta(CNPJ, TipoTitular.COMPANY));
        dadosPreparado2.normalizarLegado();
        var dados = dadosPreparado2;
        assertThat(validador.avaliar(dados).getApto()).isTrue();
        JsonNode corpo = json.valueToTree(mapeador.preparar(dados, UnidadeMonetaria.REAIS, USUARIO, CODIGO));
        var registro = corpo.path("register_information");
        assertThat(registro.path("company_name").asText()).isEqualTo("Nome fantasia de teste");
        assertThat(registro.path("trading_name").asText()).isEqualTo("Razão social de teste");
        assertThat(registro.path("corporation_type").asText()).isEqualTo("LTDA");
        assertThat(registro.path("founding_date").asText()).isEqualTo("2010-03-20");
        assertThat(registro.path("annual_revenue").decimalValue()).isEqualByComparingTo("40000.50");
        var socios = registro.path("managing_partners");
        assertThat(socios.size()).isEqualTo(1);
        assertThat(socios.path(0).path("self_declared_legal_representative").booleanValue()).isTrue();
        assertThat(socios.path(0).path("email").asText()).isEqualTo("representante@example.invalid");
        assertThat(socios.path(0).path("birthdate").asText()).isEqualTo("10/03/1990");
        assertThat(socios.path(0).has("socioQsa")).isFalse();
        assertThat(socios.path(0).has("mother_name")).isFalse();
        conferirPadroes(corpo, USUARIO);
        assertThat(corpo.path("default_bank_account").path("holder_type").asText()).isEqualTo("company");
    }

    @Test
    void indicaCamposInvalidosFaltantesEConfirmacoesSemMontarPayload() {
        EnderecoBean enderecoPreparado2 = new EnderecoBean();
        enderecoPreparado2.setCep("0123");
        enderecoPreparado2.setLogradouro("Teste");
        enderecoPreparado2.setNumero("1");
        enderecoPreparado2.setComplemento("");
        enderecoPreparado2.setBairro("Bairro");
        enderecoPreparado2.setCidade("Cidade");
        enderecoPreparado2.setUf("XX");
        enderecoPreparado2.setPontoReferencia(null);
        var endereco = enderecoPreparado2;
        DadosBean dadosPreparado3 = new DadosBean();
        dadosPreparado3.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado3.setPessoaFisica(pessoa("00000000000", endereco, "invalido"));
        dadosPreparado3.setEmpresa(null);
        dadosPreparado3.setRepresentante(null);
        dadosPreparado3.setContaBancaria(conta(CNPJ, TipoTitular.INDIVIDUAL));
        dadosPreparado3.normalizarLegado();
        var dados = dadosPreparado3;
        var resultado = validador.avaliar(dados);
        assertThat(resultado.getApto()).isFalse();
        assertThat(resultado.getInvalidos()).extracting(PendenciaBean::getCampo).contains("pessoaFisica.cpf",
                "pessoaFisica.email", "pessoaFisica.endereco.cep", "pessoaFisica.endereco.uf",
                "contaBancaria.documento");
        assertThat(resultado.getFaltantes()).extracting(PendenciaBean::getCampo)
                .contains("pessoaFisica.endereco.pontoReferencia", "pessoaFisica.endereco.complemento");
        assertThatThrownBy(() -> mapeador.preparar(dados, UnidadeMonetaria.REAIS, USUARIO, CODIGO))
                .isInstanceOf(ErrorResponseException.class).hasMessageNotContaining("00000000000");
    }

    @Test
    void bloqueiaPeriodoLegadoDeclaracaoIncompletaEContaComOutroDocumento() {
        DadosBean dadosPreparado4 = new DadosBean();
        dadosPreparado4.setModalidade(Modalidade.CORPORATION);
        dadosPreparado4.setPessoaFisica(null);
        dadosPreparado4.setEmpresa(empresa(null));
        dadosPreparado4.setRepresentante(representante(false, null));
        dadosPreparado4.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
        dadosPreparado4.normalizarLegado();
        var dados = dadosPreparado4;
        var resultado = validador.avaliar(dados);
        assertThat(resultado.getApto()).isFalse();
        assertThat(resultado.getConfirmacoes()).extracting(PendenciaBean::getCampo).containsExactlyInAnyOrder(
                "empresa.faturamentoAnual", "representante.representanteLegal", "representante.socioQsa",
                "contaBancaria.documento");
        DadosBean dadosPreparado5 = new DadosBean();
        dadosPreparado5.setModalidade(Modalidade.CORPORATION);
        dadosPreparado5.setPessoaFisica(null);
        var bigDecimalPreparado2 = new BigDecimal("-1");
        dadosPreparado5.setEmpresa(empresa(bigDecimalPreparado2));
        dadosPreparado5.setRepresentante(representante(true, true));
        dadosPreparado5.setContaBancaria(conta(CNPJ, TipoTitular.COMPANY));
        dadosPreparado5.normalizarLegado();
        assertThat(validador.avaliar(dadosPreparado5).getInvalidos()).extracting(PendenciaBean::getCampo)
                .contains("empresa.faturamentoAnual");
    }

    @Test
    void lePjLegadaSemReinterpretarValores() throws Exception {
        var dados = json.readValue("""
                {"empresa":{"razaoSocial":"Teste","faturamento":"100.00"},
                "representante":{"nome":"Teste","renda":"50.00"}}
                """, DadosBean.class);
        assertThat(dados.getModalidade()).isEqualTo(Modalidade.CORPORATION);
        assertThat(dados.getEmpresa().getFaturamento()).isEqualByComparingTo("100.00");
        assertThat(dados.getEmpresa().getFaturamentoAnual()).isNull();
        assertThat(dados.getRepresentante().getRenda()).isEqualByComparingTo("50.00");
        assertThat(dados.getRepresentante().getRendaMensal()).isNull();
        assertThat(dados.getRepresentante().getSocioQsa()).isNull();
    }

    private static PessoaFisicaBean pessoa(String documento, EnderecoBean endereco, String email) {
        PessoaFisicaBean pessoaFisicaPreparado = new PessoaFisicaBean();
        pessoaFisicaPreparado.setNome("Pessoa de teste");
        pessoaFisicaPreparado.setCpf(documento);
        pessoaFisicaPreparado.setEmail(email);
        pessoaFisicaPreparado.setTelefone("+55 34 99999-9999");
        pessoaFisicaPreparado.setTipoTelefone(TipoTelefone.CELULAR);
        pessoaFisicaPreparado.setSiteUrl("https://example.invalid");
        pessoaFisicaPreparado.setNascimento(LocalDate.parse("1990-03-10"));
        pessoaFisicaPreparado.setProfissao("Teste");
        var bigDecimalPreparado3 = new BigDecimal("1234.56");
        pessoaFisicaPreparado.setRendaMensal(bigDecimalPreparado3);
        pessoaFisicaPreparado.setEndereco(endereco);
        return pessoaFisicaPreparado;
    }

    @ParameterizedTest
    @CsvSource({ "1985-11-16,16/11/1985", "1985-04-03,03/04/1985", "2000-12-01,01/12/2000" })
    void formataNascimentoSemAlterarDataInternaOuDemaisCampos(String entrada, String esperado) {
        var original = pessoa(CPF, ENDERECO, "pessoa@example.invalid");
        var nascimento = LocalDate.parse(entrada);
        PessoaFisicaBean pessoaFisicaPreparado2 = new PessoaFisicaBean();
        pessoaFisicaPreparado2.setNome(original.getNome());
        pessoaFisicaPreparado2.setCpf(original.getCpf());
        pessoaFisicaPreparado2.setEmail(original.getEmail());
        pessoaFisicaPreparado2.setTelefone(original.getTelefone());
        pessoaFisicaPreparado2.setTipoTelefone(original.getTipoTelefone());
        pessoaFisicaPreparado2.setSiteUrl(original.getSiteUrl());
        pessoaFisicaPreparado2.setNascimento(nascimento);
        pessoaFisicaPreparado2.setProfissao(original.getProfissao());
        pessoaFisicaPreparado2.setRendaMensal(original.getRendaMensal());
        pessoaFisicaPreparado2.setEndereco(original.getEndereco());
        var pessoa = pessoaFisicaPreparado2;
        DadosBean dadosPreparado6 = new DadosBean();
        dadosPreparado6.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado6.setPessoaFisica(pessoa);
        dadosPreparado6.setEmpresa(null);
        dadosPreparado6.setRepresentante(null);
        dadosPreparado6.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
        dadosPreparado6.normalizarLegado();
        var dados = dadosPreparado6;
        var corpo = json.valueToTree(mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        assertThat(corpo.path("register_information").path("birthdate").asText()).isEqualTo(esperado);
        assertThat(dados.getPessoaFisica().getNascimento()).isEqualTo(nascimento);
        DadosBean dadosPreparado7 = new DadosBean();
        dadosPreparado7.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado7.setPessoaFisica(original);
        dadosPreparado7.setEmpresa(null);
        dadosPreparado7.setRepresentante(null);
        dadosPreparado7.setContaBancaria(dados.getContaBancaria());
        dadosPreparado7.normalizarLegado();
        var referencia = json
                .valueToTree(mapeador.preparar(dadosPreparado7, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        ((com.fasterxml.jackson.databind.node.ObjectNode) referencia.path("register_information")).put("birthdate",
                esperado);
        assertThat(corpo).isEqualTo(referencia);

        var representante = representante(true, true);
        RepresentanteBean representantePreparado = new RepresentanteBean();
        representantePreparado.setNome(representante.getNome());
        representantePreparado.setCpf(representante.getCpf());
        representantePreparado.setEmail(representante.getEmail());
        representantePreparado.setTelefone(representante.getTelefone());
        representantePreparado.setTipoTelefone(representante.getTipoTelefone());
        representantePreparado.setNascimento(nascimento);
        representantePreparado.setProfissao(representante.getProfissao());
        representantePreparado.setRenda(representante.getRenda());
        representantePreparado.setRendaMensal(representante.getRendaMensal());
        representantePreparado.setRepresentanteLegal(representante.getRepresentanteLegal());
        representantePreparado.setSocioQsa(representante.getSocioQsa());
        representantePreparado.setEndereco(representante.getEndereco());
        var comNascimento = representantePreparado;
        DadosBean dadosPreparado8 = new DadosBean();
        dadosPreparado8.setModalidade(Modalidade.CORPORATION);
        dadosPreparado8.setPessoaFisica(null);
        var bigDecimalPreparado4 = new BigDecimal("40000.50");
        dadosPreparado8.setEmpresa(empresa(bigDecimalPreparado4));
        dadosPreparado8.setRepresentante(comNascimento);
        dadosPreparado8.setContaBancaria(conta(CNPJ, TipoTitular.COMPANY));
        dadosPreparado8.normalizarLegado();
        var pj = dadosPreparado8;
        var corpoPj = json.valueToTree(mapeador.preparar(pj, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        assertThat(corpoPj.path("register_information").path("managing_partners").path(0).path("birthdate").asText())
                .isEqualTo(esperado);
        assertThat(pj.getRepresentante().getNascimento()).isEqualTo(nascimento);
    }

    private static EmpresaBean empresa(BigDecimal anual) {
        EmpresaBean empresaPreparado = new EmpresaBean();
        empresaPreparado.setRazaoSocial("Razão social de teste");
        empresaPreparado.setNomeFantasia("Nome fantasia de teste");
        empresaPreparado.setCnpj(CNPJ);
        empresaPreparado.setEmail("empresa@example.invalid");
        empresaPreparado.setTelefone("34999999999");
        empresaPreparado.setTipoTelefone(TipoTelefone.CELULAR);
        var bigDecimalPreparado5 = new BigDecimal("30.00");
        empresaPreparado.setFaturamento(bigDecimalPreparado5);
        empresaPreparado.setFaturamentoAnual(anual);
        empresaPreparado.setSiteUrl("https://example.invalid");
        empresaPreparado.setTipoSocietario("LTDA");
        empresaPreparado.setDataFundacao(LocalDate.parse("2010-03-20"));
        empresaPreparado.setEndereco(ENDERECO);
        return empresaPreparado;
    }

    private static RepresentanteBean representante(Boolean legal, Boolean socio) {
        RepresentanteBean representantePreparado2 = new RepresentanteBean();
        representantePreparado2.setNome("Representante de teste");
        representantePreparado2.setCpf(CPF);
        representantePreparado2.setEmail("representante@example.invalid");
        representantePreparado2.setTelefone("34999999999");
        representantePreparado2.setTipoTelefone(TipoTelefone.CELULAR);
        representantePreparado2.setNascimento(LocalDate.parse("1990-03-10"));
        representantePreparado2.setProfissao("Teste");
        representantePreparado2.setRenda(null);
        var bigDecimalPreparado6 = new BigDecimal("1100.00");
        representantePreparado2.setRendaMensal(bigDecimalPreparado6);
        representantePreparado2.setRepresentanteLegal(legal);
        representantePreparado2.setSocioQsa(socio);
        representantePreparado2.setEndereco(ENDERECO);
        return representantePreparado2;
    }

    private static ContaBancariaBean conta(String documento, TipoTitular tipo) {
        ContaBancariaBean contaBancariaPreparado = new ContaBancariaBean();
        contaBancariaPreparado.setTitular("Nome de teste");
        contaBancariaPreparado.setDocumento(documento);
        contaBancariaPreparado.setTipoTitular(tipo);
        contaBancariaPreparado.setTipo(TipoConta.CORRENTE);
        contaBancariaPreparado.setBanco("001");
        contaBancariaPreparado.setAgencia("0001");
        contaBancariaPreparado.setDigitoAgencia(null);
        contaBancariaPreparado.setConta("000045");
        contaBancariaPreparado.setDigitoConta("x");
        return contaBancariaPreparado;
    }

    private static void conferirPadroes(JsonNode corpo, UUID usuario) {
        assertThat(corpo.path("code").asText()).isEqualTo(CODIGO);
        assertThat(corpo.path("automatic_anticipation_settings").toString()).isEqualTo("{\"enabled\":false}");
        assertThat(corpo.path("transfer_settings").path("transfer_enabled").isBoolean()).isTrue();
        assertThat(corpo.path("transfer_settings").path("transfer_enabled").booleanValue()).isTrue();
        assertThat(corpo.path("transfer_settings").path("transfer_interval").asText()).isEqualTo("Daily");
        assertThat(corpo.path("transfer_settings").path("transfer_day").isIntegralNumber()).isTrue();
        assertThat(corpo.path("transfer_settings").path("transfer_day").intValue()).isZero();
        assertThat(corpo.path("metadata").toString()).isEqualTo("{\"companyToken\":\"" + usuario + "\"}");
    }

    @Test
    void utilizaIdentificadorDoUsuarioERejeitaAusenciaDaIdentidade() {
        DadosBean dadosPreparado9 = new DadosBean();
        dadosPreparado9.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado9.setPessoaFisica(pessoa(CPF, ENDERECO, "pessoa@example.invalid"));
        dadosPreparado9.setEmpresa(null);
        dadosPreparado9.setRepresentante(null);
        dadosPreparado9.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
        dadosPreparado9.normalizarLegado();
        var dados = dadosPreparado9;
        var outroUsuario = UUID.randomUUID();
        conferirPadroes(json.valueToTree(mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, outroUsuario, CODIGO)),
                outroUsuario);
        assertThatThrownBy(() -> mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, null, CODIGO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void descartaNomeDaMaeLegadoSemInvalidarDadosNemReintroduzirNoPayload() throws Exception {
        for (var modalidade : Modalidade.values()) {
            br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.DadosBean resultadoCondicionalPreparado;
            if (modalidade == Modalidade.INDIVIDUAL) {
                DadosBean dadosPreparado10 = new DadosBean();
                dadosPreparado10.setModalidade(modalidade);
                dadosPreparado10.setPessoaFisica(pessoa(CPF, ENDERECO, "pessoa@example.invalid"));
                dadosPreparado10.setEmpresa(null);
                dadosPreparado10.setRepresentante(null);
                dadosPreparado10.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
                dadosPreparado10.normalizarLegado();
                resultadoCondicionalPreparado = dadosPreparado10;
            } else {
                DadosBean dadosPreparado11 = new DadosBean();
                dadosPreparado11.setModalidade(modalidade);
                dadosPreparado11.setPessoaFisica(null);
                var bigDecimalPreparado7 = new BigDecimal("40000.50");
                dadosPreparado11.setEmpresa(empresa(bigDecimalPreparado7));
                dadosPreparado11.setRepresentante(representante(true, true));
                dadosPreparado11.setContaBancaria(conta(CNPJ, TipoTitular.COMPANY));
                dadosPreparado11.normalizarLegado();
                resultadoCondicionalPreparado = dadosPreparado11;
            }
            var dados = resultadoCondicionalPreparado;
            var arvore = json.valueToTree(dados);
            var pessoa = (com.fasterxml.jackson.databind.node.ObjectNode) arvore
                    .path(modalidade == Modalidade.INDIVIDUAL ? "pessoaFisica" : "representante");
            pessoa.put("nomeMae", "Nome legado ".repeat(100));
            pessoa.put("mother_name", "Campo de cliente antigo");
            var recuperado = json.treeToValue(arvore, DadosBean.class);
            assertThat(validador.avaliar(recuperado).getApto()).isTrue();
            assertThat(json.writeValueAsString(recuperado)).doesNotContain("nomeMae", "mother_name");
            var comparacaoCredenciamentoPreparado = new ComparacaoCredenciamento(json);
            assertThat(comparacaoCredenciamentoPreparado.iguais(dados, recuperado)).isTrue();
            assertThat(
                    json.writeValueAsString(mapeador.preparar(recuperado, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO)))
                    .doesNotContain("mother_name", "nomeMae");
        }
    }

    @Test
    void previaAdministrativaUsaSolicitantePersistidoECodigoDoCredenciamentoNosDoisAmbientes() {
        DadosBean dadosPreparado12 = new DadosBean();
        dadosPreparado12.setModalidade(Modalidade.INDIVIDUAL);
        dadosPreparado12.setPessoaFisica(pessoa(CPF, ENDERECO, "pessoa@example.invalid"));
        dadosPreparado12.setEmpresa(null);
        dadosPreparado12.setRepresentante(null);
        dadosPreparado12.setContaBancaria(conta(CPF, TipoTitular.INDIVIDUAL));
        dadosPreparado12.normalizarLegado();
        var dados = dadosPreparado12;
        var id = UUID.fromString(CODIGO);
        var registro = new br.com.hubdeservicos.entity.credenciamento.CredenciamentoEntity(id, USUARIO,
                br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento.STONE, "conteudo-protegido",
                RELOGIO.instant());
        var repositorio = mock(br.com.hubdeservicos.repository.credenciamento.CredenciamentoRepository.class);
        when(repositorio.findById(id)).thenReturn(java.util.Optional.of(registro));
        var autenticacao = mock(AutenticacaoPagarme.class);
        var conta = mock(AutenticacaoPagarme.ContaVO.class);
        when(autenticacao.selecionarConta()).thenReturn(conta);
        var credenciamentoServicePreparado = new CredenciamentoService(repositorio, null, null, null, null, null, json,
                RELOGIO, validador, mapeador, autenticacao, null, null, null, null);
        var servico = spy(credenciamentoServicePreparado);
        doReturn(dados).when(servico).ler(registro);
        for (var ambiente : br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente.values()) {
            when(conta.getAmbiente()).thenReturn(ambiente);
            var previa = servico.preparar(id, UnidadeMonetaria.CENTAVOS);
            assertThat(previa.getAmbiente()).isEqualTo(ambiente);
            conferirPadroes(json.valueToTree(previa.getPayload()), USUARIO);
        }
    }
}
