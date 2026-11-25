package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.app.ContratosApp.AcessoAppBean;
import br.com.hubdeservicos.dto.app.ContratosApp.CadastroEntradaBean;
import br.com.hubdeservicos.dto.app.ContratosApp.ContaAppBean;
import br.com.hubdeservicos.dto.app.ContratosApp.LoginAppEntradaBean;
import br.com.hubdeservicos.dto.autenticacao.RenovacaoEntradaBean;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacaoBean;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.app.LimiteTentativasApp;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO;

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
    public ResponseEntity<ContaAppBean> cadastrar(@Valid @RequestBody CadastroEntradaBean dados,
            HttpServletRequest requisicao) {
        limite.cadastro(requisicao.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(app.cadastrar(dados, requisicao.getRemoteAddr()));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AcessoAppBean> login(@Valid @RequestBody LoginAppEntradaBean dados,
            HttpServletRequest requisicao) {
        limite.login(requisicao.getRemoteAddr(), dados.getEmail());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(app.login(dados, requisicao.getRemoteAddr()));
    }

    @PostMapping(path = "/renovar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TokensAutenticacaoBean> renovar(@Valid @RequestBody RenovacaoEntradaBean dados,
            HttpServletRequest requisicao) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(autenticacao
                .renovar(AutenticacaoAppService.SLUG, dados.getTokenAtualizacao(), null, requisicao.getRemoteAddr()));
    }

    @GetMapping("/eu")
    public ResponseEntity<ContaAppBean> eu(@AuthenticationPrincipal IdentidadeJwtVO identidade) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ContaAppBean.de(autenticacao.consultarUsuario(AutenticacaoAppService.SLUG, identidade)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            HttpServletRequest requisicao) {
        autenticacao.logout(AutenticacaoAppService.SLUG, identidade, requisicao.getRemoteAddr());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
