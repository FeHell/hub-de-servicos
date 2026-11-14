package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.controller.administracao.AdministracaoConfiguracaoController;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.config.autenticacao.SegurancaWeb;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
import br.com.hubdeservicos.service.autenticacao.TokenJwt;
import br.com.hubdeservicos.service.autorizacao.AutorizacaoService;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;
import br.com.hubdeservicos.provider.SmsProvider;
import br.com.hubdeservicos.service.sms.SmsProviderResolver;
import br.com.hubdeservicos.service.sms.SmsService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdministracaoConfiguracaoController.class,
        excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        properties = {"hub.administracao.aplicacao-id=00000000-0000-4000-8000-000000000001",
                "INFOBIP_API_KEY=credencial-ficticia-privada"})
@Import({SegurancaWeb.class, AcessoAdministrativo.class, AdministracaoConfiguracaoService.class,
        RegistroAdministrativo.class, SmsProviderResolver.class, SmsService.class,
        AdministracaoConfiguracaoHttpTest.Componentes.class})
class AdministracaoConfiguracaoHttpTest {

    private static final String BASE = "/api/v1/admin/configuracoes";
    private static final String ATIVO = BASE + "/sms/provedor-ativo";
    private static final UUID APLICACAO = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID VISITANTE = UUID.randomUUID();
    private static final String AUDIENCIA = "hub-de-servicos-admin";
    private static final Instant AGORA = Instant.parse("2026-11-13T20:00:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private SmsService sms;
    @MockBean private TokenJwt jwt;
    @MockBean private AutorizacaoService autorizacao;
    @MockBean private ConfiguracaoSistemaRepository configuracoes;
    @MockBean private AuditoriaRepository auditoria;
    @Autowired @Qualifier("infobipSmsProvider")
    private SmsProvider infobip;

    @BeforeEach
    void preparar() {
        when(jwt.validar("admin")).thenReturn(new IdentidadeJwt(ADMIN, APLICACAO, UUID.randomUUID(), AUDIENCIA));
        when(jwt.validar("visitante")).thenReturn(new IdentidadeJwt(VISITANTE, APLICACAO, UUID.randomUUID(), AUDIENCIA));
        when(autorizacao.temPermissao(any(Authentication.class), eq(AUDIENCIA), eq("PLATAFORMA_ADMINISTRAR")))
                .thenAnswer(chamada -> {
                    Authentication autenticacao = chamada.getArgument(0);
                    return ((IdentidadeJwt) autenticacao.getPrincipal()).idUsuario().equals(ADMIN);
                });
    }

    @Test
    void protegeLeituraEAlteracaoContraAnonimoEVisitante() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer visitante"))
                .andExpect(status().isForbidden());
        mvc.perform(put(ATIVO).header(HttpHeaders.AUTHORIZATION, "Bearer visitante")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"codigo":"INFOBIP"}
                        """)).andExpect(status().isForbidden());
        verifyNoInteractions(configuracoes, auditoria);
    }

    @Test
    void recusaProvedorNaoImplementado() throws Exception {
        mvc.perform(put(ATIVO).header(HttpHeaders.AUTHORIZATION, "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"codigo":"FUTURO"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Provedor de SMS indisponível."));
        verifyNoInteractions(configuracoes, auditoria);
    }

    @Test
    void listaSomenteConfiguracaoPermitidaEProvedoresRegistradosSemCredenciais() throws Exception {
        var configuracao = mock(ConfiguracaoSistemaEntity.class);
        when(configuracao.getChave()).thenReturn(SMS_PROVEDOR_ATIVO);
        when(configuracao.getValor()).thenReturn("INFOBIP");
        when(configuracoes.findByChave(SMS_PROVEDOR_ATIVO)).thenReturn(Optional.of(configuracao));

        mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chave").value(SMS_PROVEDOR_ATIVO))
                .andExpect(content().string(not(containsString("credencial-ficticia-privada"))));
        mvc.perform(get(BASE + "/sms/provedores").header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("INFOBIP"))
                .andExpect(jsonPath("$[0].nome").value("Infobip"))
                .andExpect(content().string(not(containsString("credencial-ficticia-privada"))));
        mvc.perform(get(ATIVO).header(HttpHeaders.AUTHORIZATION, "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value("INFOBIP"))
                .andExpect(content().string(not(containsString("credencial-ficticia-privada"))));
    }

    @Test
    void atualizaConfiguracaoAuditaETrocaDelegateNaMesmaInstancia() throws Exception {
        var valor = new AtomicReference<>("SIMULADO");
        var registro = mock(ConfiguracaoSistemaEntity.class);
        when(registro.getId()).thenReturn(UUID.randomUUID());
        when(registro.getStatus()).thenReturn(StatusCadastro.ATIVO);
        when(registro.getValor()).thenAnswer(chamada -> valor.get());
        doAnswer(chamada -> {
            valor.set(chamada.getArgument(0));
            return null;
        }).when(registro).atualizarValor(eq("INFOBIP"), eq(AGORA));
        when(configuracoes.findForUpdateByChave(SMS_PROVEDOR_ATIVO)).thenReturn(Optional.of(registro));
        when(configuracoes.buscarValor(SMS_PROVEDOR_ATIVO, StatusCadastro.ATIVO))
                .thenAnswer(chamada -> Optional.of(valor.get()));
        var mensagem = new SmsSendRequest("+12025550123", "teste");
        when(infobip.send(mensagem)).thenReturn(SmsSendResult.aceito("INFOBIP", null));

        assertThat(sms.send(mensagem).codigoProvedor()).isEqualTo("SIMULADO");
        mvc.perform(put(ATIVO).header(HttpHeaders.AUTHORIZATION, "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"codigo":"INFOBIP"}
                        """)).andExpect(status().isOk()).andExpect(jsonPath("$.valor").value("INFOBIP"));
        assertThat(sms.send(mensagem).codigoProvedor()).isEqualTo("INFOBIP");

        var evento = ArgumentCaptor.forClass(AuditoriaEntity.class);
        verify(auditoria).save(evento.capture());
        assertThat(evento.getValue().getIdUsuario()).isEqualTo(ADMIN);
        assertThat(evento.getValue().getIdAplicacao()).isEqualTo(APLICACAO);
        assertThat(evento.getValue().getValorAntigo().path("valor").asText()).isEqualTo("SIMULADO");
        assertThat(evento.getValue().getValorNovo().path("valor").asText()).isEqualTo("INFOBIP");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Componentes {

        @Bean
        SmsProvider provedorSimulado() {
            return new SmsProvider() {
                public String getProviderCode() { return "SIMULADO"; }
                public String getDisplayName() { return "Simulado"; }
                public SmsSendResult send(SmsSendRequest request) { return SmsSendResult.aceito("SIMULADO", null); }
            };
        }

        @Bean
        SmsProvider infobipSmsProvider() {
            var provedor = mock(SmsProvider.class);
            when(provedor.getProviderCode()).thenReturn("INFOBIP");
            when(provedor.getDisplayName()).thenReturn("Infobip");
            return provedor;
        }

        @Bean
        Clock clock() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }
    }
}
