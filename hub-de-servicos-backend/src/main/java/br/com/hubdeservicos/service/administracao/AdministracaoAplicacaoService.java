package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarPerfilBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AuditoriaInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoPerfilBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PaginaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PerfilInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.SessaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.VinculoInfoBean;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoId;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoPermissaoRepository;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoRepository;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.perfil.PerfilEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoId;
import br.com.hubdeservicos.repository.perfil.PerfilPermissaoRepository;
import br.com.hubdeservicos.repository.perfil.PerfilRepository;
import br.com.hubdeservicos.entity.sessao.StatusSessao;
import br.com.hubdeservicos.repository.sessao.SessaoRepository;
import br.com.hubdeservicos.service.sessao.RevogacaoSessaoService;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilId;
import br.com.hubdeservicos.repository.usuario.UsuarioAplicacaoRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioPerfilRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("@acessoAdministrativo.aplicacao(authentication, #slug)")
@Transactional(readOnly = true)
public class AdministracaoAplicacaoService {

    private final AplicacaoRepository aplicacoes;
    private final UsuarioRepository usuarios;
    private final UsuarioAplicacaoRepository vinculos;
    private final UsuarioPerfilRepository atribuicoes;
    private final PerfilRepository perfis;
    private final AplicacaoPermissaoRepository habilitacoes;
    private final PerfilPermissaoRepository permissoesPerfil;
    private final SessaoRepository sessoes;
    private final AuditoriaRepository auditoria;
    private final RevogacaoSessaoService revogacao;
    private final RegistroAdministrativo registro;
    private final AcessoAdministrativo acesso;
    private final Clock relogio;

    public AdministracaoAplicacaoService(AplicacaoRepository aplicacoes, UsuarioRepository usuarios,
            UsuarioAplicacaoRepository vinculos, UsuarioPerfilRepository atribuicoes, PerfilRepository perfis,
            AplicacaoPermissaoRepository habilitacoes, PerfilPermissaoRepository permissoesPerfil,
            SessaoRepository sessoes, AuditoriaRepository auditoria, RevogacaoSessaoService revogacao,
            RegistroAdministrativo registro, AcessoAdministrativo acesso, Clock relogio) {
        this.aplicacoes = aplicacoes;
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.atribuicoes = atribuicoes;
        this.perfis = perfis;
        this.habilitacoes = habilitacoes;
        this.permissoesPerfil = permissoesPerfil;
        this.sessoes = sessoes;
        this.auditoria = auditoria;
        this.revogacao = revogacao;
        this.registro = registro;
        this.acesso = acesso;
        this.relogio = relogio;
    }

    public PaginaBean<PerfilInfoBean> listarPerfis(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return PaginaBean
                .de(perfis.findByIdAplicacao(aplicacao, PaginaBean.requisicao(pagina, tamanho, Sort.by("nome", "id")))
                        .map(PerfilInfoBean::de));
    }

    public PerfilInfoBean consultarPerfil(String slug, UUID idPerfil) {
        return PerfilInfoBean.de(perfil(consultarAplicacao(slug).getId(), idPerfil));
    }

    @Transactional
    public PerfilInfoBean criarPerfil(String slug, NovoPerfilBean dados) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        var perfilEntityPreparado = new PerfilEntity(aplicacao, dados.getNome().strip(), dados.getDescricao(),
                StatusCadastro.ATIVO);
        var perfil = perfis.saveAndFlush(perfilEntityPreparado);
        registro.registrar("CRIAR_PERFIL", "perfil", aplicacao, perfil.getId());
        return PerfilInfoBean.de(perfil);
    }

    @Transactional
    public PerfilInfoBean atualizarPerfil(String slug, UUID idPerfil, AtualizarPerfilBean dados) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        PerfilEntity perfil = perfil(aplicacao, idPerfil);
        perfil.atualizar(dados.getNome().strip(), dados.getDescricao(), dados.getStatus());
        registro.registrar("ATUALIZAR_PERFIL", "perfil", aplicacao, idPerfil);
        return PerfilInfoBean.de(perfil);
    }

    public List<PermissaoInfoBean> listarPermissoes(String slug) {
        return habilitacoes.listarPermissoes(consultarAplicacao(slug).getId()).stream().map(PermissaoInfoBean::de)
                .toList();
    }

    public List<PermissaoInfoBean> listarPermissoesDoPerfil(String slug, UUID idPerfil) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        perfil(aplicacao, idPerfil);
        return permissoesPerfil.findByIdIdAplicacaoAndIdIdPerfilOrderByIdIdPermissao(aplicacao, idPerfil).stream()
                .map(v -> PermissaoInfoBean.de(v.getAplicacaoPermissao().getPermissao())).toList();
    }

    @Transactional
    public void atribuirPermissaoAoPerfil(String slug, UUID idPerfil, UUID idPermissao) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        PerfilEntity perfil = perfil(aplicacao, idPerfil);
        var aplicacaoPermissaoIdPreparado = new AplicacaoPermissaoId(aplicacao, idPermissao);
        var habilitacao = habilitacoes.findById(aplicacaoPermissaoIdPreparado).orElseThrow(() -> {
            var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.CONFLICT,
                    "A permissão precisa estar habilitada na aplicação.");
            return responseStatusExceptionPreparado;
        });
        if (perfil.getStatus() != StatusCadastro.ATIVO
                || habilitacao.getPermissao().getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O perfil e a permissão precisam estar ativos.");
        }
        var id = new PerfilPermissaoId(aplicacao, idPerfil, idPermissao);
        if (!permissoesPerfil.existsById(id)) {
            var perfilPermissaoEntityPreparado = new PerfilPermissaoEntity(id);
            permissoesPerfil.save(perfilPermissaoEntityPreparado);
            registro.registrar("ATRIBUIR_PERMISSAO_PERFIL", "perfil_permissao", aplicacao, idPerfil, idPermissao);
        }
    }

    @Transactional
    public void removerPermissaoDoPerfil(String slug, UUID idPerfil, UUID idPermissao) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        perfil(aplicacao, idPerfil);
        var id = new PerfilPermissaoId(aplicacao, idPerfil, idPermissao);
        if (permissoesPerfil.existsById(id)) {
            permissoesPerfil.deleteById(id);
            registro.registrar("REMOVER_PERMISSAO_PERFIL", "perfil_permissao", aplicacao, idPerfil, idPermissao);
        }
    }

    public PaginaBean<VinculoInfoBean> listarVinculos(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return PaginaBean.de(
                vinculos.findByIdIdAplicacao(aplicacao, PaginaBean.requisicao(pagina, tamanho, Sort.by("id.idUsuario")))
                        .map(VinculoInfoBean::de));
    }

    @Transactional
    public void vincularUsuario(String slug, UUID idUsuario) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        if (!usuarios.existsById(idUsuario)) {
            throw naoEncontrado("Usuário");
        }
        var id = new UsuarioAplicacaoId(idUsuario, aplicacao);
        var vinculo = vinculos.findById(id).orElse(null);
        if (vinculo == null) {
            var usuarioAplicacaoEntityPreparado = new UsuarioAplicacaoEntity(id);
            vinculos.save(usuarioAplicacaoEntityPreparado);
        } else {
            if (!vinculo.isAtivo()) {
                vinculo.definirAtivo(true);
            } else {
                return;
            }
        }
        registro.registrar("VINCULAR_USUARIO", "usuario_aplicacao", aplicacao, idUsuario);
    }

    @Transactional
    public void desvincularUsuario(String slug, UUID idUsuario) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        var usuarioAplicacaoIdPreparado = new UsuarioAplicacaoId(idUsuario, aplicacao);
        var vinculo = vinculos.findById(usuarioAplicacaoIdPreparado).orElse(null);
        if (vinculo != null && vinculo.isAtivo()) {
            vinculo.definirAtivo(false);
            atribuicoes.removerDaAplicacao(idUsuario, aplicacao);
            revogacao.revogarDoVinculo(idUsuario, aplicacao);
            registro.registrar("DESVINCULAR_USUARIO", "usuario_aplicacao", aplicacao, idUsuario);
        }
    }

    public List<PerfilInfoBean> listarPerfisDoUsuario(String slug, UUID idUsuario) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        verificarVinculoAtivo(idUsuario, aplicacao);
        return atribuicoes.findByIdIdUsuarioAndIdIdAplicacaoOrderByIdIdPerfil(idUsuario, aplicacao).stream()
                .map(v -> PerfilInfoBean.de(v.getPerfil())).toList();
    }

    @Transactional
    public void atribuirPerfil(String slug, UUID idUsuario, UUID idPerfil) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        verificarVinculoAtivo(idUsuario, aplicacao);
        if (perfil(aplicacao, idPerfil).getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O perfil está inativo.");
        }
        var id = new UsuarioPerfilId(idUsuario, aplicacao, idPerfil);
        if (!atribuicoes.existsById(id)) {
            var usuarioPerfilEntityPreparado = new UsuarioPerfilEntity(id);
            atribuicoes.save(usuarioPerfilEntityPreparado);
            registro.registrar("ATRIBUIR_PERFIL_USUARIO", "usuario_perfil", aplicacao, idUsuario, idPerfil);
        }
    }

    @Transactional
    public void removerPerfil(String slug, UUID idUsuario, UUID idPerfil) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        var id = new UsuarioPerfilId(idUsuario, aplicacao, idPerfil);
        if (atribuicoes.existsById(id)) {
            atribuicoes.deleteById(id);
            registro.registrar("REMOVER_PERFIL_USUARIO", "usuario_perfil", aplicacao, idUsuario, idPerfil);
        }
    }

    public PaginaBean<SessaoInfoBean> listarSessoes(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        var agora = relogio.instant();
        return PaginaBean
                .de(sessoes
                        .findByIdAplicacao(aplicacao,
                                PaginaBean.requisicao(pagina, tamanho,
                                        Sort.by(Sort.Direction.DESC, "dataCriacao", "id")))
                        .map(sessao -> SessaoInfoBean.de(sessao, agora)));
    }

    @Transactional
    public void revogarSessao(String slug, UUID idSessao) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        var sessao = sessoes.findByIdAndIdAplicacao(idSessao, aplicacao).orElseThrow(() -> naoEncontrado("Sessão"));
        if (sessao.getStatus() == StatusSessao.ATIVO) {
            revogacao.revogar(sessao, relogio.instant());
            registro.registrar("REVOGAR_SESSAO", "sessoes", aplicacao, idSessao);
        }
    }

    public PaginaBean<AuditoriaInfoBean> listarAuditoria(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return PaginaBean.de(auditoria
                .findByIdAplicacao(aplicacao,
                        PaginaBean.requisicao(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id")))
                .map(AuditoriaInfoBean::de));
    }

    private AplicacaoEntity consultarAplicacao(String slug) {
        return aplicacoes.findBySlug(slug).orElseThrow(() -> naoEncontrado("Aplicação"));
    }

    private UUID bloquearAplicacaoEditavel(String slug) {
        UUID aplicacao = aplicacoes.bloquearPorSlug(slug).orElseThrow(() -> naoEncontrado("Aplicação")).getId();
        acesso.verificarAplicacaoEditavel(aplicacao);
        return aplicacao;
    }

    private PerfilEntity perfil(UUID aplicacao, UUID idPerfil) {
        return perfis.findByIdAndIdAplicacao(idPerfil, aplicacao).orElseThrow(() -> naoEncontrado("Perfil"));
    }

    private void verificarVinculoAtivo(UUID idUsuario, UUID aplicacao) {
        var usuarioAplicacaoIdPreparado2 = new UsuarioAplicacaoId(idUsuario, aplicacao);
        if (!vinculos.existsByIdAndAtivoTrue(usuarioAplicacaoIdPreparado2)) {
            throw naoEncontrado("Vínculo ativo");
        }
    }

    private static ResponseStatusException naoEncontrado(String recurso) {
        var responseStatusExceptionPreparado2 = new ResponseStatusException(HttpStatus.NOT_FOUND,
                recurso + " não encontrado.");
        return responseStatusExceptionPreparado2;
    }
}
