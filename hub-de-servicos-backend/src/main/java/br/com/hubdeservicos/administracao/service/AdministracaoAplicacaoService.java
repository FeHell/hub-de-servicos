package br.com.hubdeservicos.administracao.service;

import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.AtualizarPerfil;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.AuditoriaInfo;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.NovoPerfil;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.Pagina;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.PerfilInfo;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.PermissaoInfo;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.SessaoInfo;
import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.VinculoInfo;
import br.com.hubdeservicos.aplicacao.entity.AplicacaoEntity;
import br.com.hubdeservicos.aplicacao.entity.AplicacaoPermissaoId;
import br.com.hubdeservicos.aplicacao.repository.AplicacaoPermissaoRepository;
import br.com.hubdeservicos.aplicacao.repository.AplicacaoRepository;
import br.com.hubdeservicos.auditoria.repository.AuditoriaRepository;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.perfil.entity.PerfilEntity;
import br.com.hubdeservicos.perfil.entity.PerfilPermissaoEntity;
import br.com.hubdeservicos.perfil.entity.PerfilPermissaoId;
import br.com.hubdeservicos.perfil.repository.PerfilPermissaoRepository;
import br.com.hubdeservicos.perfil.repository.PerfilRepository;
import br.com.hubdeservicos.sessao.entity.StatusSessao;
import br.com.hubdeservicos.sessao.repository.SessaoRepository;
import br.com.hubdeservicos.sessao.service.RevogacaoSessaoService;
import br.com.hubdeservicos.usuario.entity.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.usuario.entity.UsuarioAplicacaoId;
import br.com.hubdeservicos.usuario.entity.UsuarioPerfilEntity;
import br.com.hubdeservicos.usuario.entity.UsuarioPerfilId;
import br.com.hubdeservicos.usuario.repository.UsuarioAplicacaoRepository;
import br.com.hubdeservicos.usuario.repository.UsuarioPerfilRepository;
import br.com.hubdeservicos.usuario.repository.UsuarioRepository;

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
                                         UsuarioAplicacaoRepository vinculos, UsuarioPerfilRepository atribuicoes,
                                         PerfilRepository perfis, AplicacaoPermissaoRepository habilitacoes,
                                         PerfilPermissaoRepository permissoesPerfil, SessaoRepository sessoes,
                                         AuditoriaRepository auditoria, RevogacaoSessaoService revogacao,
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

    public Pagina<PerfilInfo> listarPerfis(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return Pagina.de(perfis.findByIdAplicacao(aplicacao, Pagina.requisicao(pagina, tamanho, Sort.by("nome", "id")))
                .map(PerfilInfo::de));
    }

    public PerfilInfo consultarPerfil(String slug, UUID idPerfil) {
        return PerfilInfo.de(perfil(consultarAplicacao(slug).getId(), idPerfil));
    }

    @Transactional
    public PerfilInfo criarPerfil(String slug, NovoPerfil dados) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        var perfil = perfis.saveAndFlush(new PerfilEntity(aplicacao, dados.nome().strip(), dados.descricao(),
                StatusCadastro.ATIVO));
        registro.registrar("CRIAR_PERFIL", "perfil", aplicacao, perfil.getId());
        return PerfilInfo.de(perfil);
    }

    @Transactional
    public PerfilInfo atualizarPerfil(String slug, UUID idPerfil, AtualizarPerfil dados) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        PerfilEntity perfil = perfil(aplicacao, idPerfil);
        perfil.atualizar(dados.nome().strip(), dados.descricao(), dados.status());
        registro.registrar("ATUALIZAR_PERFIL", "perfil", aplicacao, idPerfil);
        return PerfilInfo.de(perfil);
    }

    public List<PermissaoInfo> listarPermissoes(String slug) {
        return habilitacoes.listarPermissoes(consultarAplicacao(slug).getId())
                .stream().map(PermissaoInfo::de).toList();
    }

    public List<PermissaoInfo> listarPermissoesDoPerfil(String slug, UUID idPerfil) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        perfil(aplicacao, idPerfil);
        return permissoesPerfil.findByIdIdAplicacaoAndIdIdPerfilOrderByIdIdPermissao(aplicacao, idPerfil)
                .stream().map(v -> PermissaoInfo.de(v.getAplicacaoPermissao().getPermissao())).toList();
    }

    @Transactional
    public void atribuirPermissaoAoPerfil(String slug, UUID idPerfil, UUID idPermissao) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        PerfilEntity perfil = perfil(aplicacao, idPerfil);
        var habilitacao = habilitacoes.findById(new AplicacaoPermissaoId(aplicacao, idPermissao))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "A permissão precisa estar habilitada na aplicação."));
        if (perfil.getStatus() != StatusCadastro.ATIVO
                || habilitacao.getPermissao().getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O perfil e a permissão precisam estar ativos.");
        }
        var id = new PerfilPermissaoId(aplicacao, idPerfil, idPermissao);
        if (!permissoesPerfil.existsById(id)) {
            permissoesPerfil.save(new PerfilPermissaoEntity(id));
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

    public Pagina<VinculoInfo> listarVinculos(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return Pagina.de(vinculos.findByIdIdAplicacao(aplicacao, Pagina.requisicao(pagina, tamanho, Sort.by("id.idUsuario")))
                .map(VinculoInfo::de));
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
            vinculos.save(new UsuarioAplicacaoEntity(id));
        } else if (!vinculo.isAtivo()) {
            vinculo.definirAtivo(true);
        } else {
            return;
        }
        registro.registrar("VINCULAR_USUARIO", "usuario_aplicacao", aplicacao, idUsuario);
    }

    @Transactional
    public void desvincularUsuario(String slug, UUID idUsuario) {
        UUID aplicacao = bloquearAplicacaoEditavel(slug);
        var vinculo = vinculos.findById(new UsuarioAplicacaoId(idUsuario, aplicacao)).orElse(null);
        if (vinculo != null && vinculo.isAtivo()) {
            vinculo.definirAtivo(false);
            atribuicoes.removerDaAplicacao(idUsuario, aplicacao);
            revogacao.revogarDoVinculo(idUsuario, aplicacao);
            registro.registrar("DESVINCULAR_USUARIO", "usuario_aplicacao", aplicacao, idUsuario);
        }
    }

    public List<PerfilInfo> listarPerfisDoUsuario(String slug, UUID idUsuario) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        verificarVinculoAtivo(idUsuario, aplicacao);
        return atribuicoes.findByIdIdUsuarioAndIdIdAplicacaoOrderByIdIdPerfil(idUsuario, aplicacao)
                .stream().map(v -> PerfilInfo.de(v.getPerfil())).toList();
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
            atribuicoes.save(new UsuarioPerfilEntity(id));
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

    public Pagina<SessaoInfo> listarSessoes(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        var agora = relogio.instant();
        return Pagina.de(sessoes.findByIdAplicacao(aplicacao,
                Pagina.requisicao(pagina, tamanho, Sort.by(Sort.Direction.DESC, "dataCriacao", "id")))
                .map(sessao -> SessaoInfo.de(sessao, agora)));
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

    public Pagina<AuditoriaInfo> listarAuditoria(String slug, int pagina, int tamanho) {
        UUID aplicacao = consultarAplicacao(slug).getId();
        return Pagina.de(auditoria.findByIdAplicacao(aplicacao,
                Pagina.requisicao(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))).map(AuditoriaInfo::de));
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
        if (!vinculos.existsByIdAndAtivoTrue(new UsuarioAplicacaoId(idUsuario, aplicacao))) {
            throw naoEncontrado("Vínculo ativo");
        }
    }

    private static ResponseStatusException naoEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " não encontrado.");
    }
}
