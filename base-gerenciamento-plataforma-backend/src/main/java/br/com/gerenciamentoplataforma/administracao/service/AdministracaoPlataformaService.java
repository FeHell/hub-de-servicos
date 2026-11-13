package br.com.gerenciamentoplataforma.administracao.service;

import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AplicacaoInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AtualizarAplicacao;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AtualizarPermissao;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AtualizarUsuario;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.AuditoriaInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.DashboardUsuarios;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovaAplicacao;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovaPermissao;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovaPermissaoPlataforma;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovoUsuario;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.Pagina;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.PermissaoInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.PermissaoPlataformaInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.PermissaoUsuario;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.TipoAcessoUsuario;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.TotalUsuariosMes;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.TotalUsuariosPermissao;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.TotalUsuariosStatus;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.TotalUsuariosTipo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.UsuarioInfo;
import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.UsuarioListagem;
import br.com.gerenciamentoplataforma.administracao.repository.ConsultaUsuariosRepository.PermissaoDoUsuario;
import br.com.gerenciamentoplataforma.administracao.repository.ConsultaUsuariosRepository;
import br.com.gerenciamentoplataforma.aplicacao.entity.AplicacaoEntity;
import br.com.gerenciamentoplataforma.aplicacao.entity.AplicacaoPermissaoEntity;
import br.com.gerenciamentoplataforma.aplicacao.entity.AplicacaoPermissaoId;
import br.com.gerenciamentoplataforma.aplicacao.repository.AplicacaoPermissaoRepository;
import br.com.gerenciamentoplataforma.aplicacao.repository.AplicacaoRepository;
import br.com.gerenciamentoplataforma.auditoria.repository.AuditoriaRepository;
import br.com.gerenciamentoplataforma.common.StatusCadastro;
import br.com.gerenciamentoplataforma.perfil.repository.PerfilPermissaoRepository;
import br.com.gerenciamentoplataforma.permissao.entity.PermissaoEntity;
import br.com.gerenciamentoplataforma.permissao.repository.PermissaoRepository;
import br.com.gerenciamentoplataforma.sessao.service.RevogacaoSessaoService;
import br.com.gerenciamentoplataforma.usuario.entity.StatusUsuario;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioEntity;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioRepository;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioPermissaoPlataformaRepository;

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
                                          ConsultaUsuariosRepository consultaUsuarios,
                                          PermissaoRepository permissoes, UsuarioPermissaoPlataformaRepository atribuicoes,
                                          PermissoesPlataformaService permissoesPlataforma,
                                          AplicacaoPermissaoRepository habilitacoes,
                                          PerfilPermissaoRepository permissoesPerfil, AuditoriaRepository auditoria,
                                          RegistroAdministrativo registro, AcessoAdministrativo acesso,
                                          SenhasAdministrativas senhas, RevogacaoSessaoService revogacao) {
        this.aplicacoes = aplicacoes;
        this.usuarios = usuarios;
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

    public Pagina<AplicacaoInfo> listarAplicacoes(int pagina, int tamanho) {
        return Pagina.de(aplicacoes.findAll(Pagina.requisicao(pagina, tamanho, Sort.by("slug"))).map(AplicacaoInfo::de));
    }

    public AplicacaoInfo consultarAplicacao(UUID id) {
        return AplicacaoInfo.de(aplicacoes.findById(id).orElseThrow(() -> naoEncontrado("Aplicação")));
    }

    @Transactional
    public AplicacaoInfo criarAplicacao(NovaAplicacao dados) {
        var aplicacao = aplicacoes.saveAndFlush(new AplicacaoEntity(dados.nome().strip(), dados.slug(), dados.tipo(),
                dados.descricao(), StatusCadastro.ATIVO));
        registro.registrar("CRIAR_APLICACAO", "aplicacao", aplicacao.getId(), aplicacao.getId());
        return AplicacaoInfo.de(aplicacao);
    }

    @Transactional
    public AplicacaoInfo atualizarAplicacao(UUID id, AtualizarAplicacao dados) {
        AplicacaoEntity aplicacao = bloquearAplicacaoEditavel(id);
        boolean desativou = aplicacao.getStatus() == StatusCadastro.ATIVO && dados.status() == StatusCadastro.INATIVO;
        aplicacao.atualizar(dados.nome().strip(), dados.descricao(), dados.status());
        if (desativou) {
            revogacao.revogarDaAplicacao(id);
        }
        registro.registrar("ATUALIZAR_APLICACAO", "aplicacao", id, id);
        return AplicacaoInfo.de(aplicacao);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Pagina<UsuarioListagem> listarUsuarios(int pagina, int tamanho, String login, String email,
                                                StatusUsuario status, TipoAcessoUsuario tipo) {
        UUID administracao = acesso.idAplicacaoPlataforma();
        var encontrados = consultaUsuarios.pesquisar(login.strip(), email.strip(), status,
                tipo == null ? null : tipo.name(), administracao, AcessoAdministrativo.PLATAFORMA_ADMINISTRAR,
                AcessoAdministrativo.APLICACAO_ADMINISTRAR, Pagina.requisicao(pagina, tamanho, Sort.by("login")));
        if (encontrados.isEmpty()) {
            return new Pagina<>(List.of(), encontrados.getNumber(), encontrados.getSize(), encontrados.getTotalElements());
        }
        var ids = encontrados.stream().map(UsuarioEntity::getId).toList();
        var porUsuario = permissoesEfetivas(administracao, ids)
                .collect(Collectors.groupingBy(PermissaoDoUsuario::getIdUsuario));
        var atribuicoesPorUsuario = atribuicoes.listarAtribuicoes(ids).stream()
                .collect(Collectors.groupingBy(UsuarioPermissaoPlataformaRepository.Atribuicao::getIdUsuario,
                        Collectors.mapping(UsuarioPermissaoPlataformaRepository.Atribuicao::getCodigo, Collectors.toList())));
        return Pagina.de(encontrados.map(u -> comAcessos(u, administracao,
                porUsuario.getOrDefault(u.getId(), List.of()), atribuicoesPorUsuario.getOrDefault(u.getId(), List.of()))));
    }

    private Stream<PermissaoDoUsuario> permissoesEfetivas(UUID administracao, List<UUID> usuarios) {
        return Stream.concat(consultaUsuarios.permissoesDaPlataforma(administracao, usuarios).stream(),
                consultaUsuarios.permissoes(administracao, usuarios).stream());
    }

    private static UsuarioListagem comAcessos(UsuarioEntity usuario, UUID administracao, List<PermissaoDoUsuario> permissoes,
                                             List<String> atribuicoes) {
        return UsuarioListagem.de(usuario, tipoAcesso(administracao, permissoes), permissoes.stream()
                .map(p -> new PermissaoUsuario(p.getAplicacao(), p.getCodigo())).toList(), atribuicoes);
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
    public DashboardUsuarios dashboardUsuarios(int meses) {
        if (meses < 0 || meses > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe de 0 a 24 meses.");
        }
        Instant agora = Instant.now();
        int mesesDaSerie = meses == 0 ? 12 : meses;
        YearMonth inicioSerie = YearMonth.from(agora.atOffset(ZoneOffset.UTC)).minusMonths(mesesDaSerie - 1L);
        Instant primeiroMes = inicioSerie.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant inicio = meses == 0 ? Instant.parse("0001-01-01T00:00:00Z") : primeiroMes;
        Instant limiteRecentes = agora.minus(30, ChronoUnit.DAYS);
        long recentes = consultaUsuarios.countByDataCriacaoBetween(inicio.isAfter(limiteRecentes) ? inicio : limiteRecentes, agora);
        var status = consultaUsuarios.contarPorStatus(inicio, agora).stream()
                .collect(Collectors.toMap(ConsultaUsuariosRepository.ContagemStatus::getStatus,
                        ConsultaUsuariosRepository.ContagemStatus::getTotal));
        var contagemMeses = consultaUsuarios.contarPorMes(primeiroMes, agora).stream()
                .collect(Collectors.toMap(ConsultaUsuariosRepository.ContagemMes::getMes,
                        ConsultaUsuariosRepository.ContagemMes::getTotal));
        var tipos = consultaUsuarios.contarPorTipo(inicio, agora, acesso.idAplicacaoPlataforma(),
                AcessoAdministrativo.PLATAFORMA_ADMINISTRAR, AcessoAdministrativo.APLICACAO_ADMINISTRAR).stream()
                .collect(Collectors.toMap(t -> TipoAcessoUsuario.valueOf(t.getTipo()),
                        ConsultaUsuariosRepository.ContagemTipo::getTotal));
        var porStatus = Arrays.stream(StatusUsuario.values())
                .map(s -> new TotalUsuariosStatus(s, status.getOrDefault(s, 0L))).toList();
        var porTipo = Arrays.stream(TipoAcessoUsuario.values())
                .map(t -> new TotalUsuariosTipo(t, tipos.getOrDefault(t, 0L))).toList();
        UUID administracao = acesso.idAplicacaoPlataforma();
        var porPermissao = Stream.concat(consultaUsuarios.contarPorPermissao(administracao, inicio, agora).stream(),
                        consultaUsuarios.contarPorPermissaoDaPlataforma(administracao, inicio, agora).stream())
                .map(p -> new TotalUsuariosPermissao(p.getAplicacao(), p.getCodigo(), p.getTotal()))
                .sorted(Comparator.comparingLong(TotalUsuariosPermissao::total).reversed()
                        .thenComparing(TotalUsuariosPermissao::aplicacao).thenComparing(TotalUsuariosPermissao::codigo))
                .toList();
        var porMes = IntStream.range(0, mesesDaSerie).mapToObj(i -> inicioSerie.plusMonths(i).toString())
                .map(m -> new TotalUsuariosMes(m, contagemMeses.getOrDefault(m, 0L))).toList();
        return new DashboardUsuarios(porStatus.stream().mapToLong(TotalUsuariosStatus::total).sum(),
                recentes, porStatus, porTipo, porPermissao, porMes);
    }

    public UsuarioListagem consultarUsuario(UUID id) {
        UsuarioEntity usuario = usuarios.findById(id).orElseThrow(() -> naoEncontrado("Usuário"));
        UUID administracao = acesso.idAplicacaoPlataforma();
        return comAcessos(usuario, administracao, permissoesEfetivas(administracao, List.of(id)).toList(),
                permissoesPlataforma.consultar(id));
    }

    @Transactional
    public UsuarioInfo criarUsuario(NovoUsuario dados) {
        StatusUsuario status = dados.status() == null ? StatusUsuario.ATIVO : dados.status();
        String hash = senhas.gerarHash(dados.senha());
        UUID administracao = bloquearAlteracoesDeUsuarios();
        var usuario = usuarios.saveAndFlush(new UsuarioEntity(dados.login().strip(), hash, dados.email().strip(), status));
        registro.registrar("CRIAR_USUARIO", "usuario", null, usuario.getId());
        var codigos = dados.permissoesPlataforma() == null ? List.<String>of() : dados.permissoesPlataforma();
        return UsuarioInfo.de(usuario, permissoesPlataforma.substituir(usuario.getId(), administracao, codigos));
    }

    @Transactional
    public UsuarioInfo atualizarUsuario(UUID id, AtualizarUsuario dados) {
        UUID administracao = bloquearAlteracoesDeUsuarios();
        if (id.equals(registro.idOperador()) && dados.status() != StatusUsuario.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O administrador deve manter a própria conta ativa.");
        }
        UsuarioEntity usuario = usuarios.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Usuário"));
        boolean desativou = usuario.getStatus() == StatusUsuario.ATIVO && dados.status() != StatusUsuario.ATIVO;
        var atuais = permissoesPlataforma.consultar(id);
        var desejadas = dados.permissoesPlataforma();
        verificarUltimoAdministrador(usuario, dados.status(), atuais, desejadas == null ? atuais : desejadas);
        usuario.atualizar(dados.login().strip(), dados.email().strip(), dados.status());
        var codigos = desejadas == null ? atuais : permissoesPlataforma.substituir(id, administracao, desejadas);
        if (desativou) {
            revogacao.revogarDoUsuario(id);
        }
        registro.registrar("ATUALIZAR_USUARIO", "usuario", null, id);
        return UsuarioInfo.de(usuario, codigos);
    }

    @Transactional
    public void desativarUsuario(UUID id) {
        bloquearAlteracoesDeUsuarios();
        if (id.equals(registro.idOperador())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O administrador deve manter a própria conta ativa.");
        }
        UsuarioEntity usuario = usuarios.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Usuário"));
        if (usuario.getStatus() == StatusUsuario.INATIVO) return;
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

    public List<PermissaoPlataformaInfo> listarPermissoesPlataforma() {
        return permissoes.findByNomePlataformaIsNotNullOrderByNomePlataformaAscCodigoAsc().stream()
                .map(PermissaoPlataformaInfo::de).toList();
    }

    @Transactional
    public PermissaoPlataformaInfo criarPermissaoPlataforma(NovaPermissaoPlataforma dados) {
        var permissao = permissoes.saveAndFlush(PermissaoEntity.daPlataforma(dados.codigo(), dados.nome().strip(),
                dados.descricao()));
        registro.registrar("CRIAR_PERMISSAO_PLATAFORMA", "permissao", null, permissao.getId());
        return PermissaoPlataformaInfo.de(permissao);
    }

    public Pagina<PermissaoInfo> listarPermissoes(int pagina, int tamanho) {
        return Pagina.de(permissoes.findAll(Pagina.requisicao(pagina, tamanho, Sort.by("codigo")))
                .map(PermissaoInfo::de));
    }

    public PermissaoInfo consultarPermissao(UUID id) {
        return PermissaoInfo.de(permissoes.findById(id).orElseThrow(() -> naoEncontrado("Permissão")));
    }

    @Transactional
    public PermissaoInfo criarPermissao(NovaPermissao dados) {
        if (AcessoAdministrativo.PLATAFORMA_ADMINISTRAR.equals(dados.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Código reservado à administração da plataforma.");
        }
        var permissao = permissoes.saveAndFlush(new PermissaoEntity(dados.codigo(), dados.descricao(), StatusCadastro.ATIVO));
        registro.registrar("CRIAR_PERMISSAO", "permissao", null, permissao.getId());
        return PermissaoInfo.de(permissao);
    }

    @Transactional
    public PermissaoInfo atualizarPermissao(UUID id, AtualizarPermissao dados) {
        PermissaoEntity permissao = permissoes.findById(id).orElseThrow(() -> naoEncontrado("Permissão"));
        if (AcessoAdministrativo.PLATAFORMA_ADMINISTRAR.equals(permissao.getCodigo())
                && dados.status() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A permissão da plataforma deve permanecer ativa.");
        }
        permissao.atualizar(dados.descricao(), dados.status());
        registro.registrar("ATUALIZAR_PERMISSAO", "permissao", null, id);
        return PermissaoInfo.de(permissao);
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
            habilitacoes.save(new AplicacaoPermissaoEntity(id));
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

    public Pagina<AuditoriaInfo> listarAuditoria(int pagina, int tamanho) {
        return Pagina.de(auditoria.findAll(Pagina.requisicao(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id")))
                .map(AuditoriaInfo::de));
    }

    private UUID bloquearAlteracoesDeUsuarios() {
        UUID id = acesso.idAplicacaoPlataforma();
        aplicacoes.bloquearPorId(id).orElseThrow(() -> naoEncontrado("Aplicação administrativa"));
        if (!acesso.plataforma(SecurityContextHolder.getContext().getAuthentication())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A administração da plataforma exige acesso ativo.");
        }
        return id;
    }

    private void verificarUltimoAdministrador(UsuarioEntity usuario, StatusUsuario status,
                                              List<String> atuais, List<String> desejadas) {
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
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " não encontrado.");
    }
}
