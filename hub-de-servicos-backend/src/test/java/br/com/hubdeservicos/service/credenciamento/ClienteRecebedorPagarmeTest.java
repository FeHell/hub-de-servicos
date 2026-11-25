package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ErroRecebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.StatusPagarme;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity.Operacao;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService.StoneVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static br.com.hubdeservicos.service.credenciamento.ClienteRecebedorPagarme.RECEBEDORES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@ExtendWith(OutputCaptureExtension.class)
class ClienteRecebedorPagarmeTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
    private final ClienteRecebedorPagarme cliente;
    {
        var argumentoPreparado = builder.build();
        var objectMapperPreparado = new ObjectMapper();
        var argumentoPreparado2 = objectMapperPreparado;
        var clienteRecebedorPagarmePreparado = new ClienteRecebedorPagarme(argumentoPreparado, argumentoPreparado2);
        cliente = clienteRecebedorPagarmePreparado;
    }
    private final String codigo = UUID.randomUUID().toString();
    private final UUID tentativa = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    @Test
    void enviaPelaV5ComAutenticacaoDoAmbienteECodigo(CapturedOutput saida) {
        servidor.expect(requestTo(RECEBEDORES)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", basic("sk_test_exemplo_ação")))
                .andExpect(header("Accept", "application/json")).andExpect(header("Content-Type", "application/json"))
                .andExpect(headerDoesNotExist("Idempotency-Key")).andExpect(jsonPath("$.code").value(codigo))
                .andExpect(jsonPath("$.register_information.type").value("individual"))
                .andExpect(jsonPath("$.register_information.birthdate").value("03/02/1990"))
                .andExpect(jsonPath("$.register_information.monthly_income").value(123456))
                .andExpect(jsonPath("$.register_information.phone_numbers[0].type").value("mobile"))
                .andExpect(jsonPath("$.register_information.mother_name").doesNotExist())
                .andExpect(jsonPath("$.default_bank_account.bank").value("001"))
                .andExpect(jsonPath("$.default_bank_account.branch_check_digit").doesNotExist())
                .andExpect(jsonPath("$.transfer_settings.transfer_enabled").value(true))
                .andExpect(jsonPath("$.transfer_settings.transfer_interval").value("Daily"))
                .andExpect(jsonPath("$.transfer_settings.transfer_day").value(0))
                .andExpect(jsonPath("$.automatic_anticipation_settings.enabled").value(false))
                .andExpect(jsonPath("$.metadata.companyToken").value(usuario.toString()))
                .andExpect(jsonPath("$.metadata.company_token").doesNotExist()).andRespond(
                        withSuccess("{\"id\":\"re_teste\",\"status\":\"registration\"}", MediaType.APPLICATION_JSON));
        var resultado = cliente.criar(conta(Ambiente.DEV, "sk_test_exemplo_ação"), payload(), tentativa);
        assertThat(resultado.getId()).isEqualTo("re_teste");
        assertThat(resultado.getStatus()).isEqualTo(StatusPagarme.REGISTRATION);
        assertThat(saida.getOut())
                .contains(tentativa.toString(), RECEBEDORES, "ambiente=DEV", "\"register_information\"",
                        "\"transfer_settings\"", "\"companyToken\"", "\"transfer_interval\" : \"Daily\"")
                .doesNotContain("sk_test_exemplo_ação", basic("sk_test_exemplo_ação"), "Authorization");
        servidor.verify();
    }

    @Test
    void recusaDeValidacaoUsaApenasCamposConhecidosSemCopiarValoresDoProvedor() {
        servidor.expect(requestTo(RECEBEDORES))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON).body("""
                        {"errors":{
                          "register_information.phone_numbers[0].type":["dado sensível"],
                          "default_bank_account.holder_document":["não ecoar este valor"],
                          "chave_injetada":["não ecoar este valor"]
                        }}
                        """));
        var resultado = cliente.criar(conta(Ambiente.DEV, "sk_test_exemplo_local"), payload(), tentativa);
        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getErro()).isEqualTo(ErroRecebedor.DADOS_RECUSADOS);
        assertThat(resultado.getCampos()).containsExactly("pessoaFisica.tipoTelefone", "contaBancaria.documento");
        servidor.verify();
    }

    @Test
    void preservaCorpoOriginalDo412EHeadersDeRastreioSemLiberarNovaCriacao() {
        String corpo = """
                {"message":"Precondition failed", "errors":{"default_bank_account.bank":["detalhe"]},
                 "gateway_response":{"code":"NOT_ALLOWED","errors":["detalhe"]}}
                """;
        var rastreio = UUID.randomUUID().toString();
        servidor.expect(requestTo(RECEBEDORES))
                .andRespond(withStatus(HttpStatus.PRECONDITION_FAILED).contentType(MediaType.APPLICATION_JSON)
                        .body(corpo).header("X-Request-Id", rastreio).header("Set-Cookie", "sessao=privada"));

        var resultado = cliente.criar(conta(Ambiente.DEV, "sk_test_exemplo_local"), payload(), tentativa);

        assertThat(resultado.getErro()).isEqualTo(ErroRecebedor.RESPOSTA_INDETERMINADA);
        assertThat(resultado.getDiagnostico().getStatusHttp()).isEqualTo(412);
        assertThat(resultado.getDiagnostico().getCabecalhos()).hasSize(1).containsEntry("x-request-id", rastreio);
        assertThat(resultado.getDiagnostico().getCorpo()).isEqualTo(corpo.getBytes(StandardCharsets.UTF_8));
        assertThat(resultado.toString()).doesNotContain("not_allowed", "NOT_ALLOWED", "detalhe");
        servidor.verify();
    }

    @Test
    void reconhecePrefixoRpDaReferenciaDeRecebedoresV5() {
        servidor.expect(requestTo(RECEBEDORES)).andRespond(
                withSuccess("{\"id\":\"rp_documentado\",\"status\":\"registration\"}", MediaType.APPLICATION_JSON));
        var resultado = cliente.criar(conta(Ambiente.DEV, "sk_test_exemplo_local"), payload(), tentativa);
        assertThat(resultado.getId()).isEqualTo("rp_documentado");
        assertThat(resultado.getStatus()).isEqualTo(StatusPagarme.REGISTRATION);
        servidor.verify();
    }

    @Test
    void credencialRecusadaNoEndpointComumNaoConsultaOutroAmbiente() {
        servidor.expect(requestTo(RECEBEDORES)).andExpect(header("Authorization", basic("sk_exemplo_prod")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThat(cliente.criar(conta(Ambiente.PROD, "sk_exemplo_prod"), payload(), tentativa).getErro())
                .isEqualTo(ErroRecebedor.AUTENTICACAO);
        servidor.verify();
    }

    @Test
    void timeoutNaoRepetePostEConsultaRecebedorPeloCodigo() {
        servidor.expect(requestTo(RECEBEDORES)).andExpect(method(HttpMethod.POST)).andRespond(requisicao -> {
            throw new SocketTimeoutException();
        });
        servidor.expect(requestTo(RECEBEDORES + "?page=1&size=50")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"data":[{"id":"re_outro","code":"outro"},
                                 {"id":"re_confirmado","status":"affiliation","code":"%s"}]}
                        """.formatted(codigo), MediaType.APPLICATION_JSON));
        var conta = conta(Ambiente.DEV, "sk_test_exemplo_local");
        assertThat(cliente.criar(conta, payload(), tentativa).getErro())
                .isEqualTo(ErroRecebedor.RESPOSTA_INDETERMINADA);
        assertThat(cliente.consultar(conta, codigo, null).getId()).isEqualTo("re_confirmado");
        servidor.verify();
    }

    @Test
    void respostaSemIdentificadorNaoConfirmaCriacao() {
        servidor.expect(requestTo(RECEBEDORES))
                .andRespond(withSuccess("{\"status\":\"registration\"}", MediaType.APPLICATION_JSON));
        var resultado = cliente.criar(conta(Ambiente.DEV, "sk_test_exemplo_local"), payload(), tentativa);
        assertThat(resultado.getErro()).isEqualTo(ErroRecebedor.RESPOSTA_INDETERMINADA);
        assertThat(resultado.getDiagnostico().getStatusHttp()).isEqualTo(200);
        servidor.verify();
    }

    @Test
    void alteraContaDoMesmoRecebedorViaPatchEConfereDadosNaConsulta() {
        String rota = RECEBEDORES + "/re_teste";
        String resposta = """
                {"id":"re_teste","status":"registration","document":"00000000000",
                 "default_bank_account":{"holder_name":"Nome de teste","holder_type":"individual","holder_document":"00000000000",
                 "bank":"001","branch_number":"0001","account_number":"000123","account_check_digit":"0","type":"checking"}}
                """;
        servidor.expect(requestTo(rota)).andExpect(method(HttpMethod.GET)).andRespond(withSuccess(
                resposta.replace("000123", "000099").replace("registration", "active"), MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(rota + "/default-bank-account")).andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Authorization", basic("sk_test_exemplo_local")))
                .andExpect(headerDoesNotExist("Idempotency-Key"))
                .andExpect(jsonPath("$.bank_account.account_number").value("000123"))
                .andExpect(jsonPath("$.bank_account.bank").value("001")).andRespond(withSuccess());
        servidor.expect(requestTo(rota)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(resposta, MediaType.APPLICATION_JSON));
        var resultado = cliente.alterar(conta(Ambiente.DEV, "sk_test_exemplo_local"), "re_teste", payload(),
                Operacao.ATUALIZACAO_BANCARIA, tentativa);
        assertThat(resultado.getId()).isEqualTo("re_teste");
        assertThat(resultado.getStatus()).isEqualTo(StatusPagarme.REGISTRATION);
        servidor.verify();
    }

    @Test
    void operacaoCadastralLegadaNaoEnviaMaisCampoRemovido() {
        assertThat(cliente.alterar(conta(Ambiente.DEV, "sk_test_exemplo_local"), "re_teste", payload(),
                Operacao.ATUALIZACAO_CADASTRAL, tentativa).getErro()).isEqualTo(ErroRecebedor.ATUALIZACAO_RESTRITA);
        assertThat(cliente.consultarAtualizacao(conta(Ambiente.DEV, "sk_test_exemplo_local"), "re_teste", payload(),
                Operacao.ATUALIZACAO_CADASTRAL).getErro()).isEqualTo(ErroRecebedor.RESPOSTA_INDETERMINADA);
        servidor.verify();
    }

    @Test
    void estadoRestritoImpedeQualquerMutacaoNoProvedor() {
        servidor.expect(requestTo(RECEBEDORES + "/re_teste")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"re_teste\",\"status\":\"blocked\"}", MediaType.APPLICATION_JSON));
        assertThat(cliente.alterar(conta(Ambiente.DEV, "sk_test_exemplo_local"), "re_teste", payload(),
                Operacao.ATUALIZACAO_BANCARIA, tentativa).getErro()).isEqualTo(ErroRecebedor.ATUALIZACAO_RESTRITA);
        servidor.verify();
    }

    @Test
    void reenvioNaoAlteraRecebedorQuandoAPreConsultaMostraAnaliseEmAndamento() {
        servidor.expect(requestTo(RECEBEDORES + "/re_teste")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"re_teste","status":"affiliation","document":"00000000000"}
                        """, MediaType.APPLICATION_JSON));
        assertThat(cliente.alterar(conta(Ambiente.DEV, "sk_test_exemplo_local"), "re_teste", payload(),
                Operacao.ATUALIZACAO_BANCARIA, tentativa).getErro()).isEqualTo(ErroRecebedor.ATUALIZACAO_RESTRITA);
        servidor.verify();
    }

    private PagarmeRecebedorBean payload() {
        PagarmeRecebedorBean pagarmeRecebedorPreparado = new PagarmeRecebedorBean();
        pagarmeRecebedorPreparado.setCode(codigo);
        PagarmeRecebedorBean.IndividualBean individualPreparado = new PagarmeRecebedorBean.IndividualBean();
        individualPreparado.setType("individual");
        individualPreparado.setName("Nome de teste");
        individualPreparado.setEmail("teste@example.invalid");
        individualPreparado.setDocument("00000000000");
        individualPreparado.setSiteUrl("https://example.invalid");
        individualPreparado.setBirthdate("03/02/1990");
        var bigDecimalPreparado = new BigDecimal("123456");
        individualPreparado.setMonthlyIncome(bigDecimalPreparado);
        individualPreparado.setProfessionalOccupation("Profissão de teste");
        individualPreparado.setAddress(null);
        PagarmeRecebedorBean.TelefoneBean telefonePreparado = new PagarmeRecebedorBean.TelefoneBean();
        telefonePreparado.setDdd("34");
        telefonePreparado.setNumber("999999999");
        telefonePreparado.setType("mobile");
        individualPreparado.setPhoneNumbers(List.of(telefonePreparado));
        pagarmeRecebedorPreparado.setRegisterInformation(individualPreparado);
        PagarmeRecebedorBean.BancoBean bancoPreparado = new PagarmeRecebedorBean.BancoBean();
        bancoPreparado.setHolderName("Nome de teste");
        bancoPreparado.setHolderType("individual");
        bancoPreparado.setHolderDocument("00000000000");
        bancoPreparado.setBank("001");
        bancoPreparado.setBranchNumber("0001");
        bancoPreparado.setBranchCheckDigit(null);
        bancoPreparado.setAccountNumber("000123");
        bancoPreparado.setAccountCheckDigit("0");
        bancoPreparado.setType("checking");
        pagarmeRecebedorPreparado.setDefaultBankAccount(bancoPreparado);
        MapeadorPagarme.prepararPadroes(pagarmeRecebedorPreparado, usuario);
        return pagarmeRecebedorPreparado;
    }

    private static AutenticacaoPagarme.ContaVO conta(Ambiente ambiente, String segredo) {
        var configuracoes = mock(ConfiguracoesExternasService.class);
        var stonePreparado = new StoneVO(ambiente, "conta-teste", "publica-teste", "recebedor-mestre", segredo);
        when(configuracoes.lerStone()).thenReturn(stonePreparado);
        var autenticacaoPagarmePreparado = new AutenticacaoPagarme(configuracoes);
        return autenticacaoPagarmePreparado.selecionarConta();
    }

    private static String basic(String segredo) {
        return "Basic " + Base64.getEncoder().encodeToString((segredo + ":").getBytes(StandardCharsets.UTF_8));
    }
}
