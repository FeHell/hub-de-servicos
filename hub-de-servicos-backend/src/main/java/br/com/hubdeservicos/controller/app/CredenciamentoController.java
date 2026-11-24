package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Credenciamento;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Dados;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Completude;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ConfirmacaoAlteracao;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.EnviarRecebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Recebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ReenviarRecebedor;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
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
    public ResponseEntity<Credenciamento> consultar(@AuthenticationPrincipal IdentidadeJwt identidade,
                                                    @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(credenciamentos.consultar(identidade, provedor));
    }

    @GetMapping("/{provedor}/completude")
    public ResponseEntity<Completude> completude(@AuthenticationPrincipal IdentidadeJwt identidade,
                                                 @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.consultar(identidade, provedor).completude());
    }

    @PostMapping("/{provedor}/validacao")
    public ResponseEntity<Completude> validar(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor, @RequestBody Dados dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(credenciamentos.validar(identidade, dados));
    }

    @PostMapping("/{provedor}")
    public ResponseEntity<Credenciamento> criar(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody Dados dados) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(credenciamentos.salvar(identidade, provedor, dados, true, null, null, null));
    }

    @PutMapping("/{provedor}")
    public ResponseEntity<Credenciamento> atualizar(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody Dados dados,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao,
            @RequestHeader(name = "X-Confirmar-Alteracao-Credenciamento", required = false) ConfirmacaoAlteracao confirmar,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.salvar(identidade, provedor, dados, false, versao, confirmar, id));
    }

    @DeleteMapping("/{provedor}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao) {
        credenciamentos.excluir(identidade, provedor, id, versao);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/{provedor}/recebedor")
    public ResponseEntity<Recebedor> consultarRecebedor(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recebedores.consultar(identidade, provedor));
    }

    @PostMapping("/{provedor}/recebedor")
    public ResponseEntity<Recebedor> enviarRecebedor(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor, @RequestBody EnviarRecebedor pedido,
            @RequestHeader(name = "X-Versao-Credenciamento", required = false) Long versao,
            @RequestHeader(name = "X-Credenciamento-Id", required = false) UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recebedores.enviar(identidade, provedor, pedido, versao, id));
    }

    @PostMapping("/{provedor}/recebedor/reenvio")
    public ResponseEntity<Recebedor> reenviarRecebedor(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor, @Valid @RequestBody ReenviarRecebedor pedido) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recebedores.reenviar(identidade, provedor, pedido));
    }

    @PostMapping("/{provedor}/recebedor/consulta")
    public ResponseEntity<Recebedor> atualizarRecebedor(@AuthenticationPrincipal IdentidadeJwt identidade,
            @PathVariable ProvedorCredenciamento provedor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recebedores.reconciliar(identidade, provedor));
    }
}
