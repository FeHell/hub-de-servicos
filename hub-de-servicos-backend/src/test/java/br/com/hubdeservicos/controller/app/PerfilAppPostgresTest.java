package br.com.hubdeservicos.controller.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean;
import br.com.hubdeservicos.persistencia.ConexaoPostgresTeste;
import br.com.hubdeservicos.service.sms.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.liquibase.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "TESTE_APP_POSTGRES", matches = "true")
class PerfilAppPostgresTest {
    private static final String ROTA = "/api/v1/app/perfil";
    private static final String SMS = ROTA + "/telefone/confirmacao";
    private static final String AUT = "/api/v1/app/autenticacao";
    private static final Instant AGORA = Instant.now();
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @MockBean
    SmsService sms;
    @MockBean
    Clock relogio;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) {
        ConexaoPostgresTeste.configurar(propriedades);
    }

    @BeforeEach
    void preparar() {
        when(relogio.instant()).thenReturn(AGORA);
    }

    @Test
    void cadastroProprioCodigoValidoETrocaDeNumero() throws Exception {
        var conta = conta();
        var outra = conta();
        String numero = telefone();
        mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
        enviar(get(ROTA + "/" + outra.getId()), conta, null).andExpect(status().isForbidden());
        enviar(get(ROTA), conta, null).andExpect(status().isOk()).andExpect(jsonPath("$.pendencias.length()").value(2));
        enviar(put(ROTA), conta,
                Map.of("nome", "  Ana  ", "telefone", numero, "id", outra.getId(), "status", "INATIVO",
                        "situacaoTelefone", "VERIFICADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(conta.getId()))
                .andExpect(jsonPath("$.nome").value("Ana")).andExpect(jsonPath("$.telefone").value("+55" + numero))
                .andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        enviar(put(ROTA), outra, Map.of("telefone", "+55 " + numero)).andExpect(status().isConflict());
        var smsEnviado = iniciar(conta, numero);
        enviar(post(SMS + "/validar"), outra, smsEnviado.resposta(smsEnviado.getCodigo()))
                .andExpect(status().isConflict());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.incorreto()))
                .andExpect(status().isBadRequest());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.getCodigo())).andExpect(status().isOk())
                .andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"))
                .andExpect(jsonPath("$.pendencias").isEmpty());
        enviar(put(ROTA), outra, Map.of("telefone", numero)).andExpect(status().isConflict());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.getCodigo()))
                .andExpect(status().isConflict());
        enviar(put(ROTA), conta, Map.of("telefone", "+55 " + numero))
                .andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"));
        String novoNumero = telefone();
        enviar(put(ROTA), conta, Map.of("telefone", novoNumero))
                .andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        enviar(get(ROTA), renovar(conta), null).andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(70));
        var novoEnvio = iniciar(conta, novoNumero);
        enviar(post(SMS + "/validar"), conta, novoEnvio.resposta(novoEnvio.getCodigo()))
                .andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"));
        enviar(put(ROTA), conta, Map.of("telefone", ""))
                .andExpect(jsonPath("$.situacaoTelefone").value("NAO_INFORMADO"));
        enviar(get(ROTA), outra, null).andExpect(jsonPath("$.nome").doesNotExist());
    }

    @Test
    void expiraLimitaTentativasEReenvioEInvalidaDesafioAnterior() throws Exception {
        var conta = conta();
        String numero = telefone();
        var primeiro = iniciar(conta, numero);
        enviar(post(SMS + "/reenviar"), conta, null).andExpect(status().isTooManyRequests());
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            enviar(post(SMS + "/validar"), conta, primeiro.resposta(primeiro.incorreto()))
                    .andExpect(tentativa == 5 ? status().isTooManyRequests() : status().isBadRequest());
        }
        enviar(post(SMS + "/validar"), conta, primeiro.resposta(primeiro.getCodigo()))
                .andExpect(status().isTooManyRequests());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(70));
        enviar(post(SMS + "/reenviar"), conta, null).andExpect(status().isOk());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(140));
        var terceiro = iniciar(conta, numero);
        enviar(put(ROTA), conta, Map.of("telefone", telefone())).andExpect(status().isOk());
        enviar(post(SMS + "/validar"), conta, terceiro.resposta(terceiro.getCodigo())).andExpect(status().isConflict());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(210));
        var quarto = iniciar(conta, numero);
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(510));
        conta = renovar(conta);
        enviar(post(SMS + "/validar"), conta, quarto.resposta(quarto.getCodigo())).andExpect(status().isGone());
        iniciar(conta, numero);
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(590));
        enviar(post(SMS + "/reenviar"), renovar(conta), null).andExpect(status().isTooManyRequests());
    }

    @Test
    void falhaDoProvedorNaoConfirmaNemExpõeCodigo() throws Exception {
        var conta = conta();
        when(sms.send(any())).thenReturn(SmsSendResultBean.falha("TESTE", SmsSendResultBean.Erro.FALHA_DE_CONEXAO));
        enviar(post(SMS), conta, Map.of("telefone", telefone())).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").doesNotExist());
        enviar(get(ROTA), conta, null).andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        enviar(post(SMS + "/reenviar"), conta, null).andExpect(status().isTooManyRequests());
    }

    @Test
    void concorrenciaAceitaUmUnicoTitular() throws Exception {
        var conta = conta();
        var outra = conta();
        String numero = telefone();
        var barreira = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primeiro = executor.submit(() -> atualizarConcorrente(conta, numero, barreira));
            var segundo = executor.submit(() -> atualizarConcorrente(outra, "+55 " + numero, barreira));
            assertThat(java.util.List.of(primeiro.get(15, TimeUnit.SECONDS), segundo.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from usuario where telefone = ?", Long.class, "+55" + numero))
                .isEqualTo(1L);
    }

    @Test
    void papeisIndependentesERecuperadosNaTabelaDePermissoes() throws Exception {
        var conta = conta();
        var outra = conta();
        String rota = ROTA + "/papeis/";
        enviar(put(rota + "EMPRESA"), null, Map.of("selecionado", true)).andExpect(status().isUnauthorized());
        enviar(put(rota + "EMPRESA"), conta, Map.of()).andExpect(status().isBadRequest());
        enviar(put(rota + "EMPRESA"), conta, Map.of("selecionado", true, "id", outra.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.papeis[0]").value("EMPRESA"));
        enviar(put(rota + "TRABALHADOR"), conta, Map.of("selecionado", true)).andExpect(status().isOk());
        enviar(get(ROTA), renovar(conta), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis.length()").value(2));
        enviar(get(ROTA), outra, null).andExpect(jsonPath("$.papeis").isEmpty());
        assertThat(jdbc.queryForObject("select count(*) from usuario_permissao_plataforma where id_usuario = ?",
                Long.class, java.util.UUID.fromString(conta.getId()))).isEqualTo(2);
        enviar(put(rota + "PLATAFORMA_ADMINISTRAR"), conta, Map.of("selecionado", true))
                .andExpect(status().isBadRequest());
        enviar(put(rota + "EMPRESA"), conta, Map.of("selecionado", false)).andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis[0]").value("TRABALHADOR"));
        enviar(put(rota + "TRABALHADOR"), conta, Map.of("selecionado", false)).andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis").isEmpty());
    }

    @Test
    void remuneracaoOpcionalExataEPreservadaAoDesmarcarPapel() throws Exception {
        var conta = conta();
        enviar(put(ROTA), conta, Map.of("remuneracao", Map.of("valorHora", 25))).andExpect(status().isConflict());
        String papel = ROTA + "/papeis/TRABALHADOR";
        enviar(put(papel), conta, Map.of("selecionado", true)).andExpect(status().isOk());
        enviar(put(ROTA), conta, Map.of("remuneracao", Map.of("valorHora", "25.00"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.remuneracao.valorHora").value("25.00"))
                .andExpect(jsonPath("$.remuneracao.valorDiaria").doesNotExist());
        enviar(put(ROTA), conta, Map.of("remuneracao", Map.of("valorDiaria", "180.72"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.remuneracao.valorHora").doesNotExist())
                .andExpect(jsonPath("$.remuneracao.valorDiaria").value("180.72"));
        enviar(put(ROTA), conta, Map.of("remuneracao", Map.of("valorHora", "0.00", "valorDiaria", "9999999999.99")))
                .andExpect(status().isOk());
        for (String invalido : java.util.List.of("-0.01", "25.001", "10000000000.00", "abc")) {
            enviar(put(ROTA), conta, Map.of("remuneracao", Map.of("valorHora", invalido)))
                    .andExpect(status().isBadRequest());
        }
        enviar(put(papel), conta, Map.of("selecionado", false)).andExpect(status().isOk());
        enviar(put(ROTA), conta, Map.of("nome", "Nome atualizado")).andExpect(status().isOk());
        enviar(get(ROTA), renovar(conta), null).andExpect(jsonPath("$.remuneracao.valorHora").value("0.00"))
                .andExpect(jsonPath("$.remuneracao.valorDiaria").value("9999999999.99"));
        enviar(put(papel), conta, Map.of("selecionado", true)).andExpect(status().isOk());
        enviar(put(ROTA), conta, Map.of("remuneracao", Map.of())).andExpect(status().isOk())
                .andExpect(jsonPath("$.remuneracao.valorHora").doesNotExist())
                .andExpect(jsonPath("$.remuneracao.valorDiaria").doesNotExist());
    }

    @Test
    void salvaSomenteMunicipioCanonicoEAtualizaQuandoMuda() throws Exception {
        var conta = conta();
        var outra = conta();
        String rota = ROTA + "/localizacao";
        var uberaba = Map.of("cidade", " Uberaba ", "estado", "minas gerais", "id", outra.getId());
        enviar(put(rota), null, uberaba).andExpect(status().isUnauthorized());
        enviar(put(rota), conta, uberaba).andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.codigoIbge").value(3170107))
                .andExpect(jsonPath("$.municipio.uf").value("MG"));
        enviar(put(rota), conta, Map.of("cidade", "Sao Paulo", "estado", "SP")).andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.nome").value("São Paulo"));
        enviar(put(rota), conta, Map.of("cidade", "Cidade inexistente", "estado", "SP"))
                .andExpect(status().isUnprocessableEntity());
        enviar(get(ROTA), renovar(conta), null).andExpect(jsonPath("$.municipio.codigoIbge").value(3550308));
        enviar(get(ROTA), outra, null).andExpect(jsonPath("$.municipio").doesNotExist());
    }

    @Test
    void pesquisaMunicipiosEProtegeEscolhaManualDeLeiturasAutomaticas() throws Exception {
        var conta = conta();
        String rota = ROTA + "/localizacao";
        var busca = get(ROTA + "/municipios").param("nome", "  uberlândia ");
        enviar(busca, null, null).andExpect(status().isUnauthorized());
        enviar(get(ROTA + "/municipios").param("nome", "  uberlandia  "), conta, null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoIbge").value(3170206));
        enviar(get(ROTA + "/municipios").param("nome", "bom jesus"), conta, null)
                .andExpect(jsonPath("$[?(@.nome == 'Bom Jesus')].uf").isArray()).andExpect(
                        jsonPath("$[?(@.nome == 'Bom Jesus')].uf").value(org.hamcrest.Matchers.hasItems("PB", "PI")));
        enviar(get(ROTA + "/municipios").param("nome", "%%"), conta, null).andExpect(jsonPath("$").isEmpty());
        enviar(get(ROTA + "/municipios").param("nome", "sa"), conta, null).andExpect(jsonPath("$.length()").value(25));

        enviar(put(rota + "/manual"), conta, Map.of("codigoIbge", 3170107)).andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.manual").value(true))
                .andExpect(jsonPath("$.municipio.codigoIbge").value(3170107));
        enviar(put(rota + "/manual"), conta, Map.of("codigoIbge", 9999999)).andExpect(status().isUnprocessableEntity());
        enviar(put(rota), conta, Map.of("cidade", "Inexistente", "estado", "MG"))
                .andExpect(status().isUnprocessableEntity());
        enviar(put(rota), conta, Map.of("cidade", "São Paulo", "estado", "SP")).andExpect(status().isOk())
                .andExpect(jsonPath("$.municipio.codigoIbge").value(3170107));
        enviar(get(ROTA), renovar(conta), null).andExpect(jsonPath("$.municipio.codigoIbge").value(3170107));
        enviar(put(rota), conta, Map.of("cidade", "São Paulo", "estado", "SP", "confirmada", true))
                .andExpect(status().isOk()).andExpect(jsonPath("$.municipio.codigoIbge").value(3550308))
                .andExpect(jsonPath("$.municipio.manual").value(false));
    }

    private int atualizarConcorrente(ContaBean conta, String numero, CyclicBarrier barreira) throws Exception {
        barreira.await(5, TimeUnit.SECONDS);
        return enviar(put(ROTA), conta, Map.of("telefone", numero)).andReturn().getResponse().getStatus();
    }

    private EnvioBean iniciar(ContaBean conta, String numero) throws Exception {
        reset(sms);
        when(sms.send(any())).thenReturn(SmsSendResultBean.aceito("TESTE", null));
        JsonNode resposta = corpo(enviar(post(SMS), conta, Map.of("telefone", numero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.cadastro.situacaoTelefone").value("PENDENTE")));
        var captura = ArgumentCaptor.forClass(SmsSendRequestBean.class);
        verify(sms).send(captura.capture());
        assertThat(captura.getValue().getDestinatario()).isEqualTo("+55" + numero);
        var codigo = Pattern.compile("[0-9]{6}").matcher(captura.getValue().getMensagem());
        assertThat(codigo.find()).isTrue();
        assertThat(resposta.path("telefoneMascarado").asText()).doesNotContain(numero);
        EnvioBean envioPreparado = new EnvioBean();
        envioPreparado.setId(resposta.path("idDesafio").asText());
        envioPreparado.setCodigo(codigo.group());
        return envioPreparado;
    }

    private ContaBean conta() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        var dados = Map.of("email", email, "senha", "Senha-Perfil-App-21!");
        var criado = corpo(enviar(post(AUT + "/cadastro"), null, dados).andExpect(status().isCreated()));
        return login(criado.path("id").asText(), dados);
    }

    private ContaBean renovar(ContaBean conta) throws Exception {
        return login(conta.getId(), conta.getDados());
    }

    private ContaBean login(String id, Map<String, String> dados) throws Exception {
        var login = corpo(enviar(post(AUT + "/login"), null, dados).andExpect(status().isOk()));
        ContaBean contaPreparado = new ContaBean();
        contaPreparado.setId(id);
        contaPreparado.setDados(dados);
        contaPreparado.setToken(login.path("tokenAcesso").asText());
        return contaPreparado;
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, ContaBean conta, Object dados)
            throws Exception {
        requisicao.with(req -> {
            req.setRemoteAddr("192.0.2." + ThreadLocalRandom.current().nextInt(1, 250));
            return req;
        });
        if (conta != null) {
            requisicao.header("Authorization", "Bearer " + conta.getToken());
        }
        if (dados != null) {
            requisicao.contentType("application/json").content(json.writeValueAsBytes(dados));
        }
        return mvc.perform(requisicao);
    }

    private JsonNode corpo(ResultActions resposta) throws Exception {
        return json.readTree(resposta.andReturn().getResponse().getContentAsByteArray());
    }

    private static String telefone() {
        return "349" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
    }

    @Schema(name = "Conta")
    private static final class ContaBean {
        private String id;

        private Map<String, String> dados;

        private String token;

        public ContaBean() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Map<String, String> getDados() {
            return dados;
        }

        public void setDados(Map<String, String> dados) {
            this.dados = dados;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ContaBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(dados, outro.dados)
                    && Objects.equals(token, outro.token);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(dados);
            resultado = 31 * resultado + Objects.hashCode(token);
            return resultado;
        }

        @Override
        public String toString() {
            return "Conta[" + "id=" + id + ", dados=" + dados + ", token=" + token + "]";
        }
    }

    @Schema(name = "Envio")
    private static final class EnvioBean {
        private String id;

        private String codigo;

        public EnvioBean() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EnvioBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(codigo, outro.codigo);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            return resultado;
        }

        @Override
        public String toString() {
            return "Envio[" + "id=" + id + ", codigo=" + codigo + "]";
        }

        Map<String, String> resposta(String valor) {
            return Map.of("idDesafio", id, "codigo", valor);
        }

        String incorreto() {
            return codigo.equals("000000") ? "000001" : "000000";
        }
    }
}
