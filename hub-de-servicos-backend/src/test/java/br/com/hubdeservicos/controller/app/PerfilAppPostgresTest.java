package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;
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
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockBean SmsService sms;
    @MockBean Clock relogio;

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
        enviar(get(ROTA + "/" + outra.id()), conta, null).andExpect(status().isForbidden());
        enviar(get(ROTA), conta, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.pendencias.length()").value(2));
        enviar(put(ROTA), conta, Map.of("nome", "  Ana  ", "telefone", numero, "id", outra.id(),
                "status", "INATIVO", "situacaoTelefone", "VERIFICADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(conta.id()))
                .andExpect(jsonPath("$.nome").value("Ana")).andExpect(jsonPath("$.telefone").value("+55" + numero))
                .andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        enviar(put(ROTA), outra, Map.of("telefone", "+55 " + numero)).andExpect(status().isConflict());
        var smsEnviado = iniciar(conta, numero);
        enviar(post(SMS + "/validar"), outra, smsEnviado.resposta(smsEnviado.codigo())).andExpect(status().isConflict());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.incorreto())).andExpect(status().isBadRequest());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.codigo()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"))
                .andExpect(jsonPath("$.pendencias").isEmpty());
        enviar(put(ROTA), outra, Map.of("telefone", numero)).andExpect(status().isConflict());
        enviar(post(SMS + "/validar"), conta, smsEnviado.resposta(smsEnviado.codigo())).andExpect(status().isConflict());
        enviar(put(ROTA), conta, Map.of("telefone", "+55 " + numero))
                .andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"));
        enviar(put(ROTA), conta, Map.of("telefone", telefone()))
                .andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
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
        enviar(post(SMS + "/validar"), conta, primeiro.resposta(primeiro.codigo())).andExpect(status().isTooManyRequests());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(70));
        enviar(post(SMS + "/reenviar"), conta, null).andExpect(status().isOk());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(140));
        var terceiro = iniciar(conta, numero);
        enviar(put(ROTA), conta, Map.of("telefone", telefone())).andExpect(status().isOk());
        enviar(post(SMS + "/validar"), conta, terceiro.resposta(terceiro.codigo())).andExpect(status().isConflict());
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(210));
        var quarto = iniciar(conta, numero);
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(510));
        conta = renovar(conta);
        enviar(post(SMS + "/validar"), conta, quarto.resposta(quarto.codigo())).andExpect(status().isGone());
        iniciar(conta, numero);
        when(relogio.instant()).thenReturn(AGORA.plusSeconds(590));
        enviar(post(SMS + "/reenviar"), renovar(conta), null).andExpect(status().isTooManyRequests());
    }

    @Test
    void falhaDoProvedorNaoConfirmaNemExpõeCodigo() throws Exception {
        var conta = conta();
        when(sms.send(any())).thenReturn(SmsSendResult.falha("TESTE", SmsSendResult.Erro.FALHA_DE_CONEXAO));
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

    private int atualizarConcorrente(Conta conta, String numero, CyclicBarrier barreira) throws Exception {
        barreira.await(5, TimeUnit.SECONDS);
        return enviar(put(ROTA), conta, Map.of("telefone", numero)).andReturn().getResponse().getStatus();
    }

    private Envio iniciar(Conta conta, String numero) throws Exception {
        reset(sms);
        when(sms.send(any())).thenReturn(SmsSendResult.aceito("TESTE", null));
        JsonNode resposta = corpo(enviar(post(SMS), conta, Map.of("telefone", numero)).andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.cadastro.situacaoTelefone").value("PENDENTE")));
        var captura = ArgumentCaptor.forClass(SmsSendRequest.class);
        verify(sms).send(captura.capture());
        assertThat(captura.getValue().destinatario()).isEqualTo("+55" + numero);
        var codigo = Pattern.compile("[0-9]{6}").matcher(captura.getValue().mensagem());
        assertThat(codigo.find()).isTrue();
        assertThat(resposta.path("telefoneMascarado").asText()).doesNotContain(numero);
        return new Envio(resposta.path("idDesafio").asText(), codigo.group());
    }

    private Conta conta() throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        var dados = Map.of("email", email, "senha", "Senha-Perfil-App-21!");
        var criado = corpo(enviar(post(AUT + "/cadastro"), null, dados).andExpect(status().isCreated()));
        return login(criado.path("id").asText(), dados);
    }

    private Conta renovar(Conta conta) throws Exception {
        return login(conta.id(), conta.dados());
    }

    private Conta login(String id, Map<String, String> dados) throws Exception {
        var login = corpo(enviar(post(AUT + "/login"), null, dados).andExpect(status().isOk()));
        return new Conta(id, dados, login.path("tokenAcesso").asText());
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, Conta conta, Object dados) throws Exception {
        requisicao.with(req -> { req.setRemoteAddr("192.0.2." + ThreadLocalRandom.current().nextInt(1, 250)); return req; });
        if (conta != null) requisicao.header("Authorization", "Bearer " + conta.token());
        if (dados != null) requisicao.contentType("application/json").content(json.writeValueAsBytes(dados));
        return mvc.perform(requisicao);
    }

    private JsonNode corpo(ResultActions resposta) throws Exception {
        return json.readTree(resposta.andReturn().getResponse().getContentAsByteArray());
    }

    private static String telefone() { return "349" + ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000); }
    private record Conta(String id, Map<String, String> dados, String token) {}
    private record Envio(String id, String codigo) {
        Map<String, String> resposta(String valor) { return Map.of("idDesafio", id, "codigo", valor); }
        String incorreto() { return codigo.equals("000000") ? "000001" : "000000"; }
    }
}
