package br.com.hubdeservicos.config.autenticacao;

import br.com.hubdeservicos.controller.autenticacao.AutenticacaoController;
import br.com.hubdeservicos.controller.autenticacao.ChavesPublicasController;
import br.com.hubdeservicos.controller.autenticacao.SessaoNavegadorController;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.TokenJwt;
import br.com.hubdeservicos.service.autorizacao.AutorizacaoService;
import jakarta.servlet.http.Cookie;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AutenticacaoController.class, ChavesPublicasController.class,
        SessaoNavegadorController.class}, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class,
        properties = "hub.autenticacao.cookie-seguro=true")
@Import({SegurancaWeb.class, TokenJwt.class, ConfiguracaoAutenticacao.Componentes.class})
@ActiveProfiles("local")
class AutenticacaoHttpTest {

    private static final String BASE = "/api/v1/aplicacoes/portal/autenticacao";
    private static final String NAVEGADOR = BASE + "/navegador";
    private static final String COOKIE = "plataforma_atualizacao";
    private static final String TOKEN = "a".repeat(43);
    private static final String ROTACIONADO = "b".repeat(43);
    private static final String CABECALHO = "X-Sessao-Navegador";

    @MockBean
    private AutenticacaoService autenticacao;

    @MockBean
    private AutorizacaoService autorizacao;

    @Autowired
    private MockMvc mvc;

    @Test
    void publicaJwksEExigeBearerNasRotasProtegidas() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
        mvc.perform(get(BASE + "/eu").header(HttpHeaders.AUTHORIZATION, "Bearer invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        mvc.perform(post(BASE + "/logout")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/permissoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void retornaErroGenericoParaEntradaInvalida() throws Exception {
        mvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"\",\"senha\":\"segredo-de-teste\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Dados de autenticação inválidos."));
    }

    @Test
    void autenticaNavegadorComCookieProtegidoSemExporRefreshNoJson() throws Exception {
        when(autenticacao.login(eq("portal"), any(), isNull(), anyString()))
                .thenReturn(new TokensAutenticacao("acesso", TOKEN, Instant.now().plusSeconds(300)));
        var entrada = "{\"login\":\"usuario\",\"senha\":\"segredo-de-teste\"}";

        mvc.perform(post(NAVEGADOR + "/login").header(CABECALHO, "1")
                        .contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(containsString(COOKIE + "=" + TOKEN),
                        containsString("Path=" + NAVEGADOR), containsString("HttpOnly"),
                        containsString("Secure"), containsString("SameSite=Strict"))))
                .andExpect(jsonPath("$.tokenAcesso").value("acesso"))
                .andExpect(jsonPath("$.tokenAtualizacao").doesNotExist());

        mvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON).content(entrada))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAtualizacao").value(TOKEN))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void rotacionaCookieNaRestauracaoERevogaSessaoNoLogout() throws Exception {
        when(autenticacao.renovar(eq("portal"), eq(TOKEN), isNull(), anyString()))
                .thenReturn(new TokensAutenticacao("novo-acesso", ROTACIONADO, Instant.now().plusSeconds(300)));

        mvc.perform(post(NAVEGADOR + "/renovar").header(CABECALHO, "1").cookie(new Cookie(COOKIE, TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcesso").value("novo-acesso"))
                .andExpect(jsonPath("$.tokenAtualizacao").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(COOKIE + "=" + ROTACIONADO)));

        mvc.perform(post(NAVEGADOR + "/logout").header(CABECALHO, "1").cookie(new Cookie(COOKIE, ROTACIONADO)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(containsString("Max-Age=0"),
                        containsString("Path=" + NAVEGADOR))));
        verify(autenticacao).logoutNavegador(eq("portal"), eq(ROTACIONADO), anyString());
    }

    @Test
    void eliminaCookieQuandoSessaoNaoPodeSerRestaurada() throws Exception {
        when(autenticacao.renovar(eq("portal"), eq(TOKEN), isNull(), anyString()))
                .thenThrow(new AutenticacaoNegadaException());
        mvc.perform(post(NAVEGADOR + "/renovar").header(CABECALHO, "1").cookie(new Cookie(COOKIE, TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void naoAceitaRequisicoesPorCookieSemCabecalhoNemAutorizaOutraOrigem() throws Exception {
        mvc.perform(post(NAVEGADOR + "/logout").cookie(new Cookie(COOKIE, TOKEN)))
                .andExpect(status().is4xxClientError());
        mvc.perform(options(NAVEGADOR + "/renovar")
                        .header(HttpHeaders.ORIGIN, "https://outra-origem.invalid")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, CABECALHO))
                .andExpect(status().is4xxClientError())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        verifyNoInteractions(autenticacao);
    }
}
