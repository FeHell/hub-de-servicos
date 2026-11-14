package br.com.hubdeservicos.controller.app;

import br.com.hubdeservicos.persistencia.ConexaoPostgresTeste;
import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.autenticacao.TokenJwt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.liquibase.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@EnabledIfEnvironmentVariable(named = "TESTE_APP_POSTGRES", matches = "true")
class AutenticacaoAppPostgresTest {
    private static final String ROTA = AutenticacaoAppController.ROTA;
    private static final String SENHA = "Senha-de-teste-App-21!";
    private static final UUID ID_ADMIN = UUID.randomUUID();
    private static final String SLUG_ADMIN = "teste-admin-" + ID_ADMIN;
    private static final String LOGIN_ADMIN = "teste-" + ID_ADMIN;
    private static final String ROTA_ADMIN = "/api/v1/aplicacoes/" + SLUG_ADMIN + "/autenticacao";
    private static final String IP_TESTE = "192.0.2.20";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder senhas;
    @Autowired private TokenJwt jwt;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) {
        ConexaoPostgresTeste.configurar(propriedades);
        propriedades.add(AcessoAdministrativo.PROPRIEDADE_APLICACAO, ID_ADMIN::toString);
    }

    @Test
    void cadastraSemTelefoneSalvaHashEVinculaApenasAoAppSemPerfis() throws Exception {
        String email = emailTeste();
        UUID usuario = id(resposta(postar(ROTA + "/cadastro", Map.of("email", email, "senha", SENHA,
                "perfis", new String[]{"TRABALHADOR", "SOLICITANTE"},
                "permissoesPlataforma", new String[]{"PLATAFORMA_ADMINISTRAR"}))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.login").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())));

        String hash = jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, usuario);
        assertThat(hash).isNotEqualTo(SENHA);
        assertThat(senhas.matches(SENHA, hash)).isTrue();
        assertThat(jdbc.queryForList("""
                select aplicacao.slug from usuario_aplicacao vinculo
                join aplicacao on aplicacao.id = vinculo.id_aplicacao
                where vinculo.id_usuario = ? and vinculo.ativo
                """, String.class, usuario)).containsExactly(AutenticacaoAppService.SLUG);
        assertThat(jdbc.queryForObject("select count(*) from usuario_perfil where id_usuario = ?", Long.class, usuario))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from usuario_permissao_plataforma where id_usuario = ?",
                Long.class, usuario)).isZero();
    }

    @Test
    void rejeitaEmailInvalidoESenhaForaDaRegraAdministrativa() throws Exception {
        postar(ROTA + "/cadastro", credenciais("sem-arroba", SENHA))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.camposInvalidos[0]").value("email"));
        postar(ROTA + "/cadastro", credenciais(emailTeste(), "curta"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.camposInvalidos[0]").value("senha"));
        postar(ROTA + "/cadastro", credenciais(emailTeste(), "á".repeat(37)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value(
                        "A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes em UTF-8."));
    }

    @Test
    void rejeitaEmailDuplicadoSemDistinguirCaixaNemAlterarAContaExistente() throws Exception {
        String email = emailTeste();
        UUID existente = criarAdmin(email);
        String hash = jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, existente);
        postar(ROTA + "/cadastro", credenciais(email.toUpperCase(Locale.ROOT), SENHA))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from usuario where lower(email) = lower(?)",
                Long.class, email)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select senha_hash from usuario where id = ?", String.class, existente))
                .isEqualTo(hash);
        assertThat(jdbc.queryForObject("select count(*) from usuario_aplicacao where id_usuario = ?",
                Long.class, existente)).isEqualTo(1L);
    }

    @Test
    void autenticaPorEmailRenovaERevogaSessaoVinculadaAoApp() throws Exception {
        String email = emailTeste();
        UUID usuario = cadastrar(email);
        JsonNode acesso = resposta(postar(ROTA + "/login", credenciais(email.toUpperCase(Locale.ROOT), SENHA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.id").value(usuario.toString()))
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.login").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE)));
        String token = acesso.path("tokenAcesso").asText();
        var identidade = jwt.validar(token);
        assertThat(identidade.idUsuario()).isEqualTo(usuario);
        assertThat(identidade.audiencia()).isEqualTo(AutenticacaoAppService.SLUG);
        assertThat(Instant.parse(acesso.path("expiracaoTokenAcesso").asText())).isAfter(Instant.now());

        mvc.perform(get(ROTA + "/eu").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuario.toString()));
        mvc.perform(get("/api/v1/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        String refresh = acesso.path("tokenAtualizacao").asText();
        JsonNode renovado = resposta(postar(ROTA + "/renovar", Map.of("tokenAtualizacao", refresh))
                .andExpect(status().isOk()));
        assertThat(renovado.path("tokenAtualizacao").asText()).isNotEqualTo(refresh);
        String tokenRenovado = renovado.path("tokenAcesso").asText();
        mvc.perform(post(ROTA + "/logout").header(HttpHeaders.AUTHORIZATION, bearer(tokenRenovado)))
                .andExpect(status().isNoContent());
        mvc.perform(get(ROTA + "/eu").header(HttpHeaders.AUTHORIZATION, bearer(tokenRenovado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void retornaMesmoErroParaSenhaIncorretaEContaInexistente() throws Exception {
        String email = emailTeste();
        cadastrar(email);
        JsonNode senhaIncorreta = resposta(postar(ROTA + "/login", credenciais(email, "senha-incorreta"))
                .andExpect(status().isUnauthorized()));
        JsonNode contaInexistente = resposta(postar(ROTA + "/login", credenciais(emailTeste(), SENHA))
                .andExpect(status().isUnauthorized()));
        assertThat(senhaIncorreta).isEqualTo(contaInexistente);
    }

    @Test
    void preservaLoginNavegadorEAutorizacaoAdministrativaEIsolaAsSessoes() throws Exception {
        String email = emailTeste();
        UUID admin = criarAdmin(email);
        mvc.perform(get("/api/v1/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get(ROTA + "/eu")).andExpect(status().isUnauthorized());

        var navegador = mvc.perform(post(ROTA_ADMIN + "/navegador/login")
                        .header("X-Sessao-Navegador", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(Map.of("login", LOGIN_ADMIN, "senha", SENHA))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenAtualizacao").doesNotExist())
                .andReturn().getResponse();
        assertThat(navegador.getHeader(HttpHeaders.SET_COOKIE)).contains("HttpOnly", "SameSite=Strict");
        String token = json.readTree(navegador.getContentAsByteArray()).path("tokenAcesso").asText();
        assertThat(jwt.validar(token).idUsuario()).isEqualTo(admin);
        mvc.perform(get("/api/v1/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        mvc.perform(get(ROTA + "/eu").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        postar(ROTA + "/login", credenciais(email, SENHA)).andExpect(status().isUnauthorized());
    }

    @Test
    void impedeQueRotaGenericaContorneOContratoDoApp() throws Exception {
        postar("/api/v1/aplicacoes/" + AutenticacaoAppService.SLUG + "/autenticacao/login",
                Map.of("login", "qualquer", "senha", SENHA)).andExpect(status().isUnauthorized());
    }

    private UUID criarAdmin(String email) {
        UUID usuario = UUID.randomUUID();
        jdbc.update("insert into aplicacao (id, nome, slug, tipo, status) values (?, ?, ?, 'PUBLICO', 'ATIVO')",
                ID_ADMIN, "Admin para teste", SLUG_ADMIN);
        jdbc.update("insert into usuario (id, login, email, senha_hash, status) values (?, ?, ?, ?, 'ATIVO')",
                usuario, LOGIN_ADMIN, email, senhas.encode(SENHA));
        jdbc.update("insert into usuario_aplicacao (id_usuario, id_aplicacao) values (?, ?)", usuario, ID_ADMIN);
        jdbc.update("""
                insert into usuario_permissao_plataforma (id_usuario, id_permissao)
                select ?, id from permissao where codigo = 'PLATAFORMA_ADMINISTRAR' and status = 'ATIVO'
                """, usuario);
        return usuario;
    }

    private UUID cadastrar(String email) throws Exception {
        return id(resposta(postar(ROTA + "/cadastro", credenciais(email, SENHA)).andExpect(status().isCreated())));
    }

    private ResultActions postar(String rota, Map<String, ?> corpo) throws Exception {
        return mvc.perform(post(rota)
                .with(requisicao -> { requisicao.setRemoteAddr(IP_TESTE); return requisicao; })
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(corpo)));
    }

    private JsonNode resposta(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsByteArray());
    }

    private static UUID id(JsonNode conta) {
        return UUID.fromString(conta.path("id").asText());
    }

    private static Map<String, String> credenciais(String email, String senha) {
        return Map.of("email", email, "senha", senha);
    }

    private static String emailTeste() {
        return UUID.randomUUID() + "@example.test";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
