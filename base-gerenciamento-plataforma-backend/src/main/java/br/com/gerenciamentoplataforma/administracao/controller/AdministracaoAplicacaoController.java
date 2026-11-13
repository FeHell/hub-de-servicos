package br.com.gerenciamentoplataforma.administracao.controller;

import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AtualizarPerfil;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AuditoriaInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovoPerfil;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.Pagina;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.PerfilInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.PermissaoInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.SessaoInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.VinculoInfo;
import br.com.gerenciamentoplataforma.administracao.service.AdministracaoAplicacaoService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aplicacoes/{slug}/admin")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administração da aplicação")
public class AdministracaoAplicacaoController {

    private final AdministracaoAplicacaoService administracao;

    public AdministracaoAplicacaoController(AdministracaoAplicacaoService administracao) {
        this.administracao = administracao;
    }

    @GetMapping("/perfis")
    public Pagina<PerfilInfo> listarPerfis(@PathVariable String slug, @RequestParam(defaultValue = "0") int pagina,
                                          @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarPerfis(slug, pagina, tamanho);
    }

    @GetMapping("/perfis/{idPerfil}")
    public PerfilInfo consultarPerfil(@PathVariable String slug, @PathVariable UUID idPerfil) {
        return administracao.consultarPerfil(slug, idPerfil);
    }

    @PostMapping("/perfis")
    @ResponseStatus(HttpStatus.CREATED)
    public PerfilInfo criarPerfil(@PathVariable String slug, @Valid @RequestBody NovoPerfil dados) {
        return administracao.criarPerfil(slug, dados);
    }

    @PutMapping("/perfis/{idPerfil}")
    public PerfilInfo atualizarPerfil(@PathVariable String slug, @PathVariable UUID idPerfil,
                                      @Valid @RequestBody AtualizarPerfil dados) {
        return administracao.atualizarPerfil(slug, idPerfil, dados);
    }

    @GetMapping("/permissoes")
    public List<PermissaoInfo> listarPermissoes(@PathVariable String slug) {
        return administracao.listarPermissoes(slug);
    }

    @GetMapping("/perfis/{idPerfil}/permissoes")
    public List<PermissaoInfo> listarPermissoesDoPerfil(@PathVariable String slug, @PathVariable UUID idPerfil) {
        return administracao.listarPermissoesDoPerfil(slug, idPerfil);
    }

    @PutMapping("/perfis/{idPerfil}/permissoes/{idPermissao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atribuirPermissao(@PathVariable String slug, @PathVariable UUID idPerfil,
                                  @PathVariable UUID idPermissao) {
        administracao.atribuirPermissaoAoPerfil(slug, idPerfil, idPermissao);
    }

    @DeleteMapping("/perfis/{idPerfil}/permissoes/{idPermissao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPermissao(@PathVariable String slug, @PathVariable UUID idPerfil,
                                 @PathVariable UUID idPermissao) {
        administracao.removerPermissaoDoPerfil(slug, idPerfil, idPermissao);
    }

    @GetMapping("/usuarios")
    public Pagina<VinculoInfo> listarVinculos(@PathVariable String slug, @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarVinculos(slug, pagina, tamanho);
    }

    @PutMapping("/usuarios/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vincularUsuario(@PathVariable String slug, @PathVariable UUID idUsuario) {
        administracao.vincularUsuario(slug, idUsuario);
    }

    @DeleteMapping("/usuarios/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincularUsuario(@PathVariable String slug, @PathVariable UUID idUsuario) {
        administracao.desvincularUsuario(slug, idUsuario);
    }

    @GetMapping("/usuarios/{idUsuario}/perfis")
    public List<PerfilInfo> listarPerfisDoUsuario(@PathVariable String slug, @PathVariable UUID idUsuario) {
        return administracao.listarPerfisDoUsuario(slug, idUsuario);
    }

    @PutMapping("/usuarios/{idUsuario}/perfis/{idPerfil}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atribuirPerfil(@PathVariable String slug, @PathVariable UUID idUsuario, @PathVariable UUID idPerfil) {
        administracao.atribuirPerfil(slug, idUsuario, idPerfil);
    }

    @DeleteMapping("/usuarios/{idUsuario}/perfis/{idPerfil}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPerfil(@PathVariable String slug, @PathVariable UUID idUsuario, @PathVariable UUID idPerfil) {
        administracao.removerPerfil(slug, idUsuario, idPerfil);
    }

    @GetMapping("/sessoes")
    public Pagina<SessaoInfo> listarSessoes(@PathVariable String slug, @RequestParam(defaultValue = "0") int pagina,
                                            @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarSessoes(slug, pagina, tamanho);
    }

    @DeleteMapping("/sessoes/{idSessao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revogarSessao(@PathVariable String slug, @PathVariable UUID idSessao) {
        administracao.revogarSessao(slug, idSessao);
    }

    @GetMapping("/auditoria")
    public Pagina<AuditoriaInfo> listarAuditoria(@PathVariable String slug,
                                                 @RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarAuditoria(slug, pagina, tamanho);
    }
}
