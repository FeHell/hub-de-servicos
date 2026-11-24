package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor.UnidadeMonetaria;
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
    private static final Endereco ENDERECO = new Endereco("01001-000", "Logradouro de teste", "1", "Bloco A",
            "Centro", "Cidade de teste", "SP", "Referência de teste");
    private final ValidacaoCredenciamento validador = new ValidacaoCredenciamento(RELOGIO, FACTORY.getValidator());
    private final MapeadorPagarme mapeador = new MapeadorPagarme(validador);
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @AfterAll
    static void fechar() { FACTORY.close(); }

    @Test
    void montaPfComValoresNumericosEContaEmTexto() {
        var dados = new Dados(Modalidade.INDIVIDUAL, pessoa(CPF, ENDERECO, "pessoa@example.invalid"), null, null,
                conta(CPF, TipoTitular.INDIVIDUAL));
        assertThat(validador.avaliar(dados).apto()).isTrue();
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
        var dados = new Dados(Modalidade.CORPORATION, null, empresa(new BigDecimal("40000.50")),
                representante(true, true), conta(CNPJ, TipoTitular.COMPANY));
        assertThat(validador.avaliar(dados).apto()).isTrue();
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
        var endereco = new Endereco("0123", "Teste", "1", "", "Bairro", "Cidade", "XX", null);
        var dados = new Dados(Modalidade.INDIVIDUAL, pessoa("00000000000", endereco, "invalido"), null, null,
                conta(CNPJ, TipoTitular.INDIVIDUAL));
        var resultado = validador.avaliar(dados);
        assertThat(resultado.apto()).isFalse();
        assertThat(resultado.invalidos()).extracting(Pendencia::campo).contains(
                "pessoaFisica.cpf", "pessoaFisica.email", "pessoaFisica.endereco.cep", "pessoaFisica.endereco.uf",
                "contaBancaria.documento");
        assertThat(resultado.faltantes()).extracting(Pendencia::campo)
                .contains("pessoaFisica.endereco.pontoReferencia", "pessoaFisica.endereco.complemento");
        assertThatThrownBy(() -> mapeador.preparar(dados, UnidadeMonetaria.REAIS, USUARIO, CODIGO))
                .isInstanceOf(ErrorResponseException.class)
                .hasMessageNotContaining("00000000000");
    }

    @Test
    void bloqueiaPeriodoLegadoDeclaracaoIncompletaEContaComOutroDocumento() {
        var dados = new Dados(Modalidade.CORPORATION, null, empresa(null),
                representante(false, null), conta(CPF, TipoTitular.INDIVIDUAL));
        var resultado = validador.avaliar(dados);
        assertThat(resultado.apto()).isFalse();
        assertThat(resultado.confirmacoes()).extracting(Pendencia::campo).containsExactlyInAnyOrder(
                "empresa.faturamentoAnual", "representante.representanteLegal", "representante.socioQsa", "contaBancaria.documento");
        assertThat(validador.avaliar(new Dados(Modalidade.CORPORATION, null, empresa(new BigDecimal("-1")),
                representante(true, true), conta(CNPJ, TipoTitular.COMPANY))).invalidos())
                .extracting(Pendencia::campo).contains("empresa.faturamentoAnual");
    }

    @Test
    void lePjLegadaSemReinterpretarValores() throws Exception {
        var dados = json.readValue("""
                {"empresa":{"razaoSocial":"Teste","faturamento":"100.00"},
                "representante":{"nome":"Teste","renda":"50.00"}}
                """, Dados.class);
        assertThat(dados.modalidade()).isEqualTo(Modalidade.CORPORATION);
        assertThat(dados.empresa().faturamento()).isEqualByComparingTo("100.00");
        assertThat(dados.empresa().faturamentoAnual()).isNull();
        assertThat(dados.representante().renda()).isEqualByComparingTo("50.00");
        assertThat(dados.representante().rendaMensal()).isNull();
        assertThat(dados.representante().socioQsa()).isNull();
    }

    private static PessoaFisica pessoa(String documento, Endereco endereco, String email) {
        return new PessoaFisica("Pessoa de teste", documento, email, "+55 34 99999-9999", TipoTelefone.CELULAR,
                "https://example.invalid", LocalDate.parse("1990-03-10"), "Teste", new BigDecimal("1234.56"), endereco);
    }

    @ParameterizedTest
    @CsvSource({"1985-11-16,16/11/1985", "1985-04-03,03/04/1985", "2000-12-01,01/12/2000"})
    void formataNascimentoSemAlterarDataInternaOuDemaisCampos(String entrada, String esperado) {
        var original = pessoa(CPF, ENDERECO, "pessoa@example.invalid");
        var nascimento = LocalDate.parse(entrada);
        var pessoa = new PessoaFisica(original.nome(), original.cpf(), original.email(), original.telefone(),
                original.tipoTelefone(), original.siteUrl(), nascimento, original.profissao(), original.rendaMensal(), original.endereco());
        var dados = new Dados(Modalidade.INDIVIDUAL, pessoa, null, null, conta(CPF, TipoTitular.INDIVIDUAL));
        var corpo = json.valueToTree(mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        assertThat(corpo.path("register_information").path("birthdate").asText()).isEqualTo(esperado);
        assertThat(dados.pessoaFisica().nascimento()).isEqualTo(nascimento);
        var referencia = json.valueToTree(mapeador.preparar(
                new Dados(Modalidade.INDIVIDUAL, original, null, null, dados.contaBancaria()),
                UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        ((com.fasterxml.jackson.databind.node.ObjectNode) referencia.path("register_information")).put("birthdate", esperado);
        assertThat(corpo).isEqualTo(referencia);

        var representante = representante(true, true);
        var comNascimento = new Representante(representante.nome(), representante.cpf(), representante.email(),
                representante.telefone(), representante.tipoTelefone(), nascimento, representante.profissao(), representante.renda(),
                representante.rendaMensal(), representante.representanteLegal(), representante.socioQsa(), representante.endereco());
        var pj = new Dados(Modalidade.CORPORATION, null, empresa(new BigDecimal("40000.50")), comNascimento,
                conta(CNPJ, TipoTitular.COMPANY));
        var corpoPj = json.valueToTree(mapeador.preparar(pj, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO));
        assertThat(corpoPj.path("register_information").path("managing_partners").path(0).path("birthdate").asText())
                .isEqualTo(esperado);
        assertThat(pj.representante().nascimento()).isEqualTo(nascimento);
    }

    private static Empresa empresa(BigDecimal anual) {
        return new Empresa("Razão social de teste", "Nome fantasia de teste", CNPJ, "empresa@example.invalid", "34999999999",
                TipoTelefone.CELULAR, new BigDecimal("30.00"), anual, "https://example.invalid",
                "LTDA", LocalDate.parse("2010-03-20"), ENDERECO);
    }

    private static Representante representante(Boolean legal, Boolean socio) {
        return new Representante("Representante de teste", CPF, "representante@example.invalid", "34999999999",
                TipoTelefone.CELULAR, LocalDate.parse("1990-03-10"), "Teste", null,
                new BigDecimal("1100.00"), legal, socio, ENDERECO);
    }

    private static ContaBancaria conta(String documento, TipoTitular tipo) {
        return new ContaBancaria("Nome de teste", documento, tipo, TipoConta.CORRENTE, "001", "0001", null, "000045", "x");
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
        var dados = new Dados(Modalidade.INDIVIDUAL, pessoa(CPF, ENDERECO, "pessoa@example.invalid"), null, null,
                conta(CPF, TipoTitular.INDIVIDUAL));
        var outroUsuario = UUID.randomUUID();
        conferirPadroes(json.valueToTree(mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, outroUsuario, CODIGO)), outroUsuario);
        assertThatThrownBy(() -> mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, null, CODIGO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void descartaNomeDaMaeLegadoSemInvalidarDadosNemReintroduzirNoPayload() throws Exception {
        for (var modalidade : Modalidade.values()) {
            var dados = modalidade == Modalidade.INDIVIDUAL
                    ? new Dados(modalidade, pessoa(CPF, ENDERECO, "pessoa@example.invalid"), null, null, conta(CPF, TipoTitular.INDIVIDUAL))
                    : new Dados(modalidade, null, empresa(new BigDecimal("40000.50")), representante(true, true), conta(CNPJ, TipoTitular.COMPANY));
            var arvore = json.valueToTree(dados);
            var pessoa = (com.fasterxml.jackson.databind.node.ObjectNode) arvore.path(
                    modalidade == Modalidade.INDIVIDUAL ? "pessoaFisica" : "representante");
            pessoa.put("nomeMae", "Nome legado ".repeat(100));
            pessoa.put("mother_name", "Campo de cliente antigo");
            var recuperado = json.treeToValue(arvore, Dados.class);
            assertThat(validador.avaliar(recuperado).apto()).isTrue();
            assertThat(json.writeValueAsString(recuperado)).doesNotContain("nomeMae", "mother_name");
            assertThat(new ComparacaoCredenciamento(json).iguais(dados, recuperado)).isTrue();
            assertThat(json.writeValueAsString(mapeador.preparar(recuperado, UnidadeMonetaria.CENTAVOS, USUARIO, CODIGO)))
                    .doesNotContain("mother_name", "nomeMae");
        }
    }

    @Test
    void previaAdministrativaUsaSolicitantePersistidoECodigoDoCredenciamentoNosDoisAmbientes() {
        var dados = new Dados(Modalidade.INDIVIDUAL, pessoa(CPF, ENDERECO, "pessoa@example.invalid"), null, null,
                conta(CPF, TipoTitular.INDIVIDUAL));
        var id = UUID.fromString(CODIGO);
        var registro = new br.com.hubdeservicos.entity.credenciamento.CredenciamentoEntity(id, USUARIO,
                br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento.STONE, "conteudo-protegido", RELOGIO.instant());
        var repositorio = mock(br.com.hubdeservicos.repository.credenciamento.CredenciamentoRepository.class);
        when(repositorio.findById(id)).thenReturn(java.util.Optional.of(registro));
        var autenticacao = mock(AutenticacaoPagarme.class);
        var conta = mock(AutenticacaoPagarme.Conta.class);
        when(autenticacao.selecionarConta()).thenReturn(conta);
        var servico = spy(new CredenciamentoService(repositorio, null, null, null, null, null, json, RELOGIO,
                validador, mapeador, autenticacao, null, null, null, null));
        doReturn(dados).when(servico).ler(registro);
        for (var ambiente : br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente.values()) {
            when(conta.ambiente()).thenReturn(ambiente);
            var previa = servico.preparar(id, UnidadeMonetaria.CENTAVOS);
            assertThat(previa.ambiente()).isEqualTo(ambiente);
            conferirPadroes(json.valueToTree(previa.payload()), USUARIO);
        }
    }
}
