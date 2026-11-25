package br.com.hubdeservicos.persistencia;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoId;
import br.com.hubdeservicos.entity.aplicacao.TipoAplicacao;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoRepository;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.perfil.PerfilEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoId;
import br.com.hubdeservicos.repository.perfil.PerfilPermissaoRepository;
import br.com.hubdeservicos.repository.perfil.PerfilRepository;
import br.com.hubdeservicos.entity.permissao.PermissaoEntity;
import br.com.hubdeservicos.entity.sessao.SessaoEntity;
import br.com.hubdeservicos.entity.sessao.StatusSessao;
import br.com.hubdeservicos.entity.sessao.TokenAtualizacaoEntity;
import br.com.hubdeservicos.repository.sessao.TokenAtualizacaoRepository;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilId;
import br.com.hubdeservicos.repository.usuario.UsuarioPerfilRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = { "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TESTE_PERSISTENCIA_POSTGRES", matches = "true")
class PersistenciaPostgresTest {

    @DynamicPropertySource
    static void configurarConexao(DynamicPropertyRegistry propriedades) {
        ConexaoPostgresTeste.configurar(propriedades);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TestEntityManager entidades;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private AplicacaoRepository aplicacoes;

    @Autowired
    private PerfilRepository perfis;

    @Autowired
    private PerfilPermissaoRepository permissoesDosPerfis;

    @Autowired
    private UsuarioPerfilRepository perfisDosUsuarios;

    @Autowired
    private TokenAtualizacaoRepository tokens;

    @BeforeEach
    void confirmarBanco() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("hub_de_servicos");
    }

    @Test
    void persisteVinculosSessaoCadeiaDeTokensEAuditoria() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        var permissaoEntityPreparado = new PermissaoEntity("TESTE_" + UUID.randomUUID(), null, StatusCadastro.ATIVO);
        PermissaoEntity permissao = entidades.persist(permissaoEntityPreparado);
        PerfilEntity perfil = criarPerfil(aplicacao);
        var usuarioAplicacaoId = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var aplicacaoPermissaoId = new AplicacaoPermissaoId(aplicacao.getId(), permissao.getId());
        var perfilPermissaoId = new PerfilPermissaoId(aplicacao.getId(), perfil.getId(), permissao.getId());
        var usuarioPerfilId = new UsuarioPerfilId(usuario.getId(), aplicacao.getId(), perfil.getId());

        var usuarioAplicacaoEntityPreparado = new UsuarioAplicacaoEntity(usuarioAplicacaoId);
        entidades.persist(usuarioAplicacaoEntityPreparado);
        var aplicacaoPermissaoEntityPreparado = new AplicacaoPermissaoEntity(aplicacaoPermissaoId);
        entidades.persist(aplicacaoPermissaoEntityPreparado);
        var perfilPermissaoEntityPreparado = new PerfilPermissaoEntity(perfilPermissaoId);
        entidades.persist(perfilPermissaoEntityPreparado);
        var usuarioPerfilEntityPreparado = new UsuarioPerfilEntity(usuarioPerfilId);
        entidades.persist(usuarioPerfilEntityPreparado);
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        TokenAtualizacaoEntity inicial = criarToken(sessao, null);
        TokenAtualizacaoEntity sucessor = criarToken(sessao, inicial.getId());

        var valor = JsonNodeFactory.instance.objectNode().put("status", "ATIVO");
        var auditoriaEntityPreparado = new AuditoriaEntity(null, null, "TESTE_PERSISTENCIA", "usuario", null, valor,
                "::1");
        AuditoriaEntity evento = entidades.persistAndFlush(auditoriaEntityPreparado);
        entidades.clear();

        assertThat(aplicacoes.findBySlug(aplicacao.getSlug())).isPresent();
        assertThat(perfis.findByIdAndIdAplicacao(perfil.getId(), aplicacao.getId())).isPresent();
        assertThat(
                perfisDosUsuarios.findById(usuarioPerfilId).orElseThrow().getUsuarioAplicacao().getUsuario().getLogin())
                .isEqualTo(usuario.getLogin());
        assertThat(permissoesDosPerfis.findById(perfilPermissaoId).orElseThrow().getAplicacaoPermissao().getPermissao()
                .getCodigo()).isEqualTo(permissao.getCodigo());
        assertThat(permissoesDosPerfis.findById(perfilPermissaoId).orElseThrow().getPerfil().getNome())
                .isEqualTo(perfil.getNome());
        TokenAtualizacaoEntity token = tokens.findByHashToken(sucessor.getHashToken()).orElseThrow();
        assertThat(token.getSessao().getUsuarioAplicacao().getId()).isEqualTo(usuarioAplicacaoId);
        assertThat(token.getAnterior().getHashToken()).isEqualTo(inicial.getHashToken());
        assertThat(token.getDataCriacao()).isNotNull();
        assertThat(token.isRevogado()).isFalse();

        AuditoriaEntity registrada = entidades.find(AuditoriaEntity.class, evento.getId());
        assertThat(registrada.getValorNovo()).isEqualTo(valor);
        assertThat(registrada.getDataRegistro()).isNotNull();
        assertThat(registrada.getIdAplicacao()).isNull();
    }

    @Test
    void consultaIdentidadeSemDistinguirCaixaERejeitaLoginDuplicado() {
        UsuarioEntity usuario = criarUsuario();
        String loginMaiusculo = usuario.getLogin().toUpperCase(Locale.ROOT);
        String emailMaiusculo = usuario.getEmail().toUpperCase(Locale.ROOT);
        assertThat(usuarios.buscarPorLogin(loginMaiusculo).orElseThrow().getId()).isEqualTo(usuario.getId());
        assertThat(usuarios.buscarPorEmail(emailMaiusculo).orElseThrow().getId()).isEqualTo(usuario.getId());

        verificarViolacao(() -> {
            var usuarioEntityPreparado = new UsuarioEntity(loginMaiusculo, "hash-teste",
                    UUID.randomUUID() + "@example.test", StatusUsuario.ATIVO);
            usuarios.saveAndFlush(usuarioEntityPreparado);
        }, "23505", "idx_usuario_login_lower");
    }

    @Test
    void rejeitaPerfilDeOutraAplicacaoMesmoComVinculoExistente() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        PerfilEntity perfilDeOutraAplicacao = criarPerfil(criarAplicacao());
        var usuarioAplicacaoIdPreparado = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var argumentoPreparado = usuarioAplicacaoIdPreparado;
        var usuarioAplicacaoEntityPreparado2 = new UsuarioAplicacaoEntity(argumentoPreparado);
        entidades.persist(usuarioAplicacaoEntityPreparado2);
        entidades.flush();

        var vinculo = new UsuarioPerfilId(usuario.getId(), aplicacao.getId(), perfilDeOutraAplicacao.getId());

        verificarViolacao(() -> {
            var usuarioPerfilEntityPreparado2 = new UsuarioPerfilEntity(vinculo);
            perfisDosUsuarios.saveAndFlush(usuarioPerfilEntityPreparado2);
        }, "23503", "fk_usuario_perfil_perfil");
    }

    @Test
    void rejeitaPermissaoNaoHabilitadaNaAplicacaoDoPerfil() {
        AplicacaoEntity aplicacao = criarAplicacao();
        AplicacaoEntity outraAplicacao = criarAplicacao();
        PerfilEntity perfil = criarPerfil(aplicacao);
        var permissaoEntityPreparado2 = new PermissaoEntity("TESTE_" + UUID.randomUUID(), null, StatusCadastro.ATIVO);
        PermissaoEntity permissao = entidades.persist(permissaoEntityPreparado2);
        var aplicacaoPermissaoIdPreparado = new AplicacaoPermissaoId(outraAplicacao.getId(), permissao.getId());
        var argumentoPreparado2 = aplicacaoPermissaoIdPreparado;
        var aplicacaoPermissaoEntityPreparado2 = new AplicacaoPermissaoEntity(argumentoPreparado2);
        entidades.persist(aplicacaoPermissaoEntityPreparado2);
        entidades.flush();

        var vinculo = new PerfilPermissaoId(aplicacao.getId(), perfil.getId(), permissao.getId());

        verificarViolacao(() -> {
            var perfilPermissaoEntityPreparado2 = new PerfilPermissaoEntity(vinculo);
            permissoesDosPerfis.saveAndFlush(perfilPermissaoEntityPreparado2);
        }, "23503", "fk_perfil_permissao_aplicacao_permissao");
    }

    @Test
    void rejeitaPredecessorDeOutraFamilia() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        var usuarioAplicacaoIdPreparado2 = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var argumentoPreparado3 = usuarioAplicacaoIdPreparado2;
        var usuarioAplicacaoEntityPreparado3 = new UsuarioAplicacaoEntity(argumentoPreparado3);
        entidades.persist(usuarioAplicacaoEntityPreparado3);
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        SessaoEntity outraSessao = criarSessao(usuario, aplicacao);
        TokenAtualizacaoEntity predecessor = criarToken(outraSessao, null);
        entidades.flush();

        var token = new TokenAtualizacaoEntity(sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(),
                predecessor.getId(), sessao.getExpiracaoAbsoluta());

        verificarViolacao(() -> tokens.saveAndFlush(token), "23503", "fk_token_anterior");
    }

    @Test
    void rejeitaSegundaRaizDaMesmaFamilia() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        var usuarioAplicacaoIdPreparado3 = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var argumentoPreparado4 = usuarioAplicacaoIdPreparado3;
        var usuarioAplicacaoEntityPreparado4 = new UsuarioAplicacaoEntity(argumentoPreparado4);
        entidades.persist(usuarioAplicacaoEntityPreparado4);
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        criarToken(sessao, null);
        entidades.flush();

        var segundaRaiz = new TokenAtualizacaoEntity(sessao.getId(), sessao.getIdFamilia(),
                UUID.randomUUID().toString(), null, sessao.getExpiracaoAbsoluta());

        verificarViolacao(() -> tokens.saveAndFlush(segundaRaiz), "23505", "uk_token_raiz_familia");
    }

    @Test
    void rejeitaTokenQueReferenciaSeuProprioId() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        var usuarioAplicacaoIdPreparado4 = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var argumentoPreparado5 = usuarioAplicacaoIdPreparado4;
        var usuarioAplicacaoEntityPreparado5 = new UsuarioAplicacaoEntity(argumentoPreparado5);
        entidades.persist(usuarioAplicacaoEntityPreparado5);
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        UUID id = UUID.randomUUID();
        entidades.flush();

        verificarViolacao(() -> jdbc.update("""
                insert into tokens_atualizacao (id, id_sessao, id_familia, hash_token, id_anterior, expiracao_absoluta)
                values (?, ?, ?, ?, ?, ?)
                """, id, sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(), id,
                sessao.getExpiracaoAbsoluta().atOffset(ZoneOffset.UTC)), "23514", "ck_token_sem_autorreferencia");
    }

    private UsuarioEntity criarUsuario() {
        String identidade = UUID.randomUUID().toString();
        var usuarioEntityPreparado2 = new UsuarioEntity(identidade, "hash-teste", identidade + "@example.test",
                StatusUsuario.ATIVO);
        return entidades.persist(usuarioEntityPreparado2);
    }

    private AplicacaoEntity criarAplicacao() {
        var aplicacaoEntityPreparado = new AplicacaoEntity("Aplicação de teste", UUID.randomUUID().toString(),
                TipoAplicacao.PUBLICO, null, StatusCadastro.ATIVO);
        return entidades.persist(aplicacaoEntityPreparado);
    }

    private PerfilEntity criarPerfil(AplicacaoEntity aplicacao) {
        var perfilEntityPreparado = new PerfilEntity(aplicacao.getId(), "Perfil de teste", null, StatusCadastro.ATIVO);
        return entidades.persist(perfilEntityPreparado);
    }

    private SessaoEntity criarSessao(UsuarioEntity usuario, AplicacaoEntity aplicacao) {
        Instant agora = Instant.now();
        var sessaoEntityPreparado = new SessaoEntity(usuario.getId(), aplicacao.getId(), UUID.randomUUID(),
                StatusSessao.ATIVO, agora.plusSeconds(3600), agora.plusSeconds(1800), agora);
        return entidades.persist(sessaoEntityPreparado);
    }

    private TokenAtualizacaoEntity criarToken(SessaoEntity sessao, UUID idAnterior) {
        var tokenAtualizacaoEntityPreparado = new TokenAtualizacaoEntity(sessao.getId(), sessao.getIdFamilia(),
                UUID.randomUUID().toString(), idAnterior, sessao.getExpiracaoAbsoluta());
        return entidades.persist(tokenAtualizacaoEntityPreparado);
    }

    private void verificarViolacao(Runnable operacao, String codigo, String restricao) {
        assertThatThrownBy(operacao::run).isInstanceOf(DataIntegrityViolationException.class)
                .hasRootCauseInstanceOf(SQLException.class).rootCause().satisfies(causa -> {
                    assertThat(((SQLException) causa).getSQLState()).isEqualTo(codigo);
                    assertThat(causa).hasMessageContaining(restricao);
                });
    }
}
