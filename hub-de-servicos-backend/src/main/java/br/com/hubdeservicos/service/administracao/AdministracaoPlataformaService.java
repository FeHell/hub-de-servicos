package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AplicacaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarAplicacaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarPermissaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AtualizarUsuarioBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.AuditoriaInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.DashboardUsuariosBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaAplicacaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovaPermissaoPlataformaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuarioBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PaginaBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoPlataformaInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.PermissaoUsuarioBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TipoAcessoUsuario;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TotalUsuariosMesBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TotalUsuariosPermissaoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TotalUsuariosStatusBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.TotalUsuariosTipoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioInfoBean;
import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioListagemBean;
import br.com.hubdeservicos.repository.administracao.ConsultaUsuariosRepository.PermissaoDoUsuario;
import br.com.hubdeservicos.repository.administracao.ConsultaUsuariosRepository;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoId;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoPermissaoRepository;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoRepository;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.perfil.PerfilPermissaoRepository;
import br.com.hubdeservicos.entity.permissao.PermissaoEntity;
import br.com.hubdeservicos.repository.permissao.PermissaoRepository;
import br.com.hubdeservicos.service.sessao.RevogacaoSessaoService;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.usuario.CadastroUsuarioService;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import br.com.hubdeservicos.repository.usuario.UsuarioPermissaoPlataformaRepository;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("@acessoAdministrativo.plataforma(authentication)")
@Transactional(readOnly = true)
public class AdministracaoPlataformaService {

    private final AplicacaoRepository aplicacoes;
    private final UsuarioRepository usuarios;
    private final EntityManager persistencia;
    private final CadastroUsuarioService cadastroUsuarios;
    private final TelefoneUsuarioService telefones;
    private final ConsultaUsuariosRepository consultaUsuarios;
    private final PermissaoRepository permissoes;
    private final UsuarioPermissaoPlataformaRepository atribuicoes;
    private final PermissoesPlataformaService permissoesPlataforma;
    private final AplicacaoPermissaoRepository habilitacoes;
    private final PerfilPermissaoRepository permissoesPerfil;
    private final AuditoriaRepository auditoria;
    private final RegistroAdministrativo registro;
    private final AcessoAdministrativo acesso;
    private final SenhasAdministrativas senhas;
    private final RevogacaoSessaoService revogacao;

    public AdministracaoPlataformaService(AplicacaoRepository aplicacoes, UsuarioRepository usuarios,
            CadastroUsuarioService cadastroUsuarios, TelefoneUsuarioService telefones, EntityManager persistencia,
            ConsultaUsuariosRepository consultaUsuarios, PermissaoRepository permissoes,
            UsuarioPermissaoPlataformaRepository atribuicoes, PermissoesPlataformaService permissoesPlataforma,
            AplicacaoPermissaoRepository habilitacoes, PerfilPermissaoRepository permissoesPerfil,
            AuditoriaRepository auditoria, RegistroAdministrativo registro, AcessoAdministrativo acesso,
            SenhasAdministrativas senhas, RevogacaoSessaoService revogacao) {
        this.aplicacoes = aplicacoes;
        this.usuarios = usuarios;
        this.persistencia = persistencia;
        this.cadastroUsuarios = cadastroUsuarios;
        this.telefones = telefones;
        this.consultaUsuarios = consultaUsuarios;
        this.permissoes = permissoes;
        this.atribuicoes = atribuicoes;
        this.permissoesPlataforma = permissoesPlataforma;
        this.habilitacoes = habilitacoes;
        this.permissoesPerfil = permissoesPerfil;
        this.auditoria = auditoria;
        this.registro = registro;
        this.acesso = acesso;
        this.senhas = senhas;
        this.revogacao = revogacao;
    }

    public PaginaBean<AplicacaoInfoBean> listarAplicacoes(int pagina, int tamanho) {
        return PaginaBean.de(
                aplicacoes.findAll(PaginaBean.requisicao(pagina, tamanho, Sort.by("slug"))).map(AplicacaoInfoBean::de));
    }

    public AplicacaoInfoBean consultarAplicacao(UUID id) {
        return AplicacaoInfoBean.de(aplicacoes.findById(id).orElseThrow(() -> naoEncontrado("Aplicação")));
    }

    @Transactional
    public AplicacaoInfoBean criarAplicacao(NovaAplicacaoBean dados) {
        var aplicacaoEntityPreparado = new AplicacaoEntity(dados.getNome().strip(), dados.getSlug(), dados.getTipo(),
                dados.getDescricao(), StatusCadastro.ATIVO);
        var aplicacao = aplicacoes.saveAndFlush(aplicacaoEntityPreparado);
        registro.registrar("CRIAR_APLICACAO", "aplicacao", aplicacao.getId(), aplicacao.getId());
        return AplicacaoInfoBean.de(aplicacao);
    }

    @Transactional
    public AplicacaoInfoBean atualizarAplicacao(UUID id, AtualizarAplicacaoBean dados) {
        AplicacaoEntity aplicacao = bloquearAplicacaoEditavel(id);
        boolean desativou = aplicacao.getStatus() == StatusCadastro.ATIVO
                && dados.getStatus() == StatusCadastro.INATIVO;
        aplicacao.atualizar(dados.getNome().strip(), dados.getDescricao(), dados.getStatus());
        if (desativou) {
            revogacao.revogarDaAplicacao(id);
        }
        registro.registrar("ATUALIZAR_APLICACAO", "aplicacao", id, id);
        return AplicacaoInfoBean.de(aplicacao);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaBean<UsuarioListagemBean> listarUsuarios(int pagina, int tamanho, String login, String email,
            StatusUsuario status, TipoAcessoUsuario tipo) {
        UUID administracao = acesso.idAplicacaoPlataforma();
        var encontrados = consultaUsuarios.pesquisar(login.strip(), email.strip(), status,
                tipo == null ? null : tipo.name(), administracao, AcessoAdministrativo.PLATAFORMA_ADMINISTRAR,
                AcessoAdministrativo.APLICACAO_ADMINISTRAR, PaginaBean.requisicao(pagina, tamanho, Sort.by("login")));
        if (encontrados.isEmpty()) {
            PaginaBean<br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioListagemBean> paginaPreparado = new PaginaBean<br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.UsuarioListagemBean>();
            paginaPreparado.setItens(List.of());
            paginaPreparado.setPagina(encontrados.getNumber());
            paginaPreparado.setTamanho(encontrados.getSize());
            paginaPreparado.setTotal(encontrados.getTotalElements());
            return paginaPreparado;
        }
        var ids = encontrados.stream().map(UsuarioEntity::getId).toList();
        var porUsuario = permissoesEfetivas(administracao, ids)
                .collect(Collectors.groupingBy(PermissaoDoUsuario::getIdUsuario));
        var atribuicoesPorUsuario = atribuicoes.listarAtribuicoes(ids).stream().collect(Collectors.groupingBy(
                UsuarioPermissaoPlataformaRepository.Atribuicao::getIdUsuario,
                Collectors.mapping(UsuarioPermissaoPlataformaRepository.Atribuicao::getCodigo, Collectors.toList())));
        return PaginaBean
                .de(encontrados.map(u -> comAcessos(u, administracao, porUsuario.getOrDefault(u.getId(), List.of()),
                        atribuicoesPorUsuario.getOrDefault(u.getId(), List.of()))));
    }

    private Stream<PermissaoDoUsuario> permissoesEfetivas(UUID administracao, List<UUID> usuarios) {
        return Stream.concat(consultaUsuarios.permissoesDaPlataforma(administracao, usuarios).stream(),
                consultaUsuarios.permissoes(administracao, usuarios).stream());
    }

    private static UsuarioListagemBean comAcessos(UsuarioEntity usuario, UUID administracao,
            List<PermissaoDoUsuario> permissoes, List<String> atribuicoes) {
        return UsuarioListagemBean.de(usuario, tipoAcesso(administracao, permissoes), permissoes.stream().map(p -> {
            PermissaoUsuarioBean permissaoUsuarioPreparado = new PermissaoUsuarioBean();
            permissaoUsuarioPreparado.setAplicacao(p.getAplicacao());
            permissaoUsuarioPreparado.setCodigo(p.getCodigo());
            return permissaoUsuarioPreparado;
        }).toList(), atribuicoes);
    }

    private static TipoAcessoUsuario tipoAcesso(UUID administracao, List<PermissaoDoUsuario> permissoesAtivas) {
        if (permissoesAtivas.stream().anyMatch(p -> administracao.equals(p.getIdAplicacao())
                && AcessoAdministrativo.PLATAFORMA_ADMINISTRAR.equals(p.getCodigo()))) {
            return TipoAcessoUsuario.PLATAFORMA;
        }
        if (permissoesAtivas.stream().anyMatch(p -> !administracao.equals(p.getIdAplicacao())
                && AcessoAdministrativo.APLICACAO_ADMINISTRAR.equals(p.getCodigo()))) {
            return TipoAcessoUsuario.APLICACAO;
        }
        return TipoAcessoUsuario.USUARIO;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardUsuariosBean dashboardUsuarios(int meses) {
        if (meses < 0 || meses > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe de 0 a 24 meses.");
        }
        Instant agora = Instant.now();
        int mesesDaSerie = meses == 0 ? 12 : meses;
        YearMonth inicioSerie = YearMonth.from(agora.atOffset(ZoneOffset.UTC)).minusMonths(mesesDaSerie - 1L);
        Instant primeiroMes = inicioSerie.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant inicio = meses == 0 ? Instant.parse("0001-01-01T00:00:00Z") : primeiroMes;
        Instant limiteRecentes = agora.minus(30, ChronoUnit.DAYS);
        long recentes = consultaUsuarios
                .countByDataCriacaoBetween(inicio.isAfter(limiteRecentes) ? inicio : limiteRecentes, agora);
        var status = consultaUsuarios.contarPorStatus(inicio, agora).stream()
                .collect(Collectors.toMap(ConsultaUsuariosRepository.ContagemStatus::getStatus,
                        ConsultaUsuariosRepository.ContagemStatus::getTotal));
        var contagemMeses = consultaUsuarios.contarPorMes(primeiroMes, agora).stream().collect(Collectors.toMap(
                ConsultaUsuariosRepository.ContagemMes::getMes, ConsultaUsuariosRepository.ContagemMes::getTotal));
        var tipos = consultaUsuarios
                .contarPorTipo(inicio, agora, acesso.idAplicacaoPlataforma(),
                        AcessoAdministrativo.PLATAFORMA_ADMINISTRAR, AcessoAdministrativo.APLICACAO_ADMINISTRAR)
                .stream().collect(Collectors.toMap(t -> TipoAcessoUsuario.valueOf(t.getTipo()),
                        ConsultaUsuariosRepository.ContagemTipo::getTotal));
        var porStatus = Arrays.stream(StatusUsuario.values()).map(s -> {
            TotalUsuariosStatusBean totalUsuariosStatusPreparado = new TotalUsuariosStatusBean();
            totalUsuariosStatusPreparado.setStatus(s);
            totalUsuariosStatusPreparado.setTotal(status.getOrDefault(s, 0L));
            return totalUsuariosStatusPreparado;
        }).toList();
        var porTipo = Arrays.stream(TipoAcessoUsuario.values()).map(t -> {
            TotalUsuariosTipoBean totalUsuariosTipoPreparado = new TotalUsuariosTipoBean();
            totalUsuariosTipoPreparado.setTipo(t);
            totalUsuariosTipoPreparado.setTotal(tipos.getOrDefault(t, 0L));
            return totalUsuariosTipoPreparado;
        }).toList();
        UUID administracao = acesso.idAplicacaoPlataforma();
        var porPermissao = Stream
                .concat(consultaUsuarios.contarPorPermissao(administracao, inicio, agora).stream(),
                        consultaUsuarios.contarPorPermissaoDaPlataforma(administracao, inicio, agora).stream())
                .map(p -> {
                    TotalUsuariosPermissaoBean totalUsuariosPermissaoPreparado = new TotalUsuariosPermissaoBean();
                    totalUsuariosPermissaoPreparado.setAplicacao(p.getAplicacao());
                    totalUsuariosPermissaoPreparado.setCodigo(p.getCodigo());
                    totalUsuariosPermissaoPreparado.setTotal(p.getTotal());
                    return totalUsuariosPermissaoPreparado;
                })
                .sorted(Comparator.comparingLong(TotalUsuariosPermissaoBean::getTotal).reversed()
                        .thenComparing(TotalUsuariosPermissaoBean::getAplicacao)
                        .thenComparing(TotalUsuariosPermissaoBean::getCodigo))
                .toList();
        var porMes = IntStream.range(0, mesesDaSerie).mapToObj(i -> inicioSerie.plusMonths(i).toString()).map(m -> {
            TotalUsuariosMesBean totalUsuariosMesPreparado = new TotalUsuariosMesBean();
            totalUsuariosMesPreparado.setMes(m);
            totalUsuariosMesPreparado.setTotal(contagemMeses.getOrDefault(m, 0L));
            return totalUsuariosMesPreparado;
        }).toList();
        DashboardUsuariosBean dashboardUsuariosPreparado = new DashboardUsuariosBean();
        dashboardUsuariosPreparado.setTotal(porStatus.stream().mapToLong(TotalUsuariosStatusBean::getTotal).sum());
        dashboardUsuariosPreparado.setRecentes(recentes);
        dashboardUsuariosPreparado.setPorStatus(porStatus);
        dashboardUsuariosPreparado.setPorTipo(porTipo);
        dashboardUsuariosPreparado.setPorPermissao(porPermissao);
        dashboardUsuariosPreparado.setPorMes(porMes);
        return dashboardUsuariosPreparado;
    }

    public UsuarioListagemBean consultarUsuario(UUID id) {
        UsuarioEntity usuario = usuarios.findById(id).orElseThrow(() -> naoEncontrado("Usuário"));
        UUID administracao = acesso.idAplicacaoPlataforma();
        return comAcessos(usuario, administracao, permissoesEfetivas(administracao, List.of(id)).toList(),
                permissoesPlataforma.consultar(id));
    }

    @Transactional
    public UsuarioInfoBean criarUsuario(NovoUsuarioBean dados) {
        StatusUsuario status = dados.getStatus() == null ? StatusUsuario.ATIVO : dados.getStatus();
        String hash = senhas.gerarHash(dados.getSenha());
        UUID administracao = bloquearAlteracoesDeUsuarios();
        var usuario = cadastroUsuarios.salvar(dados.getLogin(), hash, dados.getEmail(), status);
        usuario.atualizarNome(dados.getNome());
        telefones.atualizar(usuario, dados.getTelefone());
        registro.registrar("CRIAR_USUARIO", "usuario", null, usuario.getId());
        var codigos = dados.getPermissoesPlataforma() == null ? List.<String>of() : dados.getPermissoesPlataforma();
        return UsuarioInfoBean.de(usuario, permissoesPlataforma.substituir(usuario.getId(), administracao, codigos));
    }

    @Transactional
    public UsuarioInfoBean atualizarUsuario(UUID id, AtualizarUsuarioBean dados) {
        UUID administracao = bloquearAlteracoesDeUsuarios();
        if (id.equals(registro.idOperador()) && dados.getStatus() != StatusUsuario.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O administrador deve manter a própria conta ativa.");
        }
        UsuarioEntity usuario = usuarios.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Usuário"));
        persistencia.refresh(usuario);
        boolean desativou = usuario.getStatus() == StatusUsuario.ATIVO && dados.getStatus() != StatusUsuario.ATIVO;
        var atuais = permissoesPlataforma.consultar(id);
        var desejadas = permissoesPlataforma.conciliar(atuais, dados.getPermissoesPlataforma(),
                dados.getPermissoesOriginais());
        verificarUltimoAdministrador(usuario, dados.getStatus(), atuais, desejadas == null ? atuais : desejadas);
        usuario.atualizar(dados.getLogin().strip(), dados.getEmail().strip(), dados.getStatus());
        if (dados.getNome() != null) {
            usuario.atualizarNome(dados.getNome());
        }
        if (dados.getTelefone() != null) {
            telefones.atualizar(usuario, dados.getTelefone());
        }
        var codigos = desejadas == null ? atuais : permissoesPlataforma.substituir(id, administracao, desejadas);
        if (desativou) {
            revogacao.revogarDoUsuario(id);
        }
        registro.registrar("ATUALIZAR_USUARIO", "usuario", null, id);
        return UsuarioInfoBean.de(usuario, codigos);
    }

    @Transactional
    public void desativarUsuario(UUID id) {
        bloquearAlteracoesDeUsuarios();
        if (id.equals(registro.idOperador())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O administrador deve manter a própria conta ativa.");
        }
        UsuarioEntity usuario = usuarios.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Usuário"));
        if (usuario.getStatus() == StatusUsuario.INATIVO) {
            return;
        }
        var atuais = permissoesPlataforma.consultar(id);
        verificarUltimoAdministrador(usuario, StatusUsuario.INATIVO, atuais, atuais);
        usuario.atualizar(usuario.getLogin(), usuario.getEmail(), StatusUsuario.INATIVO);
        revogacao.revogarDoUsuario(id);
        registro.registrar("DESATIVAR_USUARIO", "usuario", null, id);
    }

    @Transactional
    public void alterarSenha(UUID id, String senha) {
        String hash = senhas.gerarHash(senha);
        UsuarioEntity usuario = usuarios.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Usuário"));
        usuario.alterarSenhaHash(hash);
        revogacao.revogarDoUsuario(id);
        registro.registrar("ALTERAR_SENHA", "usuario", null, id);
    }

    public List<PermissaoPlataformaInfoBean> listarPermissoesPlataforma() {
        return permissoes.findByNomePlataformaIsNotNullOrderByNomePlataformaAscCodigoAsc().stream()
                .map(PermissaoPlataformaInfoBean::de).toList();
    }

    @Transactional
    public PermissaoPlataformaInfoBean criarPermissaoPlataforma(NovaPermissaoPlataformaBean dados) {
        var permissao = permissoes.saveAndFlush(
                PermissaoEntity.daPlataforma(dados.getCodigo(), dados.getNome().strip(), dados.getDescricao()));
        registro.registrar("CRIAR_PERMISSAO_PLATAFORMA", "permissao", null, permissao.getId());
        return PermissaoPlataformaInfoBean.de(permissao);
    }

    public PaginaBean<PermissaoInfoBean> listarPermissoes(int pagina, int tamanho) {
        return PaginaBean.de(permissoes.findAll(PaginaBean.requisicao(pagina, tamanho, Sort.by("codigo")))
                .map(PermissaoInfoBean::de));
    }

    public PermissaoInfoBean consultarPermissao(UUID id) {
        return PermissaoInfoBean.de(permissoes.findById(id).orElseThrow(() -> naoEncontrado("Permissão")));
    }

    @Transactional
    public PermissaoInfoBean criarPermissao(NovaPermissaoBean dados) {
        if (AcessoAdministrativo.PLATAFORMA_ADMINISTRAR.equals(dados.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Código reservado à administração da plataforma.");
        }
        var permissaoEntityPreparado = new PermissaoEntity(dados.getCodigo(), dados.getDescricao(),
                StatusCadastro.ATIVO);
        var permissao = permissoes.saveAndFlush(permissaoEntityPreparado);
        registro.registrar("CRIAR_PERMISSAO", "permissao", null, permissao.getId());
        return PermissaoInfoBean.de(permissao);
    }

    @Transactional
    public PermissaoInfoBean atualizarPermissao(UUID id, AtualizarPermissaoBean dados) {
        PermissaoEntity permissao = permissoes.findById(id).orElseThrow(() -> naoEncontrado("Permissão"));
        if (AcessoAdministrativo.PLATAFORMA_ADMINISTRAR.equals(permissao.getCodigo())
                && dados.getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão da plataforma deve permanecer ativa.");
        }
        permissao.atualizar(dados.getDescricao(), dados.getStatus());
        registro.registrar("ATUALIZAR_PERMISSAO", "permissao", null, id);
        return PermissaoInfoBean.de(permissao);
    }

    @Transactional
    public void habilitarPermissao(UUID idAplicacao, UUID idPermissao) {
        bloquearAplicacaoEditavel(idAplicacao);
        PermissaoEntity permissao = permissoes.findById(idPermissao).orElseThrow(() -> naoEncontrado("Permissão"));
        if (permissao.getNomePlataforma() != null || permissao.getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão está indisponível para habilitação.");
        }
        var id = new AplicacaoPermissaoId(idAplicacao, idPermissao);
        if (!habilitacoes.existsById(id)) {
            var aplicacaoPermissaoEntityPreparado = new AplicacaoPermissaoEntity(id);
            habilitacoes.save(aplicacaoPermissaoEntityPreparado);
            registro.registrar("HABILITAR_PERMISSAO", "aplicacao_permissao", idAplicacao, idPermissao);
        }
    }

    @Transactional
    public void desabilitarPermissao(UUID idAplicacao, UUID idPermissao) {
        bloquearAplicacaoEditavel(idAplicacao);
        var id = new AplicacaoPermissaoId(idAplicacao, idPermissao);
        if (habilitacoes.existsById(id)) {
            permissoesPerfil.removerPermissaoDaAplicacao(idAplicacao, idPermissao);
            habilitacoes.deleteById(id);
            registro.registrar("DESABILITAR_PERMISSAO", "aplicacao_permissao", idAplicacao, idPermissao);
        }
    }

    public PaginaBean<AuditoriaInfoBean> listarAuditoria(int pagina, int tamanho) {
        return PaginaBean
                .de(auditoria.findAll(PaginaBean.requisicao(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id")))
                        .map(AuditoriaInfoBean::de));
    }

    private UUID bloquearAlteracoesDeUsuarios() {
        UUID id = acesso.idAplicacaoPlataforma();
        aplicacoes.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Aplicação administrativa"));
        if (!acesso.plataforma(SecurityContextHolder.getContext().getAuthentication())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A administração da plataforma exige acesso ativo.");
        }
        return id;
    }

    private void verificarUltimoAdministrador(UsuarioEntity usuario, StatusUsuario status, List<String> atuais,
            List<String> desejadas) {
        String administrador = AcessoAdministrativo.PLATAFORMA_ADMINISTRAR;
        if (usuario.getStatus() == StatusUsuario.ATIVO && atuais.contains(administrador)
                && (status != StatusUsuario.ATIVO || !desejadas.contains(administrador))
                && consultaUsuarios.contarOutrosAdministradores(acesso.idAplicacaoPlataforma(), administrador,
                        usuario.getId()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A plataforma deve manter um administrador ativo.");
        }
    }

    private AplicacaoEntity bloquearAplicacaoEditavel(UUID id) {
        AplicacaoEntity aplicacao = aplicacoes.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Aplicação"));
        acesso.verificarAplicacaoEditavel(id);
        return aplicacao;
    }

    private static ResponseStatusException naoEncontrado(String recurso) {
        var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.NOT_FOUND,
                recurso + " não encontrado.");
        return responseStatusExceptionPreparado;
    }
}
