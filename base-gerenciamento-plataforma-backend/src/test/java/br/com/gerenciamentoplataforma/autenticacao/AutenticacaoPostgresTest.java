package br.com.gerenciamentoplataforma.autenticacao;

import br.com.gerenciamentoplataforma.persistencia.ConexaoPostgresTeste;
import br.com.gerenciamentoplataforma.autenticacao.dto.TokensAutenticacao;
import br.com.gerenciamentoplataforma.autenticacao.service.TokenJwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(AutenticacaoPostgresTest.ConfiguracaoOperacao.class)
@EnabledIfEnvironmentVariable(named = "TESTE_AUTENTICACAO_POSTGRES", matches = "true")
class AutenticacaoPostgresTest {

    private static final String SENHA = "Senha-local-de-teste!";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder senhas;

    @Autowired
    private TokenJwt jwt;

    @Autowired
    private OperacaoProtegida operacao;

    private final List<UUID> permissoesCriadas = new ArrayList<>();

    private UUID usuario;
    private UUID aplicacao;
    private UUID outraAplicacao;
    private String login;
    private String slug;
    private String outroSlug;

    @DynamicPropertySource
    static void configurarConexao(DynamicPropertyRegistry propriedades) {
        ConexaoPostgresTeste.configurar(propriedades);
    }

    @BeforeEach
    void prepararDados() {
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("gerenciamento_usuarios");
        usuario = UUID.randomUUID();
        aplicacao = UUID.randomUUID();
        outraAplicacao = UUID.randomUUID();
        login = "teste-" + usuario;
        slug = "teste-" + aplicacao;
        outroSlug = "teste-" + outraAplicacao;

        jdbc.update("insert into usuario (id, login, email, senha_hash, status) values (?, ?, ?, ?, 'ATIVO')",
                usuario, login, login + "@example.test", senhas.encode(SENHA));
        jdbc.update("insert into aplicacao (id, nome, slug, tipo, status) values (?, ?, ?, 'PUBLICO', 'ATIVO')",
                aplicacao, "Aplicação de teste", slug);
        jdbc.update("insert into aplicacao (id, nome, slug, tipo, status) values (?, ?, ?, 'PUBLICO', 'ATIVO')",
                outraAplicacao, "Outra aplicação de teste", outroSlug);
        jdbc.update("insert into usuario_aplicacao (id_usuario, id_aplicacao) values (?, ?)", usuario, aplicacao);
    }

    @AfterEach
    void removerDadosCriados() {
        if (usuario == null) {
            return;
        }
        int removidos;
        do {
            removidos = jdbc.update("""
                    delete from tokens_atualizacao t
                    using sessoes s
                    where t.id_sessao = s.id and s.id_usuario = ?
                      and not exists (select 1 from tokens_atualizacao sucessor where sucessor.id_anterior = t.id)
                    """, usuario);
        } while (removidos > 0);
        jdbc.update("delete from auditoria where id_usuario = ?", usuario);
        jdbc.update("delete from sessoes where id_usuario = ?", usuario);
        jdbc.update("delete from usuario_perfil where id_usuario = ?", usuario);
        jdbc.update("delete from usuario_aplicacao where id_usuario = ?", usuario);
        jdbc.update("delete from usuario where id = ?", usuario);
        jdbc.update("delete from perfil_permissao where id_aplicacao in (?, ?)", aplicacao, outraAplicacao);
        jdbc.update("delete from perfil where id_aplicacao in (?, ?)", aplicacao, outraAplicacao);
        jdbc.update("delete from aplicacao_permissao where id_aplicacao in (?, ?)", aplicacao, outraAplicacao);
        jdbc.update("delete from aplicacao where id in (?, ?)", aplicacao, outraAplicacao);
        permissoesCriadas.forEach(id -> jdbc.update("delete from permissao where id = ?", id));
    }

    @Test
    void autenticaApenasUsuarioVinculadoERecusaJwtEmOutraAplicacao() throws Exception {
        fazerLogin(slug, "senha-incorreta").andExpect(status().isUnauthorized());
        fazerLogin(outroSlug, SENHA).andExpect(status().isUnauthorized());

        TokensAutenticacao tokens = entrar();
        mvc.perform(get(rota(slug, "eu")).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenAcesso()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.toString()))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());

        jdbc.update("insert into usuario_aplicacao (id_usuario, id_aplicacao) values (?, ?)", usuario, outraAplicacao);
        mvc.perform(get(rota(outroSlug, "eu")).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenAcesso()))
                .andExpect(status().isUnauthorized());
        renovar(outroSlug, tokens.tokenAtualizacao()).andExpect(status().isUnauthorized());
        renovar(slug, tokens.tokenAtualizacao()).andExpect(status().isOk());
    }

    @Test
    void revogaFamiliaQuandoDuasRequisicoesUsamOMesmoRefreshToken() throws Exception {
        TokensAutenticacao inicial = entrar();
        var largada = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                largada.await(5, TimeUnit.SECONDS);
                return renovar(slug, inicial.tokenAtualizacao()).andReturn().getResponse();
            });
            var segunda = executor.submit(() -> {
                largada.await(5, TimeUnit.SECONDS);
                return renovar(slug, inicial.tokenAtualizacao()).andReturn().getResponse();
            });
            var respostas = List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS));
            assertThat(respostas).extracting(resposta -> resposta.getStatus()).containsExactlyInAnyOrder(200, 401);
            var respostaValida = respostas.stream().filter(resposta -> resposta.getStatus() == 200)
                    .findFirst().orElseThrow();
            TokensAutenticacao sucessor = json.readValue(respostaValida.getContentAsByteArray(), TokensAutenticacao.class);
            renovar(slug, sucessor.tokenAtualizacao()).andExpect(status().isUnauthorized());
        }

        assertThat(jdbc.queryForObject("select status from sessoes where id_usuario = ?", String.class, usuario))
                .isEqualTo("REVOGADO");
        assertThat(jdbc.queryForObject("""
                select count(*) from tokens_atualizacao t join sessoes s on s.id = t.id_sessao
                where s.id_usuario = ? and t.is_revogado
                """, Long.class, usuario)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                select count(*) from auditoria where id_usuario = ? and acao = 'REUTILIZACAO_REFRESH_TOKEN'
                """, Long.class, usuario)).isEqualTo(1);
        mvc.perform(get(rota(slug, "eu")).header(HttpHeaders.AUTHORIZATION, "Bearer " + inicial.tokenAcesso()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidaAccessERefreshToken() throws Exception {
        TokensAutenticacao tokens = entrar();
        mvc.perform(post(rota(slug, "logout")).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenAcesso()))
                .andExpect(status().isNoContent());
        mvc.perform(get(rota(slug, "eu")).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.tokenAcesso()))
                .andExpect(status().isUnauthorized());
        consultarPermissoes(slug, tokens.tokenAcesso()).andExpect(status().isUnauthorized());
        renovar(slug, tokens.tokenAtualizacao()).andExpect(status().isUnauthorized());
    }

    @Test
    void listaPermissoesAtivasSemDuplicatasEIsolaOperacaoPorAplicacao() throws Exception {
        jdbc.update("insert into usuario_aplicacao (id_usuario, id_aplicacao) values (?, ?)", usuario, outraAplicacao);
        UUID perfilA = criarPerfil(aplicacao, "ATIVO");
        UUID segundoPerfilA = criarPerfil(aplicacao, "ATIVO");
        UUID perfilInativo = criarPerfil(aplicacao, "INATIVO");
        UUID perfilB = criarPerfil(outraAplicacao, "ATIVO");
        var consultar = criarPermissao("CONSULTAR", "ATIVO");
        var editar = criarPermissao("EDITAR", "ATIVO");
        var doPerfilInativo = criarPermissao("PERFIL_INATIVO", "ATIVO");
        var inativa = criarPermissao("INATIVA", "INATIVO");
        var semPerfil = criarPermissao("SEM_PERFIL", "ATIVO");

        conceder(aplicacao, perfilA, consultar);
        vincularPermissaoAoPerfil(aplicacao, segundoPerfilA, consultar);
        conceder(aplicacao, perfilA, editar);
        conceder(aplicacao, perfilInativo, doPerfilInativo);
        conceder(aplicacao, perfilA, inativa);
        habilitarPermissao(aplicacao, semPerfil);
        habilitarPermissao(outraAplicacao, editar);
        conceder(outraAplicacao, perfilB, consultar);

        String tokenA = entrar().tokenAcesso();
        String tokenB = entrar(outroSlug).tokenAcesso();
        consultarPermissoes(slug, tokenA).andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(List.of(consultar.codigo(), editar.codigo())), true));
        consultarPermissoes(outroSlug, tokenB).andExpect(status().isOk())
                .andExpect(content().json(json.writeValueAsString(List.of(consultar.codigo())), true));
        consultarPermissoes(outroSlug, tokenA).andExpect(status().isUnauthorized());

        try {
            autenticarOperacao(tokenA);
            assertThat(operacao.executar(slug, editar.codigo())).isTrue();
            assertThatThrownBy(() -> operacao.executar(outroSlug, editar.codigo()))
                    .isInstanceOf(AccessDeniedException.class);
            autenticarOperacao(tokenB);
            assertThatThrownBy(() -> operacao.executar(outroSlug, editar.codigo()))
                    .isInstanceOf(AccessDeniedException.class);

            jdbc.update("update permissao set status = 'INATIVO' where id = ?", editar.id());
            autenticarOperacao(tokenA);
            assertThatThrownBy(() -> operacao.executar(slug, editar.codigo()))
                    .isInstanceOf(AccessDeniedException.class);
            consultarPermissoes(slug, tokenA).andExpect(status().isOk())
                    .andExpect(content().json(json.writeValueAsString(List.of(consultar.codigo())), true));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void listaVaziaParaSessaoValidaERecusaAplicacaoDesativada() throws Exception {
        String token = entrar().tokenAcesso();
        consultarPermissoes(slug, token).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get(rota(slug, "administracao")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Acesso negado."));

        jdbc.update("update aplicacao set status = 'INATIVO' where id = ?", aplicacao);
        consultarPermissoes(slug, token).andExpect(status().isUnauthorized());
    }

    private UUID criarPerfil(UUID idAplicacao, String situacao) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into perfil (id_perfil, id_aplicacao, nome, status) values (?, ?, ?, ?)",
                id, idAplicacao, "Perfil " + id, situacao);
        jdbc.update("insert into usuario_perfil (id_usuario, id_aplicacao, id_perfil) values (?, ?, ?)",
                usuario, idAplicacao, id);
        return id;
    }

    private PermissaoTeste criarPermissao(String prefixo, String situacao) {
        UUID id = UUID.randomUUID();
        permissoesCriadas.add(id);
        String codigo = prefixo + "_" + id;
        jdbc.update("insert into permissao (id, codigo, status) values (?, ?, ?)", id, codigo, situacao);
        return new PermissaoTeste(id, codigo);
    }

    private void conceder(UUID idAplicacao, UUID idPerfil, PermissaoTeste permissao) {
        habilitarPermissao(idAplicacao, permissao);
        vincularPermissaoAoPerfil(idAplicacao, idPerfil, permissao);
    }

    private void habilitarPermissao(UUID idAplicacao, PermissaoTeste permissao) {
        jdbc.update("insert into aplicacao_permissao (id_aplicacao, id_permissao) values (?, ?)",
                idAplicacao, permissao.id());
    }

    private void vincularPermissaoAoPerfil(UUID idAplicacao, UUID idPerfil, PermissaoTeste permissao) {
        jdbc.update("insert into perfil_permissao (id_aplicacao, id_perfil, id_permissao) values (?, ?, ?)",
                idAplicacao, idPerfil, permissao.id());
    }

    private void autenticarOperacao(String token) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(jwt.validar(token), null, List.of()));
    }

    private ResultActions consultarPermissoes(String aplicacao, String token) throws Exception {
        return mvc.perform(get(rota(aplicacao, "permissoes")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private TokensAutenticacao entrar() throws Exception {
        return entrar(slug);
    }

    private TokensAutenticacao entrar(String aplicacao) throws Exception {
        var resposta = fazerLogin(aplicacao, SENHA).andExpect(status().isOk()).andReturn().getResponse();
        return json.readValue(resposta.getContentAsByteArray(), TokensAutenticacao.class);
    }

    private ResultActions fazerLogin(String aplicacao, String senha) throws Exception {
        return mvc.perform(post(rota(aplicacao, "login"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("login", login, "senha", senha))));
    }

    private ResultActions renovar(String aplicacao, String token) throws Exception {
        return mvc.perform(post(rota(aplicacao, "renovar"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("tokenAtualizacao", token))));
    }

    private String rota(String aplicacao, String acao) {
        return "/api/v1/aplicacoes/" + aplicacao + "/autenticacao/" + acao;
    }

    private record PermissaoTeste(UUID id, String codigo) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConfiguracaoOperacao {

        @Bean
        OperacaoProtegida operacaoProtegida() {
            return new OperacaoProtegida();
        }
    }

    static class OperacaoProtegida {

        @PreAuthorize("@autorizacao.temPermissao(authentication, #slug, #codigo)")
        public boolean executar(String slug, String codigo) {
            return true;
        }
    }
}
