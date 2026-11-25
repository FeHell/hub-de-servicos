package br.com.hubdeservicos.controller.autenticacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.autenticacao.LoginEntradaBean;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacaoBean;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = SessaoNavegadorController.ROTA, headers = "X-Sessao-Navegador=1", produces = MediaType.APPLICATION_JSON_VALUE)
public class SessaoNavegadorController {

    public static final String ROTA = "/api/v1/aplicacoes/{slug}/autenticacao/navegador";
    private static final String COOKIE = "plataforma_atualizacao";
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final AutenticacaoService autenticacao;
    private final boolean cookieSeguro;

    public SessaoNavegadorController(AutenticacaoService autenticacao,
            @Value("${hub.autenticacao.cookie-seguro:true}") boolean cookieSeguro) {
        this.autenticacao = autenticacao;
        this.cookieSeguro = cookieSeguro;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AcessoNavegadorBean> login(@PathVariable String slug,
            @Valid @RequestBody LoginEntradaBean entrada, HttpServletRequest requisicao) {
        validarSlug(slug);
        return acesso(slug, autenticacao.login(slug, entrada, null, requisicao.getRemoteAddr()));
    }

    @PostMapping("/renovar")
    public ResponseEntity<AcessoNavegadorBean> renovar(@PathVariable String slug,
            @CookieValue(name = COOKIE, required = false) String token, HttpServletRequest requisicao) {
        validarSlug(slug);
        if (tokenValido(token)) {
            try {
                return acesso(slug, autenticacao.renovar(slug, token, null, requisicao.getRemoteAddr()));
            } catch (AutenticacaoNegadaException erro) {
                // Uma sessão expirada, revogada ou inválida não deve ser tentada novamente.
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookieExpirado(slug)).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@PathVariable String slug,
            @CookieValue(name = COOKIE, required = false) String token, HttpServletRequest requisicao) {
        validarSlug(slug);
        if (tokenValido(token)) {
            autenticacao.logoutNavegador(slug, token, requisicao.getRemoteAddr());
        }
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookieExpirado(slug)).build();
    }

    private ResponseEntity<AcessoNavegadorBean> acesso(String slug, TokensAutenticacaoBean tokens) {
        AcessoNavegadorBean acessoNavegadorPreparado = new AcessoNavegadorBean();
        acessoNavegadorPreparado.setTokenAcesso(tokens.getTokenAcesso());
        acessoNavegadorPreparado.setExpiracaoTokenAcesso(tokens.getExpiracaoTokenAcesso());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(slug, tokens.getTokenAtualizacao()).build().toString())
                .body(acessoNavegadorPreparado);
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String slug, String token) {
        return ResponseCookie.from(COOKIE, token).httpOnly(true).secure(cookieSeguro).sameSite("Strict")
                .path(ROTA.replace("{slug}", slug));
    }

    private String cookieExpirado(String slug) {
        return cookie(slug, "").maxAge(0).build().toString();
    }

    private static boolean tokenValido(String token) {
        return token != null && TOKEN.matcher(token).matches();
    }

    private static void validarSlug(String slug) {
        if (!SLUG.matcher(slug).matches()) {
            throw new AutenticacaoNegadaException();
        }
    }

    @Schema(name = "AcessoNavegador")
    public static final class AcessoNavegadorBean {
        private String tokenAcesso;

        private Instant expiracaoTokenAcesso;

        public AcessoNavegadorBean() {
        }

        public String getTokenAcesso() {
            return tokenAcesso;
        }

        public void setTokenAcesso(String tokenAcesso) {
            this.tokenAcesso = tokenAcesso;
        }

        public Instant getExpiracaoTokenAcesso() {
            return expiracaoTokenAcesso;
        }

        public void setExpiracaoTokenAcesso(Instant expiracaoTokenAcesso) {
            this.expiracaoTokenAcesso = expiracaoTokenAcesso;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AcessoNavegadorBean outro)) {
                return false;
            }
            return Objects.equals(tokenAcesso, outro.tokenAcesso)
                    && Objects.equals(expiracaoTokenAcesso, outro.expiracaoTokenAcesso);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(tokenAcesso);
            resultado = 31 * resultado + Objects.hashCode(expiracaoTokenAcesso);
            return resultado;
        }

        @Override
        public String toString() {
            return "AcessoNavegador";
        }
    }
}
