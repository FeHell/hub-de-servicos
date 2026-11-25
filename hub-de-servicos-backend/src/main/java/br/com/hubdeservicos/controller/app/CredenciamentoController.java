package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.CredenciamentoBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.DadosBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.CompletudeBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ConfirmacaoAlteracao;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.EnviarRecebedorBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.RecebedorBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ReenviarRecebedorBean;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO;
import br.com.hubdeservicos.service.credenciamento.CredenciamentoService;
import br.com.hubdeservicos.service.credenciamento.RecebedorStoneService;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CredenciamentoController.ROTA)
public class CredenciamentoController {
    public static final String ROTA = "/api/v1/app/credenciamentos";
    private final CredenciamentoService credenciamentos;
    private final RecebedorStoneService recebedores;

    public CredenciamentoController(CredenciamentoService credenciamentos, RecebedorStoneService recebedores) {
        this.credenciamentos = credenciamentos;
        this.recebedores = recebedores;
    }

    @GetMapping("/{provedor}")
    public ResponseEntity<CredenciamentoBean> consultar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.consultar(identidade, provedor));
    }

    @GetMapping("/{provedor}/completude")
    public ResponseEntity<CompletudeBean> completude(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.consultar(identidade, provedor).getCompletude());
    }

    @PostMapping("/{provedor}/validacao")
    public ResponseEntity<CompletudeBean> validar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor, @RequestBody DadosBean dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.validar(identidade, dados));
    }

    @PostMapping("/{provedor}")
    public ResponseEntity<CredenciamentoBean> criar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody DadosBean dados) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(credenciamentos.salvar(identidade, provedor, dados, true, null, null, null));
    }

    @PutMapping("/{provedor}")
    public ResponseEntity<CredenciamentoBean> atualizar(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody DadosBean dados,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao,
            @RequestHeader(name = "X-Confirmar-Alteracao-Credenciamento", required = false) ConfirmacaoAlteracao confirmar,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.salvar(identidade, provedor, dados, false, versao, confirmar, id));
    }

    @DeleteMapping("/{provedor}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao) {
        credenciamentos.excluir(identidade, provedor, id, versao);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/{provedor}/recebedor")
    public ResponseEntity<RecebedorBean> consultarRecebedor(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(recebedores.consultar(identidade, provedor));
    }

    @PostMapping("/{provedor}/recebedor")
    public ResponseEntity<RecebedorBean> enviarRecebedor(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor, @RequestBody EnviarRecebedorBean pedido,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(recebedores.enviar(identidade, provedor, pedido, versao, id));
    }

    @PostMapping("/{provedor}/recebedor/reenvio")
    public ResponseEntity<RecebedorBean> reenviarRecebedor(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody ReenviarRecebedorBean pedido) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(recebedores.reenviar(identidade, provedor, pedido));
    }

    @PostMapping("/{provedor}/recebedor/consulta")
    public ResponseEntity<RecebedorBean> atualizarRecebedor(@AuthenticationPrincipal IdentidadeJwtVO identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(recebedores.reconciliar(identidade, provedor));
    }
}
