package br.com.gerenciamentoplataforma.autenticacao.controller;

import br.com.gerenciamentoplataforma.autenticacao.dto.LoginEntrada;
import br.com.gerenciamentoplataforma.autenticacao.dto.TokensAutenticacao;
import br.com.gerenciamentoplataforma.autenticacao.exception.AutenticacaoNegadaException;
import br.com.gerenciamentoplataforma.autenticacao.service.AutenticacaoService;
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
@RequestMapping(path = SessaoNavegadorController.ROTA, headers = "X-Sessao-Navegador=1",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class SessaoNavegadorController {

    public static final String ROTA = "/api/v1/aplicacoes/{slug}/autenticacao/navegador";
    private static final String COOKIE = "plataforma_atualizacao";
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final AutenticacaoService autenticacao;
    private final boolean cookieSeguro;

    public SessaoNavegadorController(AutenticacaoService autenticacao,
            @Value("${plataforma.autenticacao.cookie-seguro:true}") boolean cookieSeguro) {
        this.autenticacao = autenticacao;
        this.cookieSeguro = cookieSeguro;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AcessoNavegador> login(@PathVariable String slug,
            @Valid @RequestBody LoginEntrada entrada, HttpServletRequest requisicao) {
        validarSlug(slug);
        return acesso(slug, autenticacao.login(slug, entrada, null, requisicao.getRemoteAddr()));
    }

    @PostMapping("/renovar")
    public ResponseEntity<AcessoNavegador> renovar(@PathVariable String slug,
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
        if (tokenValido(token)) autenticacao.logoutNavegador(slug, token, requisicao.getRemoteAddr());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookieExpirado(slug)).build();
    }

    private ResponseEntity<AcessoNavegador> acesso(String slug, TokensAutenticacao tokens) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(slug, tokens.tokenAtualizacao()).build().toString())
                .body(new AcessoNavegador(tokens.tokenAcesso(), tokens.expiracaoTokenAcesso()));
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
        if (!SLUG.matcher(slug).matches()) throw new AutenticacaoNegadaException();
    }

    public record AcessoNavegador(String tokenAcesso, Instant expiracaoTokenAcesso) {
        @Override
        public String toString() {
            return "AcessoNavegador";
        }
    }
}
