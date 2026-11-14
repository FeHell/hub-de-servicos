package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AplicacaoInfo;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarAplicacao;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarPermissao;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarUsuario;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AuditoriaInfo;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaAplicacao;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissao;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissaoPlataforma;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaSenha;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuario;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.Pagina;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoInfo;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoPlataformaInfo;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioInfo;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioListagem;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TipoAcessoUsuario;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.DashboardUsuarios;
import br.com.hubdeservicos.service.administracao.AdministracaoPlataformaService;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;

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
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administração da plataforma")
public class AdministracaoPlataformaController {

    private final AdministracaoPlataformaService administracao;

    public AdministracaoPlataformaController(AdministracaoPlataformaService administracao) {
        this.administracao = administracao;
    }

    @GetMapping("/aplicacoes")
    public Pagina<AplicacaoInfo> listarAplicacoes(@RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarAplicacoes(pagina, tamanho);
    }

    @GetMapping("/aplicacoes/{id}")
    public AplicacaoInfo consultarAplicacao(@PathVariable UUID id) {
        return administracao.consultarAplicacao(id);
    }

    @PostMapping("/aplicacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AplicacaoInfo criarAplicacao(@Valid @RequestBody NovaAplicacao dados) {
        return administracao.criarAplicacao(dados);
    }

    @PutMapping("/aplicacoes/{id}")
    public AplicacaoInfo atualizarAplicacao(@PathVariable UUID id, @Valid @RequestBody AtualizarAplicacao dados) {
        return administracao.atualizarAplicacao(id, dados);
    }

    @GetMapping("/usuarios")
    public Pagina<UsuarioListagem> listarUsuarios(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho, @RequestParam(defaultValue = "") String login,
            @RequestParam(defaultValue = "") String email, @RequestParam(required = false) StatusUsuario status,
            @RequestParam(required = false) TipoAcessoUsuario tipo) {
        return administracao.listarUsuarios(pagina, tamanho, login, email, status, tipo);
    }

    @GetMapping("/usuarios/dashboard")
    public DashboardUsuarios dashboardUsuarios(@RequestParam(defaultValue = "0") int meses) {
        return administracao.dashboardUsuarios(meses);
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioListagem consultarUsuario(@PathVariable UUID id) {
        return administracao.consultarUsuario(id);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioInfo criarUsuario(@Valid @RequestBody NovoUsuario dados) {
        return administracao.criarUsuario(dados);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioInfo atualizarUsuario(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuario dados) {
        return administracao.atualizarUsuario(id, dados);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarUsuario(@PathVariable UUID id) {
        administracao.desativarUsuario(id);
    }

    @PutMapping("/usuarios/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@PathVariable UUID id, @Valid @RequestBody NovaSenha dados) {
        administracao.alterarSenha(id, dados.senha());
    }

    @GetMapping("/permissoes-plataforma")
    public List<PermissaoPlataformaInfo> listarPermissoesPlataforma() {
        return administracao.listarPermissoesPlataforma();
    }

    @PostMapping("/permissoes-plataforma")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissaoPlataformaInfo criarPermissaoPlataforma(@Valid @RequestBody NovaPermissaoPlataforma dados) {
        return administracao.criarPermissaoPlataforma(dados);
    }

    @GetMapping("/permissoes")
    public Pagina<PermissaoInfo> listarPermissoes(@RequestParam(defaultValue = "0") int pagina,
                                                  @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarPermissoes(pagina, tamanho);
    }

    @GetMapping("/permissoes/{id}")
    public PermissaoInfo consultarPermissao(@PathVariable UUID id) {
        return administracao.consultarPermissao(id);
    }

    @PostMapping("/permissoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissaoInfo criarPermissao(@Valid @RequestBody NovaPermissao dados) {
        return administracao.criarPermissao(dados);
    }

    @PutMapping("/permissoes/{id}")
    public PermissaoInfo atualizarPermissao(@PathVariable UUID id, @Valid @RequestBody AtualizarPermissao dados) {
        return administracao.atualizarPermissao(id, dados);
    }

    @PutMapping("/aplicacoes/{id}/permissoes/{idPermissao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void habilitarPermissao(@PathVariable UUID id, @PathVariable UUID idPermissao) {
        administracao.habilitarPermissao(id, idPermissao);
    }

    @DeleteMapping("/aplicacoes/{id}/permissoes/{idPermissao}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desabilitarPermissao(@PathVariable UUID id, @PathVariable UUID idPermissao) {
        administracao.desabilitarPermissao(id, idPermissao);
    }

    @GetMapping("/auditoria")
    public Pagina<AuditoriaInfo> listarAuditoria(@RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarAuditoria(pagina, tamanho);
    }
}
