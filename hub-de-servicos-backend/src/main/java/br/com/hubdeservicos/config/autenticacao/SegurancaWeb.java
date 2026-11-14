package br.com.hubdeservicos.config.autenticacao;

import br.com.hubdeservicos.service.autenticacao.TokenJwt;
import br.com.hubdeservicos.controller.app.AutenticacaoAppController;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class SegurancaWeb {

    private static final String AUTENTICACAO = "/api/v1/aplicacoes/*/autenticacao";

    @Bean
    SecurityFilterChain api(HttpSecurity http, TokenJwt jwt, ObjectMapper json) throws Exception {
        return http
                // Só as rotas de sessão do navegador leem cookie. Elas exigem um cabeçalho
                // não permitido em requisições simples de outra origem; não habilitar CORS nelas.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/aplicacoes/" + AutenticacaoAppService.SLUG + "/autenticacao/**").denyAll()
                        .requestMatchers(HttpMethod.POST, AutenticacaoAppController.ROTA + "/cadastro",
                                AutenticacaoAppController.ROTA + "/login", AutenticacaoAppController.ROTA + "/renovar").permitAll()
                        .requestMatchers(HttpMethod.GET, AutenticacaoAppController.ROTA + "/eu").authenticated()
                        .requestMatchers(HttpMethod.POST, AutenticacaoAppController.ROTA + "/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, AUTENTICACAO + "/login", AUTENTICACAO + "/renovar").permitAll()
                        .requestMatchers(HttpMethod.POST, AUTENTICACAO + "/navegador/login",
                                AUTENTICACAO + "/navegador/renovar", AUTENTICACAO + "/navegador/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, AUTENTICACAO + "/eu",
                                AUTENTICACAO + "/permissoes").authenticated()
                        .requestMatchers(HttpMethod.POST, AUTENTICACAO + "/logout").authenticated()
                        .requestMatchers("/api/v1/admin/**", "/api/v1/aplicacoes/*/admin/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((requisicao, resposta, erro) -> {
                            resposta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            responderErro(json, resposta, HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
                        })
                        .accessDeniedHandler((requisicao, resposta, erro) ->
                                responderErro(json, resposta, HttpStatus.FORBIDDEN, "Acesso negado.")))
                .addFilterBefore(new FiltroJwt(jwt), AnonymousAuthenticationFilter.class)
                .build();
    }

    private static void responderErro(ObjectMapper json, HttpServletResponse resposta, HttpStatus status,
                                      String detalhe) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(resposta.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
