package br.com.gerenciamentoplataforma.autenticacao.controller;

import br.com.gerenciamentoplataforma.autenticacao.dto.LoginEntrada;
import br.com.gerenciamentoplataforma.autenticacao.dto.RenovacaoEntrada;
import br.com.gerenciamentoplataforma.autenticacao.dto.TokensAutenticacao;
import br.com.gerenciamentoplataforma.autenticacao.dto.UsuarioAutenticado;
import br.com.gerenciamentoplataforma.autenticacao.service.AutenticacaoService;
import br.com.gerenciamentoplataforma.autenticacao.service.IdentidadeJwt;
import br.com.gerenciamentoplataforma.autorizacao.service.AutorizacaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/aplicacoes/{slug}/autenticacao", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AutenticacaoController {

    private final AutenticacaoService autenticacao;
    private final AutorizacaoService autorizacao;

    public AutenticacaoController(AutenticacaoService autenticacao, AutorizacaoService autorizacao) {
        this.autenticacao = autenticacao;
        this.autorizacao = autorizacao;
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Autenticar o usuário na aplicação")
    public ResponseEntity<TokensAutenticacao> login(
            @PathVariable String slug,
            @Valid @RequestBody LoginEntrada entrada,
            @RequestHeader(name = "X-Client-Secret", required = false) String segredoCliente,
            HttpServletRequest requisicao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autenticacao.login(slug, entrada, segredoCliente, requisicao.getRemoteAddr()));
    }

    @PostMapping(path = "/renovar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Renovar a sessão e rotacionar o token de atualização")
    public ResponseEntity<TokensAutenticacao> renovar(
            @PathVariable String slug,
            @Valid @RequestBody RenovacaoEntrada entrada,
            @RequestHeader(name = "X-Client-Secret", required = false) String segredoCliente,
            HttpServletRequest requisicao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autenticacao.renovar(slug, entrada.tokenAtualizacao(), segredoCliente, requisicao.getRemoteAddr()));
    }

    @GetMapping("/eu")
    @Operation(summary = "Consultar o usuário autenticado na aplicação")
    @SecurityRequirement(name = "bearer")
    public ResponseEntity<UsuarioAutenticado> eu(@PathVariable String slug,
                                                  @AuthenticationPrincipal IdentidadeJwt identidade) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autenticacao.consultarUsuario(slug, identidade));
    }

    @GetMapping("/permissoes")
    @Operation(summary = "Listar os códigos de permissão efetivos na aplicação da sessão")
    @SecurityRequirement(name = "bearer")
    public ResponseEntity<List<String>> permissoes(@PathVariable String slug,
                                                   @AuthenticationPrincipal IdentidadeJwt identidade) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(autorizacao.listarPermissoes(slug, identidade));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revogar a sessão e a família de tokens")
    @SecurityRequirement(name = "bearer")
    public ResponseEntity<Void> logout(@PathVariable String slug, @AuthenticationPrincipal IdentidadeJwt identidade,
                                       HttpServletRequest requisicao) {
        autenticacao.logout(slug, identidade, requisicao.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
