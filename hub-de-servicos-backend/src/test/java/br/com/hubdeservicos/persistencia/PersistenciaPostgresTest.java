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

@DataJpaTest(showSql = false, properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
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
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("hub_de_servicos");
    }

    @Test
    void persisteVinculosSessaoCadeiaDeTokensEAuditoria() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        PermissaoEntity permissao = entidades.persist(new PermissaoEntity(
                "TESTE_" + UUID.randomUUID(), null, StatusCadastro.ATIVO));
        PerfilEntity perfil = criarPerfil(aplicacao);
        var usuarioAplicacaoId = new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId());
        var aplicacaoPermissaoId = new AplicacaoPermissaoId(aplicacao.getId(), permissao.getId());
        var perfilPermissaoId = new PerfilPermissaoId(aplicacao.getId(), perfil.getId(), permissao.getId());
        var usuarioPerfilId = new UsuarioPerfilId(usuario.getId(), aplicacao.getId(), perfil.getId());

        entidades.persist(new UsuarioAplicacaoEntity(usuarioAplicacaoId));
        entidades.persist(new AplicacaoPermissaoEntity(aplicacaoPermissaoId));
        entidades.persist(new PerfilPermissaoEntity(perfilPermissaoId));
        entidades.persist(new UsuarioPerfilEntity(usuarioPerfilId));
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        TokenAtualizacaoEntity inicial = criarToken(sessao, null);
        TokenAtualizacaoEntity sucessor = criarToken(sessao, inicial.getId());

        var valor = JsonNodeFactory.instance.objectNode().put("status", "ATIVO");
        AuditoriaEntity evento = entidades.persistAndFlush(new AuditoriaEntity(
                null, null, "TESTE_PERSISTENCIA", "usuario", null, valor, "::1"));
        entidades.clear();

        assertThat(aplicacoes.findBySlug(aplicacao.getSlug())).isPresent();
        assertThat(perfis.findByIdAndIdAplicacao(perfil.getId(), aplicacao.getId())).isPresent();
        assertThat(perfisDosUsuarios.findById(usuarioPerfilId).orElseThrow()
                .getUsuarioAplicacao().getUsuario().getLogin()).isEqualTo(usuario.getLogin());
        assertThat(permissoesDosPerfis.findById(perfilPermissaoId).orElseThrow()
                .getAplicacaoPermissao().getPermissao().getCodigo()).isEqualTo(permissao.getCodigo());
        assertThat(permissoesDosPerfis.findById(perfilPermissaoId).orElseThrow()
                .getPerfil().getNome()).isEqualTo(perfil.getNome());
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

        verificarViolacao(() -> usuarios.saveAndFlush(new UsuarioEntity(
                loginMaiusculo, "hash-teste", UUID.randomUUID() + "@example.test", StatusUsuario.ATIVO)),
                "23505", "idx_usuario_login_lower");
    }

    @Test
    void rejeitaPerfilDeOutraAplicacaoMesmoComVinculoExistente() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        PerfilEntity perfilDeOutraAplicacao = criarPerfil(criarAplicacao());
        entidades.persist(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId())));
        entidades.flush();

        var vinculo = new UsuarioPerfilId(usuario.getId(), aplicacao.getId(), perfilDeOutraAplicacao.getId());

        verificarViolacao(() -> perfisDosUsuarios.saveAndFlush(new UsuarioPerfilEntity(vinculo)),
                "23503", "fk_usuario_perfil_perfil");
    }

    @Test
    void rejeitaPermissaoNaoHabilitadaNaAplicacaoDoPerfil() {
        AplicacaoEntity aplicacao = criarAplicacao();
        AplicacaoEntity outraAplicacao = criarAplicacao();
        PerfilEntity perfil = criarPerfil(aplicacao);
        PermissaoEntity permissao = entidades.persist(new PermissaoEntity(
                "TESTE_" + UUID.randomUUID(), null, StatusCadastro.ATIVO));
        entidades.persist(new AplicacaoPermissaoEntity(new AplicacaoPermissaoId(outraAplicacao.getId(), permissao.getId())));
        entidades.flush();

        var vinculo = new PerfilPermissaoId(aplicacao.getId(), perfil.getId(), permissao.getId());

        verificarViolacao(() -> permissoesDosPerfis.saveAndFlush(new PerfilPermissaoEntity(vinculo)),
                "23503", "fk_perfil_permissao_aplicacao_permissao");
    }

    @Test
    void rejeitaPredecessorDeOutraFamilia() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        entidades.persist(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId())));
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        SessaoEntity outraSessao = criarSessao(usuario, aplicacao);
        TokenAtualizacaoEntity predecessor = criarToken(outraSessao, null);
        entidades.flush();

        var token = new TokenAtualizacaoEntity(
                sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(),
                predecessor.getId(), sessao.getExpiracaoAbsoluta());

        verificarViolacao(() -> tokens.saveAndFlush(token), "23503", "fk_token_anterior");
    }

    @Test
    void rejeitaSegundaRaizDaMesmaFamilia() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        entidades.persist(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId())));
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        criarToken(sessao, null);
        entidades.flush();

        var segundaRaiz = new TokenAtualizacaoEntity(
                sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(),
                null, sessao.getExpiracaoAbsoluta());

        verificarViolacao(() -> tokens.saveAndFlush(segundaRaiz), "23505", "uk_token_raiz_familia");
    }

    @Test
    void rejeitaTokenQueReferenciaSeuProprioId() {
        UsuarioEntity usuario = criarUsuario();
        AplicacaoEntity aplicacao = criarAplicacao();
        entidades.persist(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId())));
        SessaoEntity sessao = criarSessao(usuario, aplicacao);
        UUID id = UUID.randomUUID();
        entidades.flush();

        verificarViolacao(() -> jdbc.update("""
                insert into tokens_atualizacao (id, id_sessao, id_familia, hash_token, id_anterior, expiracao_absoluta)
                values (?, ?, ?, ?, ?, ?)
                """, id, sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(), id,
                sessao.getExpiracaoAbsoluta().atOffset(ZoneOffset.UTC)),
                "23514", "ck_token_sem_autorreferencia");
    }

    private UsuarioEntity criarUsuario() {
        String identidade = UUID.randomUUID().toString();
        return entidades.persist(new UsuarioEntity(identidade, "hash-teste", identidade + "@example.test",
                StatusUsuario.ATIVO));
    }

    private AplicacaoEntity criarAplicacao() {
        return entidades.persist(new AplicacaoEntity("Aplicação de teste", UUID.randomUUID().toString(),
                TipoAplicacao.PUBLICO, null, StatusCadastro.ATIVO));
    }

    private PerfilEntity criarPerfil(AplicacaoEntity aplicacao) {
        return entidades.persist(new PerfilEntity(aplicacao.getId(), "Perfil de teste", null, StatusCadastro.ATIVO));
    }

    private SessaoEntity criarSessao(UsuarioEntity usuario, AplicacaoEntity aplicacao) {
        Instant agora = Instant.now();
        return entidades.persist(new SessaoEntity(usuario.getId(), aplicacao.getId(), UUID.randomUUID(),
                StatusSessao.ATIVO, agora.plusSeconds(3600), agora.plusSeconds(1800), agora));
    }

    private TokenAtualizacaoEntity criarToken(SessaoEntity sessao, UUID idAnterior) {
        return entidades.persist(new TokenAtualizacaoEntity(
                sessao.getId(), sessao.getIdFamilia(), UUID.randomUUID().toString(),
                idAnterior, sessao.getExpiracaoAbsoluta()));
    }

    private void verificarViolacao(Runnable operacao, String codigo, String restricao) {
        assertThatThrownBy(operacao::run)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasRootCauseInstanceOf(SQLException.class)
                .rootCause()
                .satisfies(causa -> {
                    assertThat(((SQLException) causa).getSQLState()).isEqualTo(codigo);
                    assertThat(causa).hasMessageContaining(restricao);
                });
    }
}
