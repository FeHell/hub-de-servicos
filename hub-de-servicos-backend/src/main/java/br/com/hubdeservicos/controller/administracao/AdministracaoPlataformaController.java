package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AplicacaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarAplicacaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarPermissaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarUsuarioBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AuditoriaInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaAplicacaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissaoPlataformaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaSenhaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuarioBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PaginaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoPlataformaInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioListagemBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TipoAcessoUsuario;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.DashboardUsuariosBean;
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
    public PaginaBean<AplicacaoInfoBean> listarAplicacoes(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarAplicacoes(pagina, tamanho);
    }

    @GetMapping("/aplicacoes/{id}")
    public AplicacaoInfoBean consultarAplicacao(@PathVariable UUID id) {
        return administracao.consultarAplicacao(id);
    }

    @PostMapping("/aplicacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AplicacaoInfoBean criarAplicacao(@Valid @RequestBody NovaAplicacaoBean dados) {
        return administracao.criarAplicacao(dados);
    }

    @PutMapping("/aplicacoes/{id}")
    public AplicacaoInfoBean atualizarAplicacao(@PathVariable UUID id,
            @Valid @RequestBody AtualizarAplicacaoBean dados) {
        return administracao.atualizarAplicacao(id, dados);
    }

    @GetMapping("/usuarios")
    public PaginaBean<UsuarioListagemBean> listarUsuarios(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho, @RequestParam(defaultValue = "") String login,
            @RequestParam(defaultValue = "") String email, @RequestParam(required = false) StatusUsuario status,
            @RequestParam(required = false) TipoAcessoUsuario tipo) {
        return administracao.listarUsuarios(pagina, tamanho, login, email, status, tipo);
    }

    @GetMapping("/usuarios/dashboard")
    public DashboardUsuariosBean dashboardUsuarios(@RequestParam(defaultValue = "0") int meses) {
        return administracao.dashboardUsuarios(meses);
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioListagemBean consultarUsuario(@PathVariable UUID id) {
        return administracao.consultarUsuario(id);
    }

    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioInfoBean criarUsuario(@Valid @RequestBody NovoUsuarioBean dados) {
        return administracao.criarUsuario(dados);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioInfoBean atualizarUsuario(@PathVariable UUID id, @Valid @RequestBody AtualizarUsuarioBean dados) {
        return administracao.atualizarUsuario(id, dados);
    }

    @DeleteMapping("/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarUsuario(@PathVariable UUID id) {
        administracao.desativarUsuario(id);
    }

    @PutMapping("/usuarios/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@PathVariable UUID id, @Valid @RequestBody NovaSenhaBean dados) {
        administracao.alterarSenha(id, dados.getSenha());
    }

    @GetMapping("/permissoes-plataforma")
    public List<PermissaoPlataformaInfoBean> listarPermissoesPlataforma() {
        return administracao.listarPermissoesPlataforma();
    }

    @PostMapping("/permissoes-plataforma")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissaoPlataformaInfoBean criarPermissaoPlataforma(@Valid @RequestBody NovaPermissaoPlataformaBean dados) {
        return administracao.criarPermissaoPlataforma(dados);
    }

    @GetMapping("/permissoes")
    public PaginaBean<PermissaoInfoBean> listarPermissoes(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarPermissoes(pagina, tamanho);
    }

    @GetMapping("/permissoes/{id}")
    public PermissaoInfoBean consultarPermissao(@PathVariable UUID id) {
        return administracao.consultarPermissao(id);
    }

    @PostMapping("/permissoes")
    @ResponseStatus(HttpStatus.CREATED)
    public PermissaoInfoBean criarPermissao(@Valid @RequestBody NovaPermissaoBean dados) {
        return administracao.criarPermissao(dados);
    }

    @PutMapping("/permissoes/{id}")
    public PermissaoInfoBean atualizarPermissao(@PathVariable UUID id,
            @Valid @RequestBody AtualizarPermissaoBean dados) {
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
    public PaginaBean<AuditoriaInfoBean> listarAuditoria(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return administracao.listarAuditoria(pagina, tamanho);
    }
}
