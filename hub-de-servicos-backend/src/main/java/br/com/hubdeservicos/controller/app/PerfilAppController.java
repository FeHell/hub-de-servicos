package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.Cadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.ConfirmarTelefone;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.Desafio;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IniciarConfirmacao;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.app.ConfirmacaoTelefoneService;
import br.com.hubdeservicos.service.app.PerfilAppService;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PerfilAppController.ROTA)
public class PerfilAppController {
    public static final String ROTA = "/api/v1/app/perfil";

    private final AutenticacaoService autenticacao;
    private final PerfilAppService perfil;
    private final ConfirmacaoTelefoneService confirmacao;

    public PerfilAppController(AutenticacaoService autenticacao, PerfilAppService perfil,
                              ConfirmacaoTelefoneService confirmacao) {
        this.autenticacao = autenticacao;
        this.perfil = perfil;
        this.confirmacao = confirmacao;
    }

    @GetMapping
    public ResponseEntity<Cadastro> consultar(@AuthenticationPrincipal IdentidadeJwt identidade) {
        return resposta(perfil.consultar(usuario(identidade)));
    }

    @PutMapping
    public ResponseEntity<Cadastro> atualizar(@AuthenticationPrincipal IdentidadeJwt identidade,
                                            @Valid @RequestBody AtualizarCadastro dados) {
        return resposta(perfil.atualizar(usuario(identidade), dados));
    }

    @PostMapping("/telefone/confirmacao")
    public ResponseEntity<Desafio> iniciar(@AuthenticationPrincipal IdentidadeJwt identidade,
                                         @Valid @RequestBody IniciarConfirmacao dados) {
        return resposta(confirmacao.enviar(usuario(identidade), dados.telefone()));
    }

    @PostMapping("/telefone/confirmacao/reenviar")
    public ResponseEntity<Desafio> reenviar(@AuthenticationPrincipal IdentidadeJwt identidade) {
        return resposta(confirmacao.enviar(usuario(identidade), null));
    }

    @PostMapping("/telefone/confirmacao/validar")
    public ResponseEntity<Cadastro> confirmar(@AuthenticationPrincipal IdentidadeJwt identidade,
                                            @Valid @RequestBody ConfirmarTelefone dados) {
        return resposta(confirmacao.confirmar(usuario(identidade), dados));
    }

    private UUID usuario(IdentidadeJwt identidade) {
        return autenticacao.consultarUsuario(AutenticacaoAppService.SLUG, identidade).id();
    }

    private static <T> ResponseEntity<T> resposta(T dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
