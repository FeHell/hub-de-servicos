package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor.UnidadeMonetaria;
import br.com.hubdeservicos.service.credenciamento.CredenciamentoService;
import br.com.hubdeservicos.service.credenciamento.CredenciamentoService.Previa;
import br.com.hubdeservicos.service.credenciamento.DiagnosticoRecebedorService;
import br.com.hubdeservicos.service.credenciamento.DiagnosticoRecebedorService.Resumo;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/credenciamentos")
public class AdministracaoCredenciamentoController {
    private final CredenciamentoService credenciamentos;
    private final DiagnosticoRecebedorService diagnosticos;

    public AdministracaoCredenciamentoController(CredenciamentoService credenciamentos, DiagnosticoRecebedorService diagnosticos) {
        this.credenciamentos = credenciamentos;
        this.diagnosticos = diagnosticos;
    }

    @GetMapping("/{id}/payload")
    public ResponseEntity<Previa> preparar(@PathVariable UUID id, @RequestParam UnidadeMonetaria unidadeMonetaria) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(credenciamentos.preparar(id, unidadeMonetaria));
    }

    @GetMapping("/tentativas/{tentativa}/diagnostico")
    public ResponseEntity<Resumo> diagnostico(@PathVariable UUID tentativa) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(diagnosticos.consultar(tentativa));
    }
}
