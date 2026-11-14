package br.com.hubdeservicos.config.autenticacao;

import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.service.autenticacao.TokenJwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

class FiltroJwt extends OncePerRequestFilter {

    private final TokenJwt jwt;

    FiltroJwt(TokenJwt jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        var cabecalhos = requisicao.getHeaders(HttpHeaders.AUTHORIZATION);
        if (cabecalhos.hasMoreElements()) {
            String cabecalho = cabecalhos.nextElement();
            if (!cabecalhos.hasMoreElements() && cabecalho.regionMatches(true, 0, "Bearer ", 0, 7)) {
                autenticar(cabecalho.substring(7));
            }
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(String token) {
        try {
            var identidade = jwt.validar(token);
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(identidade, null, List.of()));
            SecurityContextHolder.setContext(contexto);
        } catch (AutenticacaoNegadaException erro) {
            SecurityContextHolder.clearContext();
        }
    }
}
