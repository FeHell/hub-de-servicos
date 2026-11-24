package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.persistencia.ConexaoPostgresTeste;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.provider.infobip.InfobipTransporteControlado;
import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import br.com.hubdeservicos.service.credenciamento.AutenticacaoPagarme;
import br.com.hubdeservicos.service.credenciamento.ClienteRecebedorPagarme;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ErroRecebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.StatusPagarme;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;
import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=false",
        "INFOBIP_API_KEY=env-legada-ficticia",
        "INFOBIP_BASE_URL=https://env-legada.api.infobip.com",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "TESTE_ADMINISTRACAO_POSTGRES", matches = "true")
class AdministracaoPostgresTest {

    private static final String ADMIN = "/api/v1/admin";
    private static final String SENHA = "Senha-local-de-teste!";
    private static final UUID ID_PLATAFORMA = UUID.randomUUID();
    private static final AtomicInteger ENDERECO_CADASTRO = new AtomicInteger(20);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private PasswordEncoder senhas;

    @Autowired
    private ConfiguracoesExternasService configuracoesExternas;
    @Autowired private AutenticacaoPagarme autenticacaoPagarme;
    @Autowired private CriptografiaCredenciais criptografia;
    @MockBean private ClienteRecebedorPagarme recebedorPagarme;
    @Autowired
    private ConfiguracaoSistemaRepository configuracoes;

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
        byte[] chave = new byte[32];
        new SecureRandom().nextBytes(chave);
        String chaveProtecao = Base64.getEncoder().encodeToString(chave);
        propriedades.add("HUB_CONFIGURACAO_CHAVE_MESTRA", () -> chaveProtecao);
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
    @ExtendWith(OutputCaptureExtension.class)
    void gravaCredenciaisProtegidasERecarregaInfobipPorEnvio(CapturedOutput logs) throws Exception {
        String base = ADMIN + "/configuracoes/servicos";
        String infobip = base + "/INFOBIP";
        String stone = base + "/STONE";
        String chaveA = "chave-ficticia-infobip-a";
        String chaveB = "chave-ficticia-infobip-b";
        String chaveStone = "chave-ficticia-stone";
        String novaChaveStone = "outra-chave-ficticia-stone";
        String urlA = "https://conta-a.api.infobip.com";
        String urlB = "https://conta-b.api.infobip.com";
        var builder = RestClient.builder();
        var http = MockRestServiceServer.bindTo(builder).build();
        var sms = InfobipTransporteControlado.sms(builder.build(), json, configuracoesExternas, configuracoes);
        var pedido = new SmsSendRequest("+5534999999999", "Código de teste");

        try {
            chamar(HttpMethod.GET, base, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].servico").value("INFOBIP"))
                    .andExpect(jsonPath("$[0].configuracoes[0].configurada").value(false))
                    .andExpect(jsonPath("$[1].servico").value("STONE"));
            chamar(HttpMethod.GET, stone, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.configuracoes[3].configurada").value(false))
                    .andExpect(jsonPath("$.configuracoes[0].valor").doesNotExist());
            assertThat(sms.send(pedido).erro()).isEqualTo(Erro.CREDENCIAIS_AUSENTES);
            chamar(HttpMethod.PUT, infobip, tokenPlataforma, Map.of("valores", Map.of("stone.dev.secretKey", chaveStone)))
                    .andExpect(status().isBadRequest()).andExpect(content().string(not(containsString(chaveStone))));
            for (String urlInvalida : List.of("", "http://conta.api.infobip.com", urlA + "/sms",
                    "https://conta.api.infobip.com.example.test", "https://127.0.0.1")) {
                chamar(HttpMethod.PUT, infobip, tokenPlataforma,
                        Map.of("valores", Map.of("INFOBIP_BASE_URL", urlInvalida)))
                        .andExpect(status().isBadRequest());
            }
            chamar(HttpMethod.PUT, stone, tokenPlataforma, Map.of("valores", Map.of("stone.dev.secretKey", chaveStone)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.configuracoes[3].configurada").value(true))
                    .andExpect(jsonPath("$.configuracoes[0].valor").doesNotExist());
            assertThat(valorExterno("STONE", "stone.dev.secretKey")).startsWith("v1:").doesNotContain(chaveStone);
            chamar(HttpMethod.PUT, stone, tokenPlataforma,
                    Map.of("valores", Map.of("stone.dev.secretKey", novaChaveStone))).andExpect(status().isOk());
            assertThat(sms.send(pedido).erro()).isEqualTo(Erro.CREDENCIAIS_AUSENTES);

            chamar(HttpMethod.PUT, infobip, tokenPlataforma, Map.of("valores", Map.of("INFOBIP_API_KEY", chaveA)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.configuracoes[0].valor").doesNotExist());
            assertThat(sms.send(pedido).erro()).isEqualTo(Erro.URL_BASE_AUSENTE);
            String protegidaA = valorExterno("INFOBIP", "INFOBIP_API_KEY");
            assertThat(protegidaA).startsWith("v1:").doesNotContain(chaveA);

            chamar(HttpMethod.PUT, infobip, tokenPlataforma,
                    Map.of("valores", Map.of("INFOBIP_BASE_URL", urlA, "INFOBIP_API_KEY", "  ")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.configuracoes[1].valor").value(urlA));
            assertThat(valorExterno("INFOBIP", "INFOBIP_API_KEY")).isEqualTo(protegidaA);
            http.expect(requestTo(urlA + "/sms/3/messages")).andExpect(header("Authorization", "App " + chaveA))
                    .andRespond(withSuccess("{\"messages\":[{\"messageId\":\"a\",\"status\":{\"groupName\":\"PENDING\"}}]}",
                            MediaType.APPLICATION_JSON));
            assertThat(sms.send(pedido).aceito()).isTrue();
            http.verify();
            http.reset();

            chamar(HttpMethod.PUT, infobip, tokenPlataforma,
                    Map.of("valores", Map.of("INFOBIP_BASE_URL", urlB, "INFOBIP_API_KEY", chaveB)))
                    .andExpect(status().isOk()).andExpect(content().string(not(containsString(chaveB))));
            http.expect(requestTo(urlB + "/sms/3/messages")).andExpect(header("Authorization", "App " + chaveB))
                    .andRespond(withSuccess("{\"messages\":[{\"messageId\":\"b\",\"status\":{\"groupName\":\"PENDING\"}}]}",
                            MediaType.APPLICATION_JSON));
            assertThat(sms.send(pedido).idExterno()).isEqualTo("b");
            http.verify();

            String protegidaB = valorExterno("INFOBIP", "INFOBIP_API_KEY");
            var semCache = org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                    .string(HttpHeaders.CACHE_CONTROL, containsString("no-store"));
            chamar(HttpMethod.GET, infobip, tokenPlataforma, null).andExpect(status().isOk()).andExpect(semCache)
                    .andExpect(jsonPath("$.configuracoes[0].valor").value(chaveB))
                    .andExpect(jsonPath("$.configuracoes[1].valor").value(urlB))
                    .andExpect(content().string(not(containsString(novaChaveStone))))
                    .andExpect(content().string(not(containsString(protegidaB))));
            chamar(HttpMethod.GET, stone, tokenPlataforma, null).andExpect(status().isOk()).andExpect(semCache)
                    .andExpect(jsonPath("$.configuracoes[3].valor").value(novaChaveStone))
                    .andExpect(jsonPath("$.configuracoes.length()").value(8))
                    .andExpect(content().string(not(containsString(chaveB))));
            chamar(HttpMethod.GET, base, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].configuracoes[0].valor").doesNotExist())
                    .andExpect(jsonPath("$[1].configuracoes[3].valor").doesNotExist());
            assertThat(configuracoesExternas.consultarComCredenciais(
                    br.com.hubdeservicos.entity.configuracao.ServicoExterno.INFOBIP).toString())
                    .doesNotContain(chaveB);
            jdbc.update("update configuracao_sistema set valor=? where servico='STONE' and chave='stone.dev.secretKey'",
                    protegidaB);
            chamar(HttpMethod.GET, stone, tokenPlataforma, null).andExpect(status().isServiceUnavailable())
                    .andExpect(semCache)
                    .andExpect(jsonPath("$.detail").value("Não foi possível consultar a credencial protegida."))
                    .andExpect(content().string(not(containsString(protegidaB))));
            assertThat(logs.getAll()).doesNotContain(chaveA, chaveB, chaveStone, novaChaveStone, protegidaB);
            assertThat(jdbc.queryForList("select valor_novo::text from auditoria where acao = 'ALTERAR_CONFIGURACAO_SERVICO' "
                    + "and id_usuario = ?", String.class, operador))
                    .isNotEmpty().allSatisfy(item -> assertThat(item).doesNotContain(chaveA, chaveB, chaveStone, "v1:"));
            assertThatThrownBy(() -> jdbc.update("""
                    insert into configuracao_sistema (id, servico, chave, valor, tipo, status)
                    values (?, 'INFOBIP', 'INFOBIP_BASE_URL', '', 'URL', 'ATIVO')
                    """, UUID.randomUUID())).hasMessageContaining("uk_configuracao_sistema_servico_chave");
            assertThatThrownBy(() -> jdbc.update("""
                    update configuracao_sistema set valor='texto-plano-ficticio'
                    where servico='STONE' and chave='stone.dev.secretKey'
                    """)).hasMessageContaining("ck_configuracao_sistema_segredo");
        } finally {
            jdbc.update("update configuracao_sistema set valor = '' where servico in ('INFOBIP', 'STONE')");
        }
    }

    private String valorExterno(String servico, String chave) {
        return jdbc.queryForObject("select valor from configuracao_sistema where servico=? and chave=?",
                String.class, servico, chave);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void alternaAmbienteGlobalSemFallback(CapturedOutput saida) throws Exception {
        String modo = ADMIN + "/configuracoes/dev-mode";
        String stone = ADMIN + "/configuracoes/servicos/STONE";
        try {
            chamar(HttpMethod.GET, modo, null, null).andExpect(status().isUnauthorized());
            chamar(HttpMethod.PUT, modo, null, Map.of("devMode", false)).andExpect(status().isUnauthorized());
            chamar(HttpMethod.GET, modo, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.devMode").value(true));
            chamar(HttpMethod.PUT, stone, tokenPlataforma, Map.of("valores", Map.of(
                    "stone.dev.accountId", "conta-dev-teste", "stone.dev.secretKey", "sk_test_segredo-dev-teste")))
                    .andExpect(status().isOk());
            assertThat(configuracoesExternas.lerStone().ambiente()).isEqualTo(Ambiente.DEV);
            assertThat(configuracoesExternas.lerStone().secretKey()).isEqualTo("sk_test_segredo-dev-teste");
            chamar(HttpMethod.PUT, modo, tokenPlataforma, Map.of("devMode", false))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.devMode").value(false));
            assertThatThrownBy(configuracoesExternas::lerStone).hasMessageContaining("Stone PROD incompleta");
            chamar(HttpMethod.PUT, stone, tokenPlataforma, Map.of("valores", Map.of(
                    "stone.prod.accountId", "conta-prod-teste", "stone.prod.publicKey", "publica-prod-teste",
                    "stone.prod.recipientMaster", "recebedor-prod-teste"))).andExpect(status().isOk());
            assertThatThrownBy(configuracoesExternas::lerStone).hasMessageContaining("stone.prod.secretKey");
            chamar(HttpMethod.PUT, stone, tokenPlataforma,
                    Map.of("valores", Map.of("stone.prod.secretKey", "sk_segredo-prod-teste"))).andExpect(status().isOk());
            assertThat(configuracoesExternas.lerStone().ambiente()).isEqualTo(Ambiente.PROD);
            assertThat(configuracoesExternas.lerStone().accountId()).isEqualTo("conta-prod-teste");
            assertThat(configuracoesExternas.lerStone().secretKey()).isEqualTo("sk_segredo-prod-teste");
            var cabecalhos = new HttpHeaders();
            var conta = autenticacaoPagarme.selecionarConta();
            conta.aplicarAutenticacao(cabecalhos);
            assertThat(conta.accountId()).isEqualTo("conta-prod-teste");
            assertThat(cabecalhos.getFirst(HttpHeaders.AUTHORIZATION))
                    .isEqualTo("Basic " + HttpHeaders.encodeBasicAuth("sk_segredo-prod-teste", "", null));
            for (String chave : List.of("stone.dev.secretKey", "stone.prod.secretKey")) {
                String valor = valorExterno("STONE", chave);
                assertThat(valor).startsWith("v1:").doesNotContain("segredo");
                chamar(HttpMethod.PUT, stone, tokenPlataforma, Map.of("valores", Map.of(chave, " "))).andExpect(status().isOk());
                assertThat(valorExterno("STONE", chave)).isEqualTo(valor);
            }
            chamar(HttpMethod.GET, stone, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                            .string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.configuracoes[?(@.chave == 'stone.dev.secretKey')].valor",
                            org.hamcrest.Matchers.contains("sk_test_segredo-dev-teste")))
                    .andExpect(jsonPath("$.configuracoes[?(@.chave == 'stone.prod.secretKey')].valor",
                            org.hamcrest.Matchers.contains("sk_segredo-prod-teste")));
            var app = contaCredenciamento();
            chamar(HttpMethod.GET, stone, app.token(), null).andExpect(status().isForbidden())
                    .andExpect(content().string(not(containsString("segredo"))));
            mvc.perform(request(HttpMethod.GET, stone).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlataforma)
                    .with(req -> { req.setRemoteAddr("192.0.2.10"); return req; }))
                    .andExpect(status().isForbidden()).andExpect(content().string(not(containsString("segredo"))));
            chamar(HttpMethod.PUT, stone, tokenPlataforma,
                    Map.of("valores", Map.of("stone.prod.secretKey", "sk_test_chave-incompativel"))).andExpect(status().isOk());
            assertThatThrownBy(autenticacaoPagarme::selecionarConta)
                    .hasMessageContaining("incompatível").hasMessageNotContaining("sk_test_chave-incompativel");
            assertThat(saida.getAll()).doesNotContain("sk_test_segredo-dev-teste", "sk_segredo-prod-teste",
                    "sk_test_chave-incompativel");
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave='stone.dev.accountId'");
            chamar(HttpMethod.PUT, modo, tokenPlataforma, Map.of("devMode", true)).andExpect(status().isOk());
            assertThatThrownBy(configuracoesExternas::lerStone).hasMessageContaining("stone.dev.accountId");
        } finally {
            jdbc.update("update configuracao_sistema set valor='true' where servico='SISTEMA' and chave='DEV_MODE'");
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave like 'stone.%'");
        }
    }

    @Test
    void credenciamentoIsolaSolicitantesEGaranteDadosProtegidos() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        var primeiro = contaCredenciamento();
        var segundo = contaCredenciamento();
        var endereco = Map.of("cep", "00000-000", "logradouro", "Endereço de teste", "numero", "0",
                "bairro", "Bairro de teste", "cidade", "Cidade de teste", "uf", "MG");
        var empresa = new java.util.HashMap<String, Object>(Map.of("razaoSocial", "Cenário de teste",
                "cnpj", "12.ABC.345/01DE-35", "email", "caso@example.test", "telefone", "34999999999",
                "faturamento", "100.00", "endereco", endereco));
        var representante = new java.util.HashMap<String, Object>(Map.of("nome", "Representante de teste",
                "cpf", cpfTeste(), "nascimento", "2000-01-01", "profissao", "Teste", "renda", "1.00", "endereco", endereco));
        var conta = new java.util.HashMap<String, String>(Map.of("titular", "Titular de teste",
                "documento", representante.get("cpf").toString(), "tipo", "CORRENTE", "banco", "001",
                "agencia", "0007", "conta", "0000123", "digitoConta", "0"));
        var dados = Map.of("empresa", empresa, "representante", representante, "contaBancaria", conta);
        chamar(HttpMethod.GET, rota, null, null).andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, rota, tokenPlataforma, null).andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, ADMIN + "/configuracoes/dev-mode", primeiro.token(), null).andExpect(status().isForbidden());
        chamar(HttpMethod.PUT, ADMIN + "/configuracoes/dev-mode", primeiro.token(), Map.of("devMode", false))
                .andExpect(status().isForbidden());
        chamar(HttpMethod.POST, rota, primeiro.token(), dados).andExpect(status().isForbidden());

        for (var alvo : List.of(primeiro, segundo)) {
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/EMPRESA", alvo.token(), Map.of("selecionado", true))
                    .andExpect(status().isOk());
        }

        String cpf = representante.get("cpf").toString();
        try {
            chamar(HttpMethod.GET, rota, primeiro.token(), null).andExpect(status().isNotFound());
            UUID idLegado = UUID.randomUUID();
            String legado = criptografia.criptografar("credenciamento:" + idLegado + ":" + primeiro.id(),
                    json.writeValueAsString(dados));
            jdbc.update("""
                    insert into credenciamento_empresarial (id, id_solicitante, provedor, dados_protegidos, data_criacao, data_atualizacao)
                    values (?, ?, 'STONE', ?, current_timestamp, current_timestamp)
                    """, idLegado, primeiro.id(), legado);
            chamar(HttpMethod.GET, rota, primeiro.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.dados.modalidade").value("CORPORATION"))
                    .andExpect(jsonPath("$.dados.empresa.faturamento").value("100.00"))
                    .andExpect(jsonPath("$.dados.empresa.faturamentoAnual").isEmpty())
                    .andExpect(jsonPath("$.completude.apto").value(false));
            assertThat(jdbc.queryForObject("select dados_protegidos from credenciamento_empresarial where id=?",
                    String.class, idLegado)).isEqualTo(legado);
            representante.put("cpf", "00000000000");
            empresa.put("cnpj", "00000000000000");
            chamar(HttpMethod.POST, rota + "/validacao", primeiro.token(), dados).andExpect(status().isOk())
                    .andExpect(jsonPath("$.apto").value(false))
                    .andExpect(jsonPath("$.invalidos[*].campo", org.hamcrest.Matchers.hasItems("empresa.cnpj", "representante.cpf")));
            representante.put("cpf", cpf);
            empresa.put("cnpj", "12.ABC.345/01DE-35");
            chamar(HttpMethod.POST, rota, primeiro.token(), dados).andExpect(status().isConflict());
            chamar(HttpMethod.GET, rota, segundo.token(), null).andExpect(status().isNotFound());
            chamar(HttpMethod.PUT, rota, segundo.token(), dados).andExpect(status().isNotFound());
            conta.put("conta", "0000001");
            chamar(HttpMethod.PUT, rota, primeiro.token(), dados).andExpect(status().isOk());
            chamar(HttpMethod.GET, rota, primeiro.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.dados.contaBancaria.conta").value("0000001"))
                    .andExpect(jsonPath("$.dados.representante.nome").value("Representante de teste"));
            assertThat(jdbc.queryForObject("select dados_protegidos from credenciamento_empresarial where id_solicitante=?",
                    String.class, primeiro.id())).startsWith("v1:").doesNotContain("0000001", cpf, "Cenário de teste");
        } finally {
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", primeiro.id());
        }
    }

    @Test
    void trabalhadorECadastradoPfEPjComValidacaoEPreviaAdministrativa() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        String stone = ADMIN + "/configuracoes/servicos/STONE";
        var conta = contaCredenciamento();
        chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", conta.token(), Map.of("selecionado", true))
                .andExpect(status().isOk());
        var endereco = Map.of("cep", "01001-000", "logradouro", "Rua de teste", "numero", "1",
                "complemento", "Bloco de teste", "bairro", "Centro", "cidade", "Cidade do teste",
                "uf", "SP", "pontoReferencia", "Referência de teste");
        String cpf = cpfTeste();
        String cnpj = "11222333000181";
        var pessoa = new java.util.HashMap<String, Object>(Map.of("nome", "Pessoa de teste",
                "cpf", cpf, "email", "pessoa@example.invalid", "telefone", "34999999999", "tipoTelefone", "CELULAR",
                "nascimento", "2000-01-01", "profissao", "Teste", "rendaMensal", "100.10", "endereco", endereco,
                "siteUrl", "https://example.invalid"));
        var banco = new java.util.HashMap<String, String>(Map.of("titular", "Pessoa de teste", "documento", cpf,
                "tipoTitular", "INDIVIDUAL", "tipo", "CORRENTE", "banco", "001", "agencia", "0001",
                "conta", "000007", "digitoConta", "0"));
        var pf = Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", pessoa, "contaBancaria", banco);
        try {
            var rascunho = chamar(HttpMethod.POST, rota, conta.token(), Map.of("modalidade", "INDIVIDUAL"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.completude.apto").value(false)).andReturn();
            String id = json.readTree(rascunho.getResponse().getContentAsByteArray()).path("id").asText();
            String previa = ADMIN + "/credenciamentos/" + id + "/payload?unidadeMonetaria=CENTAVOS";
            chamar(HttpMethod.GET, previa, conta.token(), null).andExpect(status().isForbidden());
            chamar(HttpMethod.GET, previa, tokenPlataforma, null).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.completude.apto").value(false));
            chamar(HttpMethod.PUT, rota, conta.token(), pf).andExpect(status().isOk())
                    .andExpect(jsonPath("$.completude.apto").value(true));
            chamar(HttpMethod.GET, rota + "/completude", conta.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.apto").value(true));
            chamar(HttpMethod.GET, previa, tokenPlataforma, null).andExpect(status().isServiceUnavailable());
            chamar(HttpMethod.PUT, stone, tokenPlataforma, Map.of("valores", Map.of(
                    "stone.dev.accountId", "conta-dev-teste", "stone.dev.publicKey", "publica-dev-teste",
                    "stone.dev.secretKey", "sk_test_prev_local", "stone.dev.recipientMaster", "recebedor-dev-teste")))
                    .andExpect(status().isOk());
            chamar(HttpMethod.GET, previa, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.register_information.type").value("individual"))
                    .andExpect(jsonPath("$.payload.register_information.monthly_income").value(10010))
                    .andExpect(content().string(not(containsString("sk_test_prev_local"))));
            var empresa = Map.of("razaoSocial", "Razão de teste", "nomeFantasia", "Fantasia de teste",
                    "cnpj", cnpj, "email", "empresa@example.invalid", "telefone", "34999999999",
                    "tipoTelefone", "CELULAR", "faturamentoAnual", "40000.00", "siteUrl", "https://example.invalid",
                    "endereco", endereco);
            pessoa.remove("siteUrl");
            pessoa.put("representanteLegal", true);
            pessoa.put("socioQsa", true);
            banco.put("documento", cnpj);
            banco.put("tipoTitular", "COMPANY");
            var pj = Map.of("modalidade", "CORPORATION", "empresa", empresa, "representante", pessoa, "contaBancaria", banco);
            chamar(HttpMethod.PUT, rota, conta.token(), pj).andExpect(status().isOk())
                    .andExpect(jsonPath("$.completude.apto").value(true));
            chamar(HttpMethod.GET, previa, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload.register_information.type").value("corporation"))
                    .andExpect(jsonPath("$.payload.register_information.managing_partners").isArray())
                    .andExpect(jsonPath("$.payload.default_bank_account.account_number").value("000007"));
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", conta.id());
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/EMPRESA", conta.token(), Map.of("selecionado", true))
                    .andExpect(status().isOk());
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", conta.token(), Map.of("selecionado", false))
                    .andExpect(status().isOk());
            chamar(HttpMethod.POST, rota, conta.token(), pj).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dados.modalidade").value("CORPORATION"))
                    .andExpect(jsonPath("$.completude.apto").value(true));
            assertThat(jdbc.queryForObject("select count(*) from credenciamento_recebedor_externo", Long.class)).isZero();
        } finally {
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", conta.id());
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave like 'stone.%'");
        }
    }

    @Test
    void enviaPfPersisteTentativaEReconciliacaoSemNovoPost() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE/recebedor";
        String configuracao = ADMIN + "/configuracoes/servicos/STONE";
        String devMode = ADMIN + "/configuracoes/dev-mode";
        var trabalhador = contaCredenciamento();
        var outro = contaCredenciamento();
        chamar(HttpMethod.POST, rota, null, Map.of()).andExpect(status().isUnauthorized());
        chamar(HttpMethod.POST, rota + "/reenvio", null, Map.of()).andExpect(status().isUnauthorized());
        chamar(HttpMethod.GET, rota, outro.token(), null).andExpect(status().isForbidden());
        for (var pessoa : List.of(trabalhador, outro)) {
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", pessoa.token(), Map.of("selecionado", true))
                    .andExpect(status().isOk());
        }
        String cpf = cpfTeste();
        var endereco = Map.of("cep", "01001-000", "logradouro", "Rua de teste", "numero", "1", "complemento", "Bloco",
                "bairro", "Bairro", "cidade", "Cidade", "uf", "SP", "pontoReferencia", "Referência de teste");
        var pessoa = new java.util.HashMap<String, Object>(Map.of("nome", "Pessoa de teste", "cpf", cpf,
                "email", "pessoa@example.invalid", "telefone", "34999999999", "nascimento", "2000-01-01", "profissao", "Teste",
                "rendaMensal", "100.10", "endereco", endereco, "siteUrl", "https://example.invalid"));
        var banco = new java.util.HashMap<>(Map.of("titular", "Pessoa de teste", "documento", cpf, "tipoTitular", "INDIVIDUAL", "tipo", "CORRENTE",
                "banco", "001", "agencia", "0001", "conta", "000007", "digitoConta", "0"));
        var dados = Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", pessoa, "contaBancaria", banco);
        var pedido = Map.of("ambiente", "DEV", "dados", dados);
        try {
            chamar(HttpMethod.PUT, configuracao, tokenPlataforma, Map.of("valores", Map.of(
                    "stone.dev.accountId", "conta-dev-teste", "stone.dev.publicKey", "publica-dev-teste",
                    "stone.dev.secretKey", "sk_test_local", "stone.dev.recipientMaster", "recebedor-dev-teste")))
                    .andExpect(status().isOk());
            chamar(HttpMethod.POST, rota, trabalhador.token(), pedido).andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.completude.faltantes[0].campo").value("pessoaFisica.tipoTelefone"));
            verifyNoInteractions(recebedorPagarme);
            pessoa.put("tipoTelefone", "CELULAR");
            chamar(HttpMethod.POST, rota, trabalhador.token(), Map.of("ambiente", "PROD", "dados", dados))
                    .andExpect(status().isConflict());
            verifyNoInteractions(recebedorPagarme);

            String rastreio = UUID.randomUUID().toString();
            String corpoProvedor = """
                    {"message":"Invalid bank account: %s", "gateway_response":{"code":"BANK_ACCOUNT_INVALID",
                    "errors":[{"message":"CPF %s da pessoa de teste", "code":"HOLDER_CHECK_FAILED"}]},
                    "errors":{"default_bank_account.bank":["Invalid bank account: 000007"]}}
                    """.formatted(cpf, cpf);
            var captura = new ClienteRecebedorPagarme.DiagnosticoHttp(422, Map.of("x-request-id", rastreio),
                    corpoProvedor.getBytes(StandardCharsets.UTF_8), false, false);
            var referencia = new java.util.concurrent.atomic.AtomicReference<UUID>();
            when(recebedorPagarme.criar(any(), any(), any()))
                    .thenReturn(new ClienteRecebedorPagarme.Resultado(null, null, ErroRecebedor.DADOS_RECUSADOS,
                            List.of("contaBancaria.banco"), captura))
                    .thenAnswer(chamada -> {
                        AutenticacaoPagarme.Conta conta = chamada.getArgument(0);
                        PagarmeRecebedor payload = chamada.getArgument(1);
                        assertThat(conta.ambiente()).isEqualTo(Ambiente.DEV);
                        assertThat(json.valueToTree(payload).path("default_bank_account").path("bank").asText()).isEqualTo("077");
                        assertThat(jdbc.queryForObject("select situacao from credenciamento_recebedor_externo where id_credenciamento=?",
                                String.class, UUID.fromString(payload.code()))).isEqualTo("EM_ANALISE");
                        var duplicada = java.util.concurrent.CompletableFuture.runAsync(() -> {
                            try {
                                chamar(HttpMethod.POST, rota + "/reenvio", trabalhador.token(),
                                        Map.of("ambiente", "DEV", "versao", 1, "tentativa", referencia.get()))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.emProcessamento").value(true))
                                        .andExpect(jsonPath("$.podeReenviar").value(false));
                            } catch (Exception erro) {
                                throw new IllegalStateException(erro);
                            }
                        });
                        duplicada.get(5, java.util.concurrent.TimeUnit.SECONDS);
                        return new ClienteRecebedorPagarme.Resultado(null, null, ErroRecebedor.RESPOSTA_INDETERMINADA, List.of());
                    });
            chamar(HttpMethod.POST, rota, trabalhador.token(), pedido).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("FALHA"))
                    .andExpect(jsonPath("$.podeReenviar").value(true))
                    .andExpect(jsonPath("$.pendencias[0].campo").value("contaBancaria.banco"));
            var tentativaInicial = jdbc.queryForObject("select tentativa_ref from credenciamento_recebedor_externo", UUID.class);
            referencia.set(tentativaInicial);
            String diagnostico = ADMIN + "/credenciamentos/tentativas/" + tentativaInicial + "/diagnostico";
            var protegido = jdbc.queryForObject("select resposta_protegida from credenciamento_recebedor_diagnostico where tentativa_ref=?",
                    String.class, tentativaInicial);
            assertThat(protegido).startsWith("v1:").doesNotContain(cpf, "Invalid bank account");
            var conteudo = criptografia.descriptografar("pagarme:diagnostico:" + tentativaInicial, protegido);
            assertThat(json.readTree(conteudo).path("corpo").binaryValue()).isEqualTo(captura.corpo());
            chamar(HttpMethod.GET, diagnostico, null, null).andExpect(status().isUnauthorized());
            chamar(HttpMethod.GET, diagnostico, trabalhador.token(), null).andExpect(status().isForbidden());
            banco.put("banco", "077");
            chamarVersionado(HttpMethod.PUT, rota.replace("/recebedor", ""), trabalhador.token(), dados, 0, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.versao").value(1));
            var reenvio = Map.of("ambiente", "DEV", "versao", 1, "tentativa", tentativaInicial);
            chamar(HttpMethod.POST, rota + "/reenvio", outro.token(), reenvio).andExpect(status().isConflict());
            var tentativaAtual = json.readTree(chamar(HttpMethod.POST, rota + "/reenvio", trabalhador.token(),
                    Map.of("ambiente", "DEV", "versao", 1, "tentativa", tentativaInicial,
                            "dados", Map.of("contaBancaria", Map.of("banco", "999"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("INDETERMINADO"))
                    .andExpect(jsonPath("$.podeReenviar").value(false))
                    .andExpect(jsonPath("$.envioAceito").value(false))
                    .andReturn().getResponse().getContentAsByteArray()).path("tentativa").asText();
            assertThat(tentativaAtual).isNotEqualTo(tentativaInicial.toString());
            chamar(HttpMethod.POST, rota + "/reenvio", trabalhador.token(),
                    Map.of("ambiente", "DEV", "versao", 1, "tentativa", tentativaAtual))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.tentativa").value(tentativaAtual))
                    .andExpect(jsonPath("$.podeReenviar").value(false));
            chamar(HttpMethod.GET, diagnostico, tokenPlataforma, null).andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                            .string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(jsonPath("$.statusHttp").value(422)).andExpect(jsonPath("$.ambiente").value("DEV"))
                    .andExpect(jsonPath("$.cabecalhos['x-request-id']").value(rastreio))
                    .andExpect(jsonPath("$.codigoGateway").value("BANK_ACCOUNT_INVALID"))
                    .andExpect(jsonPath("$.codigos").value(org.hamcrest.Matchers.hasItem("HOLDER_CHECK_FAILED")))
                    .andExpect(jsonPath("$.trechosSeguros[0]").value("Invalid bank account"))
                    .andExpect(jsonPath("$.camposRecusados[0]").value("contaBancaria.banco"))
                    .andExpect(content().string(not(containsString(cpf))))
                    .andExpect(content().string(not(containsString("000007"))));
            chamar(HttpMethod.POST, rota, trabalhador.token(), pedido).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("INDETERMINADO"));
            chamar(HttpMethod.POST, rota, outro.token(), pedido).andExpect(status().isConflict());
            chamar(HttpMethod.GET, rota, outro.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("NAO_ENVIADO"));
            verify(recebedorPagarme, times(2)).criar(any(), any(), any());

            when(recebedorPagarme.consultar(any(), anyString(), isNull()))
                    .thenReturn(new ClienteRecebedorPagarme.Resultado(null, null, ErroRecebedor.RESPOSTA_INDETERMINADA, List.of()))
                    .thenReturn(new ClienteRecebedorPagarme.Resultado("re_teste_local", StatusPagarme.REGISTRATION, null, List.of()));
            chamar(HttpMethod.POST, rota + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("INDETERMINADO"));
            chamar(HttpMethod.POST, rota + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("CRIADO"))
                    .andExpect(jsonPath("$.statusProvedor").value("REGISTRATION"))
                    .andExpect(jsonPath("$.envioAceito").value(true))
                    .andExpect(jsonPath("$.podeReenviar").value(false))
                    .andExpect(content().string(not(containsString(cpf))))
                    .andExpect(content().string(not(containsString("sk_test_local"))));
            assertThat(jdbc.queryForObject("select id_recebedor from credenciamento_recebedor_externo", String.class))
                    .isEqualTo("re_teste_local");
            assertThat(jdbc.queryForObject("select documento_digest from credenciamento_recebedor_externo", String.class))
                    .matches("[a-f0-9]{64}").doesNotContain(cpf);
            chamar(HttpMethod.POST, rota, trabalhador.token(), pedido).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("CRIADO"));
            when(recebedorPagarme.consultar(any(), anyString(), eq("re_teste_local"))).thenReturn(
                    new ClienteRecebedorPagarme.Resultado("re_teste_local", StatusPagarme.REFUSED, null, List.of()));
            chamar(HttpMethod.POST, rota + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusProvedor").value("REFUSED"))
                    .andExpect(jsonPath("$.podeReenviar").value(false));
            chamar(HttpMethod.POST, rota + "/reenvio", trabalhador.token(),
                    Map.of("ambiente", "DEV", "versao", 1, "tentativa", tentativaAtual))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.orientacao").value(containsString("recusou")));
            chamar(HttpMethod.PUT, devMode, tokenPlataforma, Map.of("devMode", false)).andExpect(status().isOk());
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.ambiente").value("PROD")).andExpect(jsonPath("$.situacao").value("NAO_ENVIADO"));
            chamar(HttpMethod.POST, rota, trabalhador.token(), Map.of("ambiente", "PROD", "dados", dados))
                    .andExpect(status().isServiceUnavailable());
            verify(recebedorPagarme, times(2)).criar(any(), any(), any());
        } finally {
            jdbc.update("delete from credenciamento_recebedor_externo where id_credenciamento in "
                    + "(select id from credenciamento_empresarial where id_solicitante in (?, ?))", trabalhador.id(), outro.id());
            jdbc.update("delete from credenciamento_empresarial where id_solicitante in (?, ?)", trabalhador.id(), outro.id());
            jdbc.update("update configuracao_sistema set valor='true' where servico='SISTEMA' and chave='DEV_MODE'");
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave like 'stone.%'");
        }
    }

    @Test
    void editaDadosSubmetidosPorVersaoSemRecriarRecebedor() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        String envio = rota + "/recebedor";
        var trabalhador = contaCredenciamento();
        var outro = contaCredenciamento();
        for (var pessoa : List.of(trabalhador, outro)) {
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", pessoa.token(), Map.of("selecionado", true))
                    .andExpect(status().isOk());
        }
        String cpf = cpfTeste();
        var endereco = Map.of("cep", "01001-000", "logradouro", "Rua de teste", "numero", "1", "complemento", "Bloco",
                "bairro", "Bairro", "cidade", "Cidade", "uf", "SP", "pontoReferencia", "Referência de teste");
        var pessoa = new java.util.HashMap<>(Map.ofEntries(Map.entry("nome", "Pessoa de teste"), Map.entry("cpf", cpf),
                Map.entry("email", "pessoa@example.invalid"), Map.entry("telefone", "34999999999"),
                Map.entry("tipoTelefone", "CELULAR"), Map.entry("nascimento", "2000-01-01"), Map.entry("profissao", "Teste"),
                Map.entry("rendaMensal", "100.10"), Map.entry("endereco", endereco), Map.entry("siteUrl", "https://example.invalid")));
        var banco = new java.util.HashMap<>(Map.of("titular", "Pessoa de teste", "documento", cpf, "tipoTitular", "INDIVIDUAL",
                "tipo", "CORRENTE", "banco", "001", "agencia", "0001", "conta", "000007", "digitoConta", "0"));
        var dados = Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", pessoa, "contaBancaria", banco);
        var pedido = Map.of("ambiente", "DEV", "dados", dados);
        var pendente = new ClienteRecebedorPagarme.Resultado("re_teste_local", StatusPagarme.REGISTRATION, null, List.of());
        var aprovado = new ClienteRecebedorPagarme.Resultado("re_teste_local", StatusPagarme.ACTIVE, null, List.of());
        var incerto = new ClienteRecebedorPagarme.Resultado(null, null, ErroRecebedor.RESPOSTA_INDETERMINADA, List.of());
        try {
            chamar(HttpMethod.PUT, ADMIN + "/configuracoes/servicos/STONE", tokenPlataforma, Map.of("valores", Map.of(
                    "stone.dev.accountId", "conta-dev-teste", "stone.dev.publicKey", "publica-dev-teste",
                    "stone.dev.secretKey", "sk_test_local", "stone.dev.recipientMaster", "recebedor-dev-teste")))
                    .andExpect(status().isOk());
            chamar(HttpMethod.POST, rota, trabalhador.token(), dados).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.versao").value(0))
                    .andExpect(jsonPath("$.recebedor.situacao").value("NAO_ENVIADO"));
            chamar(HttpMethod.GET, rota, outro.token(), null).andExpect(status().isNotFound());
            chamar(HttpMethod.PUT, rota, outro.token(), dados).andExpect(status().isNotFound());
            when(recebedorPagarme.criar(any(), any(), any())).thenAnswer(chamada -> {
                chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk())
                        .andExpect(jsonPath("$.dados.contaBancaria.conta").value("000007"))
                        .andExpect(jsonPath("$.recebedor.situacao").value("EM_ANALISE"));
                banco.put("conta", "000008");
                chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 0, null).andExpect(status().isConflict())
                        .andExpect(jsonPath("$.codigo").value("CONFIRMAR_ALTERACAO_CREDENCIAMENTO"))
                        .andExpect(jsonPath("$.aprovado").value(false));
                chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 0, "SUBMISSAO").andExpect(status().isOk())
                        .andExpect(jsonPath("$.versao").value(1))
                        .andExpect(jsonPath("$.recebedor.situacao").value("AGUARDANDO_NOVA_SUBMISSAO"))
                        .andExpect(jsonPath("$.recebedor.podeEnviar").value(false));
                return pendente;
            });
            chamarVersionado(HttpMethod.POST, envio, trabalhador.token(), pedido, 0, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("AGUARDANDO_NOVA_SUBMISSAO"))
                    .andExpect(jsonPath("$.statusProvedor").doesNotExist())
                    .andExpect(jsonPath("$.versao").value(1));
            verify(recebedorPagarme, never()).alterar(any(), anyString(), any(), any(), any());

            UUID tentativaInicial = jdbc.queryForObject("select tentativa_ref from credenciamento_recebedor_externo", UUID.class);
            var reenvio = Map.of("ambiente", "DEV", "versao", 1, "tentativa", tentativaInicial);
            chamar(HttpMethod.POST, envio + "/reenvio", trabalhador.token(), reenvio).andExpect(status().isOk())
                    .andExpect(jsonPath("$.podeReenviar").value(false)).andExpect(jsonPath("$.orientacao").value(containsString("Aguarde")));
            when(recebedorPagarme.consultar(any(), anyString(), eq("re_teste_local"))).thenReturn(aprovado);
            chamar(HttpMethod.POST, envio + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.podeReenviar").value(true))
                    .andExpect(jsonPath("$.statusProvedor").doesNotExist());

            when(recebedorPagarme.alterar(any(), eq("re_teste_local"), any(),
                    eq(br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity.Operacao.ATUALIZACAO_BANCARIA), any()))
                    .thenReturn(incerto);
            when(recebedorPagarme.consultar(any(), anyString(), eq("re_teste_local"))).thenAnswer(chamada -> {
                chamar(HttpMethod.POST, envio + "/reenvio", trabalhador.token(), reenvio).andExpect(status().isOk())
                        .andExpect(jsonPath("$.situacao").value("INDETERMINADO"));
                return pendente;
            });
            chamar(HttpMethod.POST, envio + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("INDETERMINADO"));
            chamar(HttpMethod.POST, envio + "/reenvio", trabalhador.token(), reenvio).andExpect(status().isOk())
                    .andExpect(jsonPath("$.situacao").value("INDETERMINADO"));
            verify(recebedorPagarme, times(1)).alterar(any(), anyString(), any(), any(), any());
            when(recebedorPagarme.consultarAtualizacao(any(), eq("re_teste_local"), any(), any()))
                    .thenReturn(aprovado, pendente, aprovado);
            chamar(HttpMethod.POST, envio + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusProvedor").doesNotExist());
            chamar(HttpMethod.POST, envio + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusProvedor").value("REGISTRATION"));
            chamar(HttpMethod.POST, envio + "/consulta", trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusProvedor").value("ACTIVE"));

            pessoa.put("telefone", "+55 (34) 99999-9999");
            pessoa.put("rendaMensal", "100.1");
            pessoa.put("nome", "  Pessoa  de teste ");
            chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 1, null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.versao").value(1))
                    .andExpect(jsonPath("$.recebedor.statusProvedor").value("ACTIVE"));
            pessoa.put("profissao", "Outra atividade");
            chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 1, null).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.aprovado").value(true));
            chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 1, "SUBMISSAO").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.aprovado").value(true));
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.recebedor.statusProvedor").value("ACTIVE"))
                    .andExpect(jsonPath("$.dados.pessoaFisica.profissao").value("Teste"));
            chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 1, "APROVACAO").andExpect(status().isOk())
                    .andExpect(jsonPath("$.versao").value(2))
                    .andExpect(jsonPath("$.recebedor.situacao").value("AGUARDANDO_NOVA_SUBMISSAO"));
            chamarVersionado(HttpMethod.PUT, rota, trabalhador.token(), dados, 1, "APROVACAO").andExpect(status().isConflict());
            UUID tentativaAtual = jdbc.queryForObject("select tentativa_ref from credenciamento_recebedor_externo", UUID.class);
            chamar(HttpMethod.POST, envio + "/reenvio", trabalhador.token(),
                    Map.of("ambiente", "DEV", "versao", 2, "tentativa", tentativaAtual)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.podeReenviar").value(false))
                    .andExpect(jsonPath("$.orientacao").value(containsString("suporte")));
            verify(recebedorPagarme, times(1)).criar(any(), any(), any());
            verify(recebedorPagarme, times(1)).alterar(any(), anyString(), any(), any(), any());
            assertThat(jdbc.queryForList("""
                    select t.versao_submetida from credenciamento_recebedor_tentativa t
                    join credenciamento_empresarial c on c.id=t.id_credenciamento
                    where c.id_solicitante=? and t.id_recebedor=? and t.dados_protegidos like 'v1:%'
                    order by t.versao_submetida
                    """, Long.class, trabalhador.id(), "re_teste_local")).containsExactly(0L, 1L);
        } finally {
            jdbc.update("delete from credenciamento_recebedor_externo where id_credenciamento in "
                    + "(select id from credenciamento_empresarial where id_solicitante in (?, ?))", trabalhador.id(), outro.id());
            jdbc.update("delete from credenciamento_empresarial where id_solicitante in (?, ?)", trabalhador.id(), outro.id());
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave like 'stone.%'");
        }
    }

    @Test
    void excluiRascunhoProprioReiniciaComOutroIdEProtegeFormularioNovo() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        var trabalhador = contaCredenciamento();
        var outro = contaCredenciamento();
        for (var pessoa : List.of(trabalhador, outro)) {
            chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", pessoa.token(), Map.of("selecionado", true))
                    .andExpect(status().isOk());
        }
        var dados = Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", Map.of("nome", "Primeiro formulário"),
                "contaBancaria", Map.of("agencia", "0001", "conta", "000007"));
        try {
            var id = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(), dados).andExpect(status().isCreated()));
            var idOutro = idCredenciamento(chamar(HttpMethod.POST, rota, outro.token(), dados).andExpect(status().isCreated()));
            chamar(HttpMethod.DELETE, rota, null, null).andExpect(status().isUnauthorized());
            excluirFormulario(rota, outro.token(), id, 0).andExpect(status().isConflict());
            excluirFormulario(rota, trabalhador.token(), id, 1).andExpect(status().isConflict());
            chamar(HttpMethod.DELETE, rota, trabalhador.token(), null).andExpect(status().isConflict());
            chamar(HttpMethod.DELETE, rota + "/" + id, outro.token(), null).andExpect(status().isForbidden());
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.dados.contaBancaria.conta").value("000007"));
            excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
            excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isNotFound());
            assertThat(jdbc.queryForObject("select count(*) from credenciamento_empresarial where id=?", Long.class, id)).isZero();
            chamar(HttpMethod.GET, rota, outro.token(), null).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(idOutro.toString()));
            var novo = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(),
                    Map.of("modalidade", "CORPORATION", "empresa", Map.of("razaoSocial", "Novo formulário")))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.versao").value(0))
                    .andExpect(jsonPath("$.dados.pessoaFisica").doesNotExist())
                    .andExpect(jsonPath("$.dados.contaBancaria").doesNotExist()));
            assertThat(novo).isNotEqualTo(id);
            mvc.perform(request(HttpMethod.PUT, rota).header(HttpHeaders.AUTHORIZATION, "Bearer " + trabalhador.token())
                    .header("X-Credenciamento-Id", id).header("X-Versao-Credenciamento", 0)
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(dados)))
                    .andExpect(status().isConflict());
            excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isConflict());
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(novo.toString()));
            assertThat(jdbc.queryForObject("""
                    select count(*) from auditoria where acao='EXCLUIR_CREDENCIAMENTO' and id_usuario=?
                    """, Long.class, trabalhador.id())).isEqualTo(1);
            verifyNoInteractions(recebedorPagarme);
        } finally {
            jdbc.update("delete from credenciamento_empresarial where id_solicitante in (?, ?)", trabalhador.id(), outro.id());
        }
    }

    @Test
    void falhaDuranteExclusaoReverteRegistroEAuditoria() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        var trabalhador = contaCredenciamento();
        chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", trabalhador.token(), Map.of("selecionado", true))
                .andExpect(status().isOk());
        var id = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(),
                Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", Map.of("nome", "Rascunho")))
                .andExpect(status().isCreated()));
        jdbc.execute("""
                create function impedir_exclusao_teste() returns trigger language plpgsql as $$
                begin raise exception 'Falha controlada de exclusão'; end; $$
                """);
        jdbc.execute("create trigger impedir_exclusao_teste before delete on credenciamento_empresarial"
                + " for each row execute function impedir_exclusao_teste()");
        try {
            assertThatThrownBy(() -> excluirFormulario(rota, trabalhador.token(), id, 0)).hasStackTraceContaining("Falha controlada de exclusão");
            assertThat(jdbc.queryForObject("select count(*) from credenciamento_empresarial where id=?", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from auditoria where acao='EXCLUIR_CREDENCIAMENTO' and id_usuario=?",
                    Long.class, trabalhador.id())).isZero();
        } finally {
            jdbc.execute("drop trigger impedir_exclusao_teste on credenciamento_empresarial");
            jdbc.execute("drop function impedir_exclusao_teste()");
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", trabalhador.id());
        }
    }

    @Test
    void excluiFormularioDuranteEnvioPreservandoHistoricoESemAprovarNovoFormulario() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        var trabalhador = contaCredenciamento();
        chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", trabalhador.token(), Map.of("selecionado", true))
                .andExpect(status().isOk());
        var endereco = Map.of("cep", "01001-000", "logradouro", "Rua de teste", "numero", "1", "complemento", "Bloco",
                "bairro", "Bairro", "cidade", "Cidade", "uf", "SP", "pontoReferencia", "Referência de teste");
        var pessoa = Map.ofEntries(Map.entry("nome", "Pessoa de teste"), Map.entry("cpf", cpfTeste()),
                Map.entry("email", "pessoa@example.invalid"), Map.entry("telefone", "34999999999"),
                Map.entry("tipoTelefone", "CELULAR"), Map.entry("nascimento", "2000-01-01"), Map.entry("profissao", "Teste"),
                Map.entry("rendaMensal", "100.10"), Map.entry("endereco", endereco), Map.entry("siteUrl", "https://example.invalid"));
        var banco = Map.of("titular", "Pessoa de teste", "documento", cpfTeste(), "tipoTitular", "INDIVIDUAL",
                "tipo", "CORRENTE", "banco", "001", "agencia", "0001", "conta", "000007", "digitoConta", "0");
        var dados = Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", pessoa, "contaBancaria", banco);
        var novoId = new java.util.concurrent.atomic.AtomicReference<UUID>();
        try {
            chamar(HttpMethod.PUT, ADMIN + "/configuracoes/servicos/STONE", tokenPlataforma, Map.of("valores", Map.of(
                    "stone.dev.accountId", "conta-dev-teste", "stone.dev.secretKey", "sk_test_local"))).andExpect(status().isOk());
            UUID id = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(), dados).andExpect(status().isCreated()));
            when(recebedorPagarme.criar(any(), any(), any())).thenAnswer(chamada -> {
                var antes = jdbc.queryForObject("select dados_protegidos from credenciamento_empresarial where id=?", String.class, id);
                excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
                chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isNotFound());
                chamar(HttpMethod.GET, ADMIN + "/credenciamentos/" + id + "/payload?unidadeMonetaria=CENTAVOS", tokenPlataforma, null)
                        .andExpect(status().isNotFound());
                assertThat(jdbc.queryForObject("select excluido_em is not null from credenciamento_empresarial where id=?",
                        Boolean.class, id)).isTrue();
                var apagado = jdbc.queryForObject("select dados_protegidos from credenciamento_empresarial where id=?", String.class, id);
                assertThat(criptografia.descriptografar("credenciamento:" + id + ":" + trabalhador.id(), apagado)).isEqualTo("{}");
                assertThat(jdbc.queryForObject("select dados_protegidos from credenciamento_recebedor_tentativa where id_credenciamento=?",
                        String.class, id)).isEqualTo(antes);
                novoId.set(idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(), Map.of("modalidade", "INDIVIDUAL",
                        "pessoaFisica", Map.of("nome", "Novo processo"))).andExpect(status().isCreated())
                        .andExpect(jsonPath("$.dados.contaBancaria").doesNotExist())));
                return new ClienteRecebedorPagarme.Resultado("re_teste_historico", StatusPagarme.ACTIVE, null, List.of(),
                        new ClienteRecebedorPagarme.DiagnosticoHttp(200, Map.of(), "{}".getBytes(StandardCharsets.UTF_8), false, false));
            });
            chamar(HttpMethod.POST, rota + "/recebedor", trabalhador.token(), Map.of("ambiente", "DEV", "dados", dados))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("NAO_ENVIADO"))
                    .andExpect(jsonPath("$.statusProvedor").doesNotExist());
            assertThat(novoId.get()).isNotEqualTo(id);
            excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
            chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(novoId.get().toString()))
                    .andExpect(jsonPath("$.dados.pessoaFisica.nome").value("Novo processo"))
                    .andExpect(jsonPath("$.recebedor.situacao").value("NAO_ENVIADO"));
            assertThat(jdbc.queryForObject("select count(*) from credenciamento_recebedor_diagnostico where id_credenciamento=?", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select id_recebedor from credenciamento_recebedor_tentativa where id_credenciamento=?", String.class, id))
                    .isEqualTo("re_teste_historico");
            chamar(HttpMethod.POST, rota + "/recebedor", trabalhador.token(), Map.of("ambiente", "DEV", "dados", dados))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value(containsString("tentativa anterior")));
            assertThat(jdbc.queryForObject("select count(*) from credenciamento_empresarial where id_solicitante=? and excluido_em is null",
                    Long.class, trabalhador.id())).isEqualTo(1);
            verify(recebedorPagarme, times(1)).criar(any(), any(), any());
            verify(recebedorPagarme, never()).alterar(any(), anyString(), any(), any(), any());
        } finally {
            jdbc.update("delete from credenciamento_recebedor_externo where id_credenciamento in "
                    + "(select id from credenciamento_empresarial where id_solicitante=?)", trabalhador.id());
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", trabalhador.id());
            jdbc.update("update configuracao_sistema set valor='' where servico='STONE' and chave like 'stone.%'");
        }
    }

    @Test
    void exclusaoPreservaTentativasEmTodosOsEstadosDoProvedor() throws Exception {
        String rota = "/api/v1/app/credenciamentos/STONE";
        var trabalhador = contaCredenciamento();
        chamar(HttpMethod.PUT, "/api/v1/app/perfil/papeis/TRABALHADOR", trabalhador.token(), Map.of("selecionado", true))
                .andExpect(status().isOk());
        try {
            for (String situacao : List.of("EM_ANALISE", "INDETERMINADO", "FALHA", "CRIADO")) {
                var id = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(),
                        Map.of("modalidade", "INDIVIDUAL", "pessoaFisica", Map.of("nome", "Dados anteriores")))
                        .andExpect(status().isCreated()));
                UUID tentativa = UUID.randomUUID();
                String digest = UUID.randomUUID().toString().replace("-", "").repeat(2);
                boolean criado = situacao.equals("CRIADO");
                jdbc.update("""
                        insert into credenciamento_recebedor_externo
                            (id_credenciamento, provedor, ambiente, modalidade, documento_digest, situacao,
                             tentativa_ref, tentativa_em, id_recebedor, confirmado_em, versao_submetida, status_provedor)
                        values (?, 'STONE', 'DEV', 'INDIVIDUAL', ?, ?, ?, now(), ?, ?, 0, ?)
                        """, id, digest, situacao, tentativa, criado ? "re_fixture_" + tentativa : null,
                        criado ? java.sql.Timestamp.from(java.time.Instant.now()) : null, criado ? "ACTIVE" : null);
                jdbc.update("""
                        insert into credenciamento_recebedor_tentativa
                            (tentativa_ref, id_credenciamento, provedor, ambiente, versao_submetida, operacao,
                             tentativa_em, dados_protegidos, situacao, id_recebedor, confirmado_em, status_provedor)
                        select e.tentativa_ref, e.id_credenciamento, e.provedor, e.ambiente, e.versao_submetida,
                               'CRIACAO', e.tentativa_em, c.dados_protegidos, e.situacao, e.id_recebedor, e.confirmado_em, e.status_provedor
                        from credenciamento_recebedor_externo e join credenciamento_empresarial c on c.id=e.id_credenciamento
                        where e.id_credenciamento=?
                        """, id);
                excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
                excluirFormulario(rota, trabalhador.token(), id, 0).andExpect(status().isNoContent());
                chamar(HttpMethod.GET, rota, trabalhador.token(), null).andExpect(status().isNotFound());
                assertThat(jdbc.queryForObject("select situacao from credenciamento_recebedor_externo where id_credenciamento=?",
                        String.class, id)).isEqualTo(situacao);
                assertThat(jdbc.queryForObject("select count(*) from credenciamento_recebedor_tentativa where tentativa_ref=?",
                        Long.class, tentativa)).isEqualTo(1);
                var novo = idCredenciamento(chamar(HttpMethod.POST, rota, trabalhador.token(), Map.of()).andExpect(status().isCreated())
                        .andExpect(jsonPath("$.dados.pessoaFisica").doesNotExist())
                        .andExpect(jsonPath("$.recebedor.situacao").value("NAO_ENVIADO")));
                assertThat(novo).isNotEqualTo(id);
                excluirFormulario(rota, trabalhador.token(), novo, 0).andExpect(status().isNoContent());
            }
            verifyNoInteractions(recebedorPagarme);
        } finally {
            jdbc.update("delete from credenciamento_recebedor_externo where id_credenciamento in "
                    + "(select id from credenciamento_empresarial where id_solicitante=?)", trabalhador.id());
            jdbc.update("delete from credenciamento_empresarial where id_solicitante=?", trabalhador.id());
        }
    }

    private UUID idCredenciamento(ResultActions resultado) throws Exception {
        return UUID.fromString(json.readTree(resultado.andReturn().getResponse().getContentAsByteArray()).path("id").asText());
    }

    private ResultActions excluirFormulario(String rota, String token, UUID id, long versao) throws Exception {
        return mvc.perform(request(HttpMethod.DELETE, rota).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Credenciamento-Id", id).header("X-Versao-Credenciamento", versao));
    }

    private ResultActions chamarVersionado(HttpMethod metodo, String rota, String token, Map<String, ?> dados,
            long versao, String confirmacao) throws Exception {
        var requisicao = request(metodo, rota).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Versao-Credenciamento", versao)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(dados));
        if (confirmacao != null) requisicao.header("X-Confirmar-Alteracao-Credenciamento", confirmacao);
        return mvc.perform(requisicao);
    }

    private ContaCredenciamento contaCredenciamento() throws Exception {
        var dados = Map.of("email", UUID.randomUUID() + "@example.test", "senha", SENHA);
        var criado = chamar(HttpMethod.POST, "/api/v1/app/autenticacao/cadastro", null, dados)
                .andExpect(status().isCreated()).andReturn();
        var login = chamar(HttpMethod.POST, "/api/v1/app/autenticacao/login", null, dados)
                .andExpect(status().isOk()).andReturn();
        return new ContaCredenciamento(UUID.fromString(json.readTree(criado.getResponse().getContentAsByteArray()).path("id").asText()),
                json.readTree(login.getResponse().getContentAsByteArray()).path("tokenAcesso").asText());
    }

    private static String cpfTeste() {
        var cpf = new StringBuilder("900000001");
        for (int comprimento : new int[]{9, 10}) {
            int soma = 0;
            for (int i = 0; i < comprimento; i++) soma += (cpf.charAt(i) - '0') * (comprimento + 1 - i);
            int resto = soma % 11;
            cpf.append(resto < 2 ? 0 : 11 - resto);
        }
        return cpf.toString();
    }

    private record ContaCredenciamento(UUID id, String token) {}

    @Test
    void sincronizaAmbientesEPreservaAlteracoesDeFormularioDesatualizado() throws Exception {
        String app = "/api/v1/app";
        String consulta = app + "/perfil";
        String empresa = "PLATAFORMA_EMPRESA";
        String trabalhador = "PLATAFORMA_TRABALHADOR";
        String visitante = "PLATAFORMA_VISITANTE";
        String email = "sincronizacao-" + UUID.randomUUID() + "@example.test";
        var credenciais = Map.of("email", email, "senha", SENHA);
        var criado = chamar(HttpMethod.POST, app + "/autenticacao/cadastro", null, credenciais)
                .andExpect(status().isCreated()).andReturn();
        var id = UUID.fromString(json.readTree(criado.getResponse().getContentAsByteArray()).path("id").asText());
        var autenticado = chamar(HttpMethod.POST, app + "/autenticacao/login", null, credenciais)
                .andExpect(status().isOk()).andReturn();
        String tokenApp = json.readTree(autenticado.getResponse().getContentAsByteArray()).path("tokenAcesso").asText();
        String rotaAdmin = ADMIN + "/usuarios/" + id;
        var conta = chamar(HttpMethod.GET, rotaAdmin, tokenPlataforma, null).andExpect(status().isOk()).andReturn();
        String login = json.readTree(conta.getResponse().getContentAsByteArray()).path("login").asText();
        chamar(HttpMethod.PUT, rotaAdmin, tokenPlataforma, Map.of("login", login, "email", email, "status", "ATIVO",
                "permissoesPlataforma", List.of(empresa, trabalhador, visitante))).andExpect(status().isOk());
        chamar(HttpMethod.GET, consulta, tokenApp, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.papeis").value(org.hamcrest.Matchers.contains("EMPRESA", "TRABALHADOR")));
        chamar(HttpMethod.PUT, rotaAdmin, tokenPlataforma, Map.of("login", login, "email", email, "status", "ATIVO",
                "permissoesPlataforma", List.of(visitante))).andExpect(status().isOk());
        chamar(HttpMethod.GET, consulta, tokenApp, null).andExpect(status().isOk()).andExpect(jsonPath("$.papeis").isEmpty());

        for (String papel : List.of("EMPRESA", "TRABALHADOR")) {
            chamar(HttpMethod.PUT, consulta + "/papeis/" + papel, tokenApp, Map.of("selecionado", true))
                    .andExpect(status().isOk());
        }
        chamar(HttpMethod.GET, rotaAdmin, tokenPlataforma, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoesPlataforma").value(org.hamcrest.Matchers.hasItems(empresa, trabalhador, visitante)));
        chamar(HttpMethod.PUT, rotaAdmin, tokenPlataforma, Map.of("login", login, "email", email, "status", "ATIVO",
                "permissoesOriginais", List.of(empresa, visitante), "permissoesPlataforma", List.of(visitante)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoesPlataforma").value(org.hamcrest.Matchers.contains(trabalhador, visitante)));
        for (String papel : List.of("TRABALHADOR", "EMPRESA")) {
            chamar(HttpMethod.PUT, consulta + "/papeis/" + papel, tokenApp, Map.of("selecionado", false))
                    .andExpect(status().isOk());
        }
        chamar(HttpMethod.GET, rotaAdmin, tokenPlataforma, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoesPlataforma").value(org.hamcrest.Matchers.contains(visitante)));
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
    void criaListaEAtualizaNomeSemConfundirComLogin() throws Exception {
        String login = "teste-" + UUID.randomUUID();
        String email = login + "@example.test";
        var criado = criar(ADMIN + "/usuarios", Map.of(
                "login", login, "email", email, "senha", SENHA, "nome", "  Ana Cheff  "));
        UUID id = id(criado);
        usuarios.add(id);
        assertThat(criado.path("nome").asText()).isEqualTo("Ana Cheff");
        chamar(HttpMethod.GET, ADMIN + "/usuarios?login=" + login, tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].nome").value("Ana Cheff"));
        var rota = ADMIN + "/usuarios/" + id;
        chamar(HttpMethod.GET, rota, tokenPlataforma, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana Cheff"));
        chamar(HttpMethod.PUT, rota, tokenPlataforma, Map.of(
                "login", login, "email", email, "status", "ATIVO", "nome", "Ana Silva"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana Silva"));
        chamar(HttpMethod.PUT, rota, tokenPlataforma, Map.of("login", login, "email", email, "status", "ATIVO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana Silva"));
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
        if (rota.equals("/api/v1/app/autenticacao/cadastro")) {
            requisicao.with(pedido -> {
                pedido.setRemoteAddr("192.0.2." + ENDERECO_CADASTRO.getAndIncrement());
                return pedido;
            });
        }
        if (token != null) {
            requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (dados != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(dados));
        }
        return mvc.perform(requisicao);
    }
}
