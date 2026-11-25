package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastroBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.CadastroBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.ConfirmarTelefoneBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.DesafioBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IniciarConfirmacaoBean;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.app.ConfirmacaoTelefoneService;
import br.com.hubdeservicos.service.app.PerfilAppService;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IdentificarMunicipioBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.SelecionarPapelBean;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.OpcaoMunicipioBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.SelecionarMunicipioBean;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PerfilAppController.ROTA)
public class PerfilAppController {
    public static final String ROTA = "/api/v1/app/perfil";

    public static final String PAPEIS = "/papeis/{papel}";
    public static final String LOCALIZACAO = "/localizacao";

    public static final String LOCALIZACAO_MANUAL = LOCALIZACAO + "/manual";
    public static final String MUNICIPIOS = "/municipios";

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
    public ResponseEntity<CadastroBean> consultar(@AuthenticationPrincipal IdentidadeJwtVO identidade) {
        return resposta(perfil.consultar(usuario(identidade)));
    }

    @PutMapping
    public ResponseEntity<CadastroBean> atualizar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @Valid @RequestBody AtualizarCadastroBean dados) {
        return resposta(perfil.atualizar(usuario(identidade), dados));
    }

    @PutMapping(PAPEIS)
    public ResponseEntity<CadastroBean> selecionarPapel(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable PapelUsuario papel, @Valid @RequestBody SelecionarPapelBean dados) {
        return resposta(perfil.selecionarPapel(usuario(identidade), identidade.getIdAplicacao(), papel,
                dados.getSelecionado()));
    }

    @PutMapping(LOCALIZACAO)
    public ResponseEntity<CadastroBean> registrarMunicipio(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @Valid @RequestBody IdentificarMunicipioBean dados) {
        return resposta(perfil.registrarMunicipio(usuario(identidade), dados));
    }

    @GetMapping(MUNICIPIOS)
    public ResponseEntity<List<OpcaoMunicipioBean>> pesquisarMunicipios(
            @AuthenticationPrincipal IdentidadeJwtVO identidade, @RequestParam @Size(max = 120) String nome) {
        usuario(identidade);
        return resposta(perfil.pesquisarMunicipios(nome));
    }

    @PutMapping(LOCALIZACAO_MANUAL)
    public ResponseEntity<CadastroBean> selecionarMunicipio(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @Valid @RequestBody SelecionarMunicipioBean dados) {
        return resposta(perfil.selecionarMunicipio(usuario(identidade), dados.getCodigoIbge()));
    }

    @PostMapping("/telefone/confirmacao")
    public ResponseEntity<DesafioBean> iniciar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @Valid @RequestBody IniciarConfirmacaoBean dados) {
        return resposta(confirmacao.enviar(usuario(identidade), dados.getTelefone()));
    }

    @PostMapping("/telefone/confirmacao/reenviar")
    public ResponseEntity<DesafioBean> reenviar(@AuthenticationPrincipal IdentidadeJwtVO identidade) {
        return resposta(confirmacao.enviar(usuario(identidade), null));
    }

    @PostMapping("/telefone/confirmacao/validar")
    public ResponseEntity<CadastroBean> confirmar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @Valid @RequestBody ConfirmarTelefoneBean dados) {
        return resposta(confirmacao.confirmar(usuario(identidade), dados));
    }

    private UUID usuario(IdentidadeJwtVO identidade) {
        return autenticacao.consultarUsuario(AutenticacaoAppService.SLUG, identidade).getId();
    }

    private static <T> ResponseEntity<T> resposta(T dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
