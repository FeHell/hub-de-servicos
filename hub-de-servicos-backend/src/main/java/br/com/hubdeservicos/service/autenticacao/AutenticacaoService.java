package br.com.hubdeservicos.service.autenticacao;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoRepository;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.dto.autenticacao.LoginEntrada;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;
import br.com.hubdeservicos.dto.autenticacao.UsuarioAutenticado;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.sessao.SessaoEntity;
import br.com.hubdeservicos.entity.sessao.StatusSessao;
import br.com.hubdeservicos.entity.sessao.TokenAtualizacaoEntity;
import br.com.hubdeservicos.repository.sessao.SessaoRepository;
import br.com.hubdeservicos.repository.sessao.TokenAtualizacaoRepository;
import br.com.hubdeservicos.service.sessao.RevogacaoSessaoService;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.repository.usuario.UsuarioAplicacaoRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private static final Duration VALIDADE_ABSOLUTA = Duration.ofDays(30);
    private static final Duration LIMITE_INATIVIDADE = Duration.ofDays(7);

    private final UsuarioRepository usuarios;
    private final AplicacaoRepository aplicacoes;
    private final UsuarioAplicacaoRepository vinculos;
    private final SessaoRepository sessoes;
    private final TokenAtualizacaoRepository tokens;
    private final RevogacaoSessaoService revogacao;
    private final AuditoriaRepository auditoria;
    private final PasswordEncoder senhas;
    private final SegredosAutenticacao segredos;
    private final TokenJwt jwt;
    private final Clock relogio;
    private final String hashAusente;

    AutenticacaoService(UsuarioRepository usuarios, AplicacaoRepository aplicacoes,
                        UsuarioAplicacaoRepository vinculos, SessaoRepository sessoes, RevogacaoSessaoService revogacao,
                        TokenAtualizacaoRepository tokens, AuditoriaRepository auditoria,
                        PasswordEncoder senhas, SegredosAutenticacao segredos, TokenJwt jwt, Clock relogio) {
        this.usuarios = usuarios;
        this.aplicacoes = aplicacoes;
        this.vinculos = vinculos;
        this.sessoes = sessoes;
        this.tokens = tokens;
        this.revogacao = revogacao;
        this.auditoria = auditoria;
        this.senhas = senhas;
        this.segredos = segredos;
        this.jwt = jwt;
        this.relogio = relogio;
        this.hashAusente = senhas.encode(segredos.gerarTokenAtualizacao());
    }

    @Transactional
    public TokensAutenticacao login(String slug, LoginEntrada entrada, String segredoCliente, String ip) {
        var encontrado = usuarios.buscarLoginParaAutenticacao(entrada.login());
        if (entrada.senha().getBytes(StandardCharsets.UTF_8).length > 72
                || !senhas.matches(entrada.senha(), encontrado.map(UsuarioEntity::getSenhaHash).orElse(hashAusente))) {
            throw new AutenticacaoNegadaException();
        }
        UsuarioEntity usuario = encontrado.orElseThrow(AutenticacaoNegadaException::new);
        AplicacaoEntity aplicacao = autenticarCliente(aplicacoes.buscarSlugParaLogin(slug), segredoCliente);
        validarVinculo(usuario, aplicacao.getId());
        Instant agora = relogio.instant();
        SessaoEntity sessao = sessoes.save(new SessaoEntity(usuario.getId(), aplicacao.getId(), UUID.randomUUID(),
                StatusSessao.ATIVO, agora.plus(VALIDADE_ABSOLUTA), agora.plus(LIMITE_INATIVIDADE), agora));
        TokensAutenticacao resposta = emitirTokens(sessao, slug, null, agora);
        registrar("LOGIN", sessao, ip);
        return resposta;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, noRollbackFor = AutenticacaoNegadaException.class)
    public TokensAutenticacao renovar(String slug, String tokenAtualizacao, String segredoCliente, String ip) {
        AplicacaoEntity aplicacao = autenticarCliente(aplicacoes.findBySlug(slug), segredoCliente);
        String hash = segredos.hash(tokenAtualizacao);
        UUID idSessao = tokens.buscarIdSessaoPorHash(hash).orElseThrow(AutenticacaoNegadaException::new);
        // O bloqueio da sessão serializa renovação e logout. O token é carregado depois do bloqueio.
        SessaoEntity sessao = sessoes.bloquearPorId(idSessao).orElseThrow(AutenticacaoNegadaException::new);
        if (!sessao.getIdAplicacao().equals(aplicacao.getId())) {
            throw new AutenticacaoNegadaException();
        }
        Instant agora = relogio.instant();
        if (!sessao.estaAtivaEm(agora)) {
            if (sessao.getStatus() == StatusSessao.ATIVO) {
                sessao.expirar();
            }
            throw new AutenticacaoNegadaException();
        }
        TokenAtualizacaoEntity token = tokens.findByHashToken(hash).orElseThrow(AutenticacaoNegadaException::new);
        if (token.getDataUtilizacao() != null) {
            revogacao.revogar(sessao, agora);
            registrar("REUTILIZACAO_REFRESH_TOKEN", sessao, ip);
            throw new AutenticacaoNegadaException();
        }
        if (token.isRevogado() || !token.getExpiracaoAbsoluta().isAfter(agora)) {
            throw new AutenticacaoNegadaException();
        }

        UsuarioEntity usuario = usuarios.findById(sessao.getIdUsuario()).orElseThrow(AutenticacaoNegadaException::new);
        validarVinculo(usuario, aplicacao.getId());
        Instant expiracao = menor(sessao.getExpiracaoAbsoluta(), agora.plus(LIMITE_INATIVIDADE));
        if (!expiracao.truncatedTo(ChronoUnit.SECONDS).isAfter(agora)) {
            throw new AutenticacaoNegadaException();
        }
        token.registrarUtilizacao(agora);
        sessao.registrarRenovacao(agora, expiracao);
        return emitirTokens(sessao, slug, token.getId(), agora);
    }

    @Transactional(readOnly = true, noRollbackFor = AutenticacaoNegadaException.class)
    public UsuarioAutenticado consultarUsuario(String slug, IdentidadeJwt identidade) {
        SessaoEntity sessao = sessoes.findById(identidade.idSessao()).orElseThrow(AutenticacaoNegadaException::new);
        UsuarioEntity usuario = validarSessao(slug, identidade, sessao, relogio.instant());
        return new UsuarioAutenticado(usuario.getId(), usuario.getLogin(), usuario.getEmail(),
                sessao.getIdAplicacao(), slug);
    }

    @Transactional
    public void logout(String slug, IdentidadeJwt identidade, String ip) {
        SessaoEntity sessao = sessoes.bloquearPorId(identidade.idSessao()).orElseThrow(AutenticacaoNegadaException::new);
        Instant agora = relogio.instant();
        validarSessao(slug, identidade, sessao, agora);
        revogacao.revogar(sessao, agora);
        registrar("LOGOUT", sessao, ip);
    }

    @Transactional
    public void logoutNavegador(String slug, String tokenAtualizacao, String ip) {
        var aplicacao = aplicacoes.findBySlug(slug).filter(a -> segredos.autorizaCliente(a, null));
        if (aplicacao.isEmpty()) return;
        String hash = segredos.hash(tokenAtualizacao);
        var sessao = tokens.buscarIdSessaoPorHash(hash).flatMap(sessoes::bloquearPorId);
        Instant agora = relogio.instant();
        if (sessao.isEmpty() || !sessao.get().getIdAplicacao().equals(aplicacao.get().getId())
                || !sessao.get().estaAtivaEm(agora)) return;
        var token = tokens.findByHashToken(hash);
        if (token.isEmpty() || token.get().isRevogado() || token.get().getDataUtilizacao() != null) return;
        revogacao.revogar(sessao.get(), agora);
        registrar("LOGOUT", sessao.get(), ip);
    }

    private UsuarioEntity validarSessao(String slug, IdentidadeJwt identidade, SessaoEntity sessao, Instant agora) {
        if (!slug.equals(identidade.audiencia())
                || !sessao.getIdAplicacao().equals(identidade.idAplicacao())
                || !sessao.getIdUsuario().equals(identidade.idUsuario())
                || !sessao.estaAtivaEm(agora)) {
            throw new AutenticacaoNegadaException();
        }
        var aplicacao = aplicacoes.findBySlug(slug).filter(a -> a.getId().equals(sessao.getIdAplicacao()))
                .filter(a -> a.getStatus() == StatusCadastro.ATIVO)
                .orElseThrow(AutenticacaoNegadaException::new);
        var usuario = usuarios.findById(sessao.getIdUsuario()).orElseThrow(AutenticacaoNegadaException::new);
        validarVinculo(usuario, aplicacao.getId());
        return usuario;
    }

    private AplicacaoEntity autenticarCliente(Optional<AplicacaoEntity> aplicacao, String segredo) {
        return aplicacao
                .filter(a -> a.getStatus() == StatusCadastro.ATIVO && segredos.autorizaCliente(a, segredo))
                .orElseThrow(AutenticacaoNegadaException::new);
    }

    private void validarVinculo(UsuarioEntity usuario, UUID idAplicacao) {
        if (usuario.getStatus() != StatusUsuario.ATIVO
                || !vinculos.existsByIdAndAtivoTrue(new UsuarioAplicacaoId(usuario.getId(), idAplicacao))) {
            throw new AutenticacaoNegadaException();
        }
    }

    private TokensAutenticacao emitirTokens(SessaoEntity sessao, String slug, UUID idAnterior, Instant agora) {
        Instant expiracao = menor(agora.plus(TokenJwt.VALIDADE),
                menor(sessao.getExpiracaoAbsoluta(), sessao.getExpiracaoInatividade())).truncatedTo(ChronoUnit.SECONDS);
        String acesso = jwt.emitir(sessao, slug, agora, expiracao);
        String atualizacao = segredos.gerarTokenAtualizacao();
        tokens.save(new TokenAtualizacaoEntity(sessao.getId(), sessao.getIdFamilia(), segredos.hash(atualizacao),
                idAnterior, sessao.getExpiracaoAbsoluta()));
        return new TokensAutenticacao(acesso, atualizacao, expiracao);
    }

    private void registrar(String acao, SessaoEntity sessao, String ip) {
        var dados = JsonNodeFactory.instance.objectNode().put("idSessao", sessao.getId().toString());
        auditoria.save(new AuditoriaEntity(sessao.getIdUsuario(), sessao.getIdAplicacao(), acao,
                "sessoes", null, dados, ip != null && ip.length() <= 45 ? ip : null));
    }

    private static Instant menor(Instant primeira, Instant segunda) {
        return primeira.isBefore(segunda) ? primeira : segunda;
    }
}
