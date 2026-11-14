package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.app.ContratosApp.AcessoApp;
import br.com.hubdeservicos.dto.app.ContratosApp.CadastroEntrada;
import br.com.hubdeservicos.dto.app.ContratosApp.ContaApp;
import br.com.hubdeservicos.dto.app.ContratosApp.LoginAppEntrada;
import br.com.hubdeservicos.dto.autenticacao.RenovacaoEntrada;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.app.LimiteTentativasApp;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = AutenticacaoAppController.ROTA, produces = MediaType.APPLICATION_JSON_VALUE)
public class AutenticacaoAppController {
    public static final String ROTA = "/api/v1/app/autenticacao";

    private final AutenticacaoAppService app;
    private final AutenticacaoService autenticacao;
    private final LimiteTentativasApp limite;

    public AutenticacaoAppController(AutenticacaoAppService app, AutenticacaoService autenticacao,
                                     LimiteTentativasApp limite) {
        this.app = app;
        this.autenticacao = autenticacao;
        this.limite = limite;
    }

    @PostMapping(path = "/cadastro", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContaApp> cadastrar(@Valid @RequestBody CadastroEntrada dados, HttpServletRequest requisicao) {
        limite.cadastro(requisicao.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(app.cadastrar(dados, requisicao.getRemoteAddr()));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AcessoApp> login(@Valid @RequestBody LoginAppEntrada dados, HttpServletRequest requisicao) {
        limite.login(requisicao.getRemoteAddr(), dados.email());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(app.login(dados, requisicao.getRemoteAddr()));
    }

    @PostMapping(path = "/renovar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokensAutenticacao> renovar(@Valid @RequestBody RenovacaoEntrada dados,
                                                      HttpServletRequest requisicao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(autenticacao.renovar(
                AutenticacaoAppService.SLUG, dados.tokenAtualizacao(), null, requisicao.getRemoteAddr()));
    }

    @GetMapping("/eu")
    public ResponseEntity<ContaApp> eu(@AuthenticationPrincipal IdentidadeJwt identidade) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ContaApp.de(autenticacao.consultarUsuario(AutenticacaoAppService.SLUG, identidade)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal IdentidadeJwt identidade, HttpServletRequest requisicao) {
        autenticacao.logout(AutenticacaoAppService.SLUG, identidade, requisicao.getRemoteAddr());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
