package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.persistencia.ConexaoPostgresTeste;
import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "TESTE_ADMINISTRACAO_POSTGRES", matches = "true")
class AdministracaoPostgresTest {

    private static final String ADMIN = "/api/v1/admin";
    private static final String SENHA = "Senha-local-de-teste!";
    private static final UUID ID_PLATAFORMA = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private PasswordEncoder senhas;

    private final List<UUID> aplicacoes = new ArrayList<>();
    private final List<UUID> usuarios = new ArrayList<>();
    private final List<UUID> permissoes = new ArrayList<>();

    private UUID idPermissaoPlataforma;
    private UUID idPermissaoAplicacao;
    private String tokenPlataforma;
    private UUID operador;
    private String slugPlataforma;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry propriedades) {
        ConexaoPostgresTeste.configurar(propriedades);
        propriedades.add(AcessoAdministrativo.PROPRIEDADE_APLICACAO, ID_PLATAFORMA::toString);
    }

    @BeforeEach
    void preparar() throws Exception {
        assertThat(jdbc.queryForObject("select current_database()", String.class))
                .isEqualTo("hub_de_servicos");
        slugPlataforma = "teste-" + ID_PLATAFORMA;
        operador = UUID.randomUUID();
        String login = "teste-" + operador;
        idPermissaoPlataforma = permissaoDoTeste(AcessoAdministrativo.PLATAFORMA_ADMINISTRAR);
        idPermissaoAplicacao = permissaoDoTeste(AcessoAdministrativo.APLICACAO_ADMINISTRAR);
        jdbc.update("insert into aplicacao (id, nome, slug, tipo, status) values (?, ?, ?, 'PUBLICO', 'ATIVO')",
                ID_PLATAFORMA, "Administração de teste", slugPlataforma);
        aplicacoes.add(ID_PLATAFORMA);
        jdbc.update("insert into usuario (id, login, email, senha_hash, status) values (?, ?, ?, ?, 'ATIVO')",
                operador, login, login + "@example.test", senhas.encode(SENHA));
        usuarios.add(operador);
        jdbc.update("insert into usuario_aplicacao (id_usuario, id_aplicacao) values (?, ?)", operador, ID_PLATAFORMA);
        jdbc.update("insert into usuario_permissao_plataforma (id_usuario, id_permissao) values (?, ?)",
                operador, idPermissaoPlataforma);
        tokenPlataforma = login(slugPlataforma, login, SENHA).tokenAcesso();
    }

    @AfterEach
    void removerDadosDeTeste() {
        for (UUID app : aplicacoes) {
            int removidos;
            do {
                removidos = jdbc.update("""
                        delete from tokens_atualizacao t
                        where t.id_sessao in (select s.id from sessoes s where s.id_aplicacao = ?)
                          and not exists (select 1 from tokens_atualizacao sucessor where sucessor.id_anterior = t.id)
                        """, app);
            } while (removidos > 0);
            jdbc.update("delete from auditoria where id_aplicacao = ?", app);
            jdbc.update("delete from sessoes where id_aplicacao = ?", app);
            jdbc.update("delete from usuario_perfil where id_aplicacao = ?", app);
            jdbc.update("delete from perfil_permissao where id_aplicacao = ?", app);
            jdbc.update("delete from perfil where id_aplicacao = ?", app);
            jdbc.update("delete from aplicacao_permissao where id_aplicacao = ?", app);
            jdbc.update("delete from usuario_aplicacao where id_aplicacao = ?", app);
            jdbc.update("delete from aplicacao where id = ?", app);
        }
        for (UUID usuario : usuarios) {
            jdbc.update("delete from auditoria where id_usuario = ?", usuario);
            jdbc.update("delete from usuario_permissao_plataforma where id_usuario = ?", usuario);
            jdbc.update("delete from usuario where id = ?", usuario);
        }
        permissoes.forEach(id -> jdbc.update("delete from permissao where id = ?", id));
    }

    @Test
    void administraCadastrosComIsolamentoERevogaVinculoSemPerderHistorico() throws Exception {
        chamar(HttpMethod.GET, ADMIN + "/usuarios", null, null).andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, ADMIN + "/usuarios?tamanho=101", tokenPlataforma, null)
                .andExpect(status().isBadRequest());
        String slugA = "teste-" + UUID.randomUUID();
        String slugB = "teste-" + UUID.randomUUID();
        UUID appA = criarAplicacao(slugA);
        criarAplicacao(slugB);
        String rotaA = "/api/v1/aplicacoes/" + slugA + "/admin";
        String rotaB = "/api/v1/aplicacoes/" + slugB + "/admin";
        String login = "teste-" + UUID.randomUUID();
        UUID usuario = id(criar(ADMIN + "/usuarios", Map.of("login", login, "email", login + "@example.test",
                "senha", SENHA)));
        usuarios.add(usuario);

        chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + usuario, tokenPlataforma,
                Map.of("login", login, "email", login + "@example.test", "status", "ATIVO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.login").value(login));

        String codigo = "TESTE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
        UUID permissao = id(criar(ADMIN + "/permissoes", Map.of("codigo", codigo)));
        permissoes.add(permissao);
        UUID perfil = id(criar(rotaA + "/perfis", Map.of("nome", "Administrador de teste")));

        semConteudo(HttpMethod.PUT, ADMIN + "/aplicacoes/" + appA + "/permissoes/" + idPermissaoAplicacao);
        chamar(HttpMethod.PUT, ADMIN + "/aplicacoes/" + appA + "/permissoes/" + idPermissaoPlataforma,
                tokenPlataforma, null).andExpect(status().isConflict());
        semConteudo(HttpMethod.PUT, ADMIN + "/aplicacoes/" + appA + "/permissoes/" + permissao);
        semConteudo(HttpMethod.PUT, rotaA + "/perfis/" + perfil + "/permissoes/" + idPermissaoAplicacao);
        semConteudo(HttpMethod.PUT, rotaA + "/perfis/" + perfil + "/permissoes/" + permissao);
        semConteudo(HttpMethod.PUT, rotaA + "/usuarios/" + usuario);
        semConteudo(HttpMethod.PUT, rotaB + "/usuarios/" + usuario);
        semConteudo(HttpMethod.PUT, rotaA + "/usuarios/" + usuario + "/perfis/" + perfil);
        chamar(HttpMethod.PUT, rotaB + "/usuarios/" + usuario + "/perfis/" + perfil, tokenPlataforma, null)
                .andExpect(status().isNotFound());

        TokensAutenticacao tokensA = login(slugA, login, SENHA);
        TokensAutenticacao tokensB = login(slugB, login, SENHA);
        chamar(HttpMethod.GET, rotaA + "/perfis", tokensA.tokenAcesso(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(perfil.toString()));
        chamar(HttpMethod.GET, rotaA + "/usuarios", tokensA.tokenAcesso(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].idUsuario").value(usuario.toString()));
        chamar(HttpMethod.GET, rotaA + "/perfis/" + perfil + "/permissoes", tokensA.tokenAcesso(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        chamar(HttpMethod.GET, rotaB + "/perfis", tokensA.tokenAcesso(), null).andExpect(status().isForbidden());
        chamar(HttpMethod.GET, ADMIN + "/usuarios", tokensA.tokenAcesso(), null).andExpect(status().isForbidden());
        chamar(HttpMethod.GET, ADMIN + "/usuarios/" + usuario, tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.senhaHash").doesNotExist());
        chamar(HttpMethod.GET, ADMIN + "/usuarios?login=" + login.toUpperCase(java.util.Locale.ROOT)
                + "&email=example.test&status=ATIVO&tipo=APLICACAO", tokenPlataforma, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(usuario.toString()))
                .andExpect(jsonPath("$.itens[0].tipo").value("APLICACAO"))
                .andExpect(jsonPath("$.itens[0].permissoes.length()").value(2));
        chamar(HttpMethod.GET, ADMIN + "/usuarios?login=" + login + "&tipo=PLATAFORMA", tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        var resultadoDashboard = chamar(HttpMethod.GET, ADMIN + "/usuarios/dashboard?meses=3", tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.porMes.length()").value(3))
                .andExpect(jsonPath("$.porTipo[1].tipo").value("APLICACAO"))
                .andReturn().getResponse();
        var dashboard = json.readTree(resultadoDashboard.getContentAsByteArray());
        assertThat(dashboard.path("porTipo").get(1).path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("porPermissao").findValuesAsText("codigo")).contains(codigo);


        semConteudo(HttpMethod.DELETE, ADMIN + "/aplicacoes/" + appA + "/permissoes/" + permissao);
        chamar(HttpMethod.GET, rotaA + "/perfis/" + perfil + "/permissoes", tokensA.tokenAcesso(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        semConteudo(HttpMethod.DELETE, rotaA + "/usuarios/" + usuario);
        chamar(HttpMethod.GET, autenticacao(slugA, "eu"), tokensA.tokenAcesso(), null)
                .andExpect(status().isUnauthorized());
        chamar(HttpMethod.POST, autenticacao(slugA, "renovar"), null, Map.of("tokenAtualizacao", tokensA.tokenAtualizacao()))
                .andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, autenticacao(slugB, "eu"), tokensB.tokenAcesso(), null).andExpect(status().isOk());
        chamar(HttpMethod.GET, rotaA + "/sessoes", tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].status").value("REVOGADO"));
        chamar(HttpMethod.GET, rotaA + "/auditoria", tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].acao").value("DESVINCULAR_USUARIO"));

        semConteudo(HttpMethod.PUT, rotaA + "/usuarios/" + usuario);
        String novaSessao = login(slugA, login, SENHA).tokenAcesso();
        chamar(HttpMethod.GET, autenticacao(slugA, "permissoes"), novaSessao, null)
                .andExpect(status().isOk()).andExpect(content().json("[]"));

        semConteudo(HttpMethod.PUT, ADMIN + "/usuarios/" + usuario + "/senha", Map.of("senha", SENHA + "2"));
        chamar(HttpMethod.GET, autenticacao(slugA, "eu"), novaSessao, null).andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, autenticacao(slugB, "eu"), tokensB.tokenAcesso(), null)
                .andExpect(status().isUnauthorized());
        String tokenAntesDeExcluir = login(slugA, login, SENHA + "2").tokenAcesso();
        semConteudo(HttpMethod.DELETE, ADMIN + "/usuarios/" + usuario);
        semConteudo(HttpMethod.DELETE, ADMIN + "/usuarios/" + usuario);
        chamar(HttpMethod.GET, autenticacao(slugA, "eu"), tokenAntesDeExcluir, null)
                .andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, ADMIN + "/usuarios/" + usuario, tokenPlataforma, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"))
                .andExpect(jsonPath("$.permissoes").isEmpty());

    }

    @Test
    void atribuiPermissoesDiretasNoCadastroNaEdicaoENaSessao() throws Exception {
        String visitante = "PLATAFORMA_VISITANTE";
        String analista = "PLATAFORMA_ANALISTA";
        String administrador = AcessoAdministrativo.PLATAFORMA_ADMINISTRAR;
        var catalogo = chamar(HttpMethod.GET, ADMIN + "/permissoes-plataforma", tokenPlataforma, null)
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(json.readTree(catalogo.getContentAsByteArray()).findValuesAsText("codigo"))
                .contains(administrador, visitante, analista);

        for (List<String> codigos : List.of(List.<String>of(), List.of(visitante), List.of(analista),
                List.of(administrador), List.of(administrador, analista, visitante, visitante))) {
            String login = "teste-" + UUID.randomUUID();
            String email = login + "@example.test";
            JsonNode conta = criar(ADMIN + "/usuarios", Map.of("login", login, "email", email,
                    "senha", SENHA, "permissoesPlataforma", codigos));
            UUID usuario = id(conta);
            usuarios.add(usuario);
            List<String> esperadas = codigos.stream().distinct().sorted().toList();
            assertThat(codigos(conta.path("permissoesPlataforma"))).containsExactlyElementsOf(esperadas);
            chamar(HttpMethod.GET, ADMIN + "/usuarios/" + usuario, tokenPlataforma, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.permissoesPlataforma").value(is(esperadas)));
            if (codigos.isEmpty()) {
                chamar(HttpMethod.POST, autenticacao(slugPlataforma, "login"), null,
                        Map.of("login", login, "senha", SENHA)).andExpect(status().isUnauthorized());
                continue;
            }

            var sessao = login(slugPlataforma, login, SENHA);
            chamar(HttpMethod.GET, autenticacao(slugPlataforma, "permissoes"), sessao.tokenAcesso(), null)
                    .andExpect(status().isOk()).andExpect(content().json(json.writeValueAsString(esperadas), true));
            chamar(HttpMethod.GET, ADMIN + "/usuarios", sessao.tokenAcesso(), null)
                    .andExpect(codigos.contains(administrador) ? status().isOk() : status().isForbidden());

            chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + usuario, tokenPlataforma,
                    Map.of("login", login, "email", email, "status", "ATIVO",
                            "permissoesPlataforma", List.of(analista, visitante)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.permissoesPlataforma")
                            .value(is(List.of(analista, visitante))));
            var renovar = chamar(HttpMethod.POST, autenticacao(slugPlataforma, "renovar"), null,
                    Map.of("tokenAtualizacao", sessao.tokenAtualizacao()))
                    .andExpect(status().isOk()).andReturn().getResponse();
            var renovada = json.readValue(renovar.getContentAsByteArray(), TokensAutenticacao.class);
            chamar(HttpMethod.GET, autenticacao(slugPlataforma, "permissoes"), renovada.tokenAcesso(), null)
                    .andExpect(status().isOk()).andExpect(content().json(json.writeValueAsString(
                            List.of(analista, visitante)), true));
            chamar(HttpMethod.GET, ADMIN + "/usuarios", sessao.tokenAcesso(), null)
                    .andExpect(status().isForbidden());

            chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + usuario, tokenPlataforma,
                    Map.of("login", login, "email", email, "status", "ATIVO"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.permissoesPlataforma")
                            .value(is(List.of(analista, visitante))));
            chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + usuario, tokenPlataforma,
                    Map.of("login", login, "email", email, "status", "ATIVO", "permissoesPlataforma", List.of()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.permissoesPlataforma").isEmpty());
            var novaSessao = login(slugPlataforma, login, SENHA);
            chamar(HttpMethod.GET, autenticacao(slugPlataforma, "permissoes"), novaSessao.tokenAcesso(), null)
                    .andExpect(status().isOk()).andExpect(content().json("[]", true));
        }

        var contaOperador = chamar(HttpMethod.GET, ADMIN + "/usuarios/" + operador, tokenPlataforma, null)
                .andExpect(status().isOk()).andReturn().getResponse();
        JsonNode dadosOperador = json.readTree(contaOperador.getContentAsByteArray());
        chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + operador, tokenPlataforma,
                Map.of("login", dadosOperador.path("login").asText(), "email", dadosOperador.path("email").asText(),
                        "status", "ATIVO", "permissoesPlataforma", List.of(visitante)))
                .andExpect(status().isConflict());

        String loginInvalido = "teste-" + UUID.randomUUID();
        chamar(HttpMethod.POST, ADMIN + "/usuarios", tokenPlataforma,
                Map.of("login", loginInvalido, "email", loginInvalido + "@example.test",
                        "senha", SENHA, "permissoesPlataforma", List.of(AcessoAdministrativo.APLICACAO_ADMINISTRAR)))
                .andExpect(status().isBadRequest());
        chamar(HttpMethod.GET, ADMIN + "/usuarios?login=" + loginInvalido, tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void atribuiOsDoisPerfisDeNegocioEAdministraTelefoneSemVerificar() throws Exception {
        String empresa = "PLATAFORMA_EMPRESA";
        String trabalhador = "PLATAFORMA_TRABALHADOR";
        var catalogo = chamar(HttpMethod.GET, ADMIN + "/permissoes-plataforma", tokenPlataforma, null)
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(json.readTree(catalogo.getContentAsByteArray()).findValuesAsText("codigo"))
                .contains(empresa, trabalhador, "PLATAFORMA_ADMINISTRAR");
        String login = "teste-" + UUID.randomUUID();
        String email = login + "@example.test";
        String telefone = "349" + java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
        var conta = criar(ADMIN + "/usuarios", Map.of("login", login, "email", email, "senha", SENHA,
                "permissoesPlataforma", List.of(empresa, trabalhador), "telefone", telefone));
        UUID id = id(conta);
        usuarios.add(id);
        assertThat(codigos(conta.path("permissoesPlataforma"))).containsExactly(empresa, trabalhador);
        assertThat(conta.path("situacaoTelefone").asText()).isEqualTo("PENDENTE");
        assertThat(conta.path("telefone").asText()).isEqualTo("+55" + telefone);
        jdbc.update("update usuario set telefone_verificado_em = now() where id = ?", id);
        chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + id, tokenPlataforma,
                Map.of("login", login, "email", email, "status", "ATIVO", "telefone", "+55 " + telefone,
                        "permissoesPlataforma", List.of(trabalhador)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacaoTelefone").value("VERIFICADO"))
                .andExpect(jsonPath("$.permissoesPlataforma").value(is(List.of(trabalhador))));
        String outro = "teste-" + UUID.randomUUID();
        chamar(HttpMethod.POST, ADMIN + "/usuarios", tokenPlataforma, Map.of(
                "login", outro, "email", outro + "@example.test", "senha", SENHA, "telefone", telefone))
                .andExpect(status().isConflict());
        String numeroNovo = telefone.startsWith("34") ? "35" + telefone.substring(2) : telefone;
        chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + id, tokenPlataforma,
                Map.of("login", login, "email", email, "status", "ATIVO", "telefone", numeroNovo,
                        "situacaoTelefone", "VERIFICADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacaoTelefone").value("PENDENTE"));
        chamar(HttpMethod.PUT, ADMIN + "/usuarios/" + id, tokenPlataforma,
                Map.of("login", login, "email", email, "status", "ATIVO", "telefone", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.situacaoTelefone").value("NAO_INFORMADO"));
    }

    @Test
    void disponibilizaNovaPermissaoNoCatalogoSemAlterarFormulario() throws Exception {
        String codigo = "PLATAFORMA_TESTE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
        UUID permissao = id(criar(ADMIN + "/permissoes-plataforma", Map.of("codigo", codigo, "nome", "Acesso de teste")));
        permissoes.add(permissao);
        var resposta = chamar(HttpMethod.GET, ADMIN + "/permissoes-plataforma", tokenPlataforma, null)
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(json.readTree(resposta.getContentAsByteArray()).findValuesAsText("codigo")).contains(codigo);
        String login = "teste-" + UUID.randomUUID();
        var conta = criar(ADMIN + "/usuarios", Map.of("login", login, "email", login + "@example.test",
                "senha", SENHA, "permissoesPlataforma", List.of(codigo)));
        usuarios.add(id(conta));
        assertThat(codigos(conta.path("permissoesPlataforma"))).containsExactly(codigo);
    }

    private static List<String> codigos(JsonNode no) {
        var codigos = new ArrayList<String>();
        no.forEach(codigo -> codigos.add(codigo.asText()));
        return codigos;
    }

    private UUID permissaoDoTeste(String codigo) {
        var existentes = jdbc.query("select id from permissao where codigo = ?",
                (linha, indice) -> linha.getObject("id", UUID.class), codigo);
        if (!existentes.isEmpty()) {
            return existentes.getFirst();
        }
        UUID id = UUID.randomUUID();
        jdbc.update("insert into permissao (id, codigo, status) values (?, ?, 'ATIVO')", id, codigo);
        permissoes.add(id);
        return id;
    }

    private UUID criarAplicacao(String slug) throws Exception {
        UUID id = id(criar(ADMIN + "/aplicacoes", Map.of("nome", slug, "slug", slug, "tipo", "PUBLICO")));
        aplicacoes.add(id);
        return id;
    }

    private JsonNode criar(String rota, Map<String, ?> dados) throws Exception {
        var resposta = chamar(HttpMethod.POST, rota, tokenPlataforma, dados)
                .andExpect(status().isCreated()).andReturn().getResponse();
        return json.readTree(resposta.getContentAsByteArray());
    }

    private UUID id(JsonNode recurso) {
        return UUID.fromString(recurso.path("id").asText());
    }

    private TokensAutenticacao login(String slug, String login, String senha) throws Exception {
        var resposta = chamar(HttpMethod.POST, autenticacao(slug, "login"), null,
                Map.of("login", login, "senha", senha)).andExpect(status().isOk()).andReturn().getResponse();
        return json.readValue(resposta.getContentAsByteArray(), TokensAutenticacao.class);
    }

    private String autenticacao(String slug, String acao) {
        return "/api/v1/aplicacoes/" + slug + "/autenticacao/" + acao;
    }

    private void semConteudo(HttpMethod metodo, String rota) throws Exception {
        semConteudo(metodo, rota, null);
    }

    private void semConteudo(HttpMethod metodo, String rota, Map<String, ?> dados) throws Exception {
        chamar(metodo, rota, tokenPlataforma, dados).andExpect(status().isNoContent());
    }

    private ResultActions chamar(HttpMethod metodo, String rota, String token, Map<String, ?> dados)
            throws Exception {
        var requisicao = request(metodo, rota);
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (dados != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(dados));
        }
        return mvc.perform(requisicao);
    }
}
