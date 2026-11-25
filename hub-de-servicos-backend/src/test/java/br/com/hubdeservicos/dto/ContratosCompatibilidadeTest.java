package br.com.hubdeservicos.dto;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuarioBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AtualizarServicoBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.DadosBean;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Modalidade;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import br.com.hubdeservicos.service.credenciamento.ClienteRecebedorPagarme.DiagnosticoHttpBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import jakarta.validation.Validation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compara contratos sintéticos capturados antes da migração com as classes em
 * execução.
 */
class ContratosCompatibilidadeTest {
    private static final ObjectMapper JSON;
    static {
        var objectMapperPreparado = new ObjectMapper();
        JSON = objectMapperPreparado.findAndRegisterModules();
    }
    private static final String RECURSO = "contratos/baseline-java.json";

    @Test
    void preservaJsonValidacaoESchemasDasFamiliasDeContratos() throws Exception {
        try (var entrada = getClass().getClassLoader().getResourceAsStream(RECURSO);
                var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(entrada).isNotNull();
            var baseline = JSON.readTree(entrada);
            for (var contrato : baseline) {
                if (!contrato.path("classeAtual").asText().matches(System.getProperty("hub.contratos.filtro", ".*"))) {
                    continue;
                }
                var tipo = Class.forName(contrato.path("classeAtual").asText());
                for (boolean vazio : List.of(false, true)) {
                    Object objeto = instanciar(tipo, vazio, 0);
                    String variante = vazio ? "nulo" : "preenchido";
                    assertThat(JSON.readTree(JSON.writeValueAsBytes(objeto))).as("JSON %s %s", tipo.getName(), variante)
                            .isEqualTo(contrato.path(variante));
                    var violacoes = factory.getValidator().validate(objeto).stream()
                            .map(v -> v.getPropertyPath() + ":" + v.getMessage()).sorted().toList();
                    assertThat(JSON.<JsonNode>valueToTree(violacoes)).as("Validação %s %s", tipo.getName(), variante)
                            .isEqualTo(contrato.path(variante + "Violacoes"));
                }
                var schemas = JSON.readTree(JSON.writeValueAsBytes(ModelConverters.getInstance().readAll(tipo)));
                assertThat(schemaPublicado(schemas)).as("Schemas %s", tipo.getName())
                        .isEqualTo(contrato.path("schemas"));
            }
        }
    }

    @Test
    void beansMantemAcessoPorGettersSettersEIgualdadePorValor() throws Exception {
        try (var entrada = getClass().getClassLoader().getResourceAsStream(RECURSO)) {
            for (var contrato : JSON.readTree(entrada)) {
                Class<?> tipo = Class.forName(contrato.path("classeAtual").asText());
                Object original = instanciar(tipo, false, 0);
                var construtor = tipo.getDeclaredConstructor();
                construtor.setAccessible(true);
                Object copia = construtor.newInstance();
                for (var campo : tipo.getDeclaredFields()) {
                    if (Modifier.isStatic(campo.getModifiers())) {
                        continue;
                    }
                    String sufixo = Character.toUpperCase(campo.getName().charAt(0)) + campo.getName().substring(1);
                    Method getter = tipo.getDeclaredMethod("get" + sufixo);
                    Method setter = tipo.getDeclaredMethod("set" + sufixo, campo.getType());
                    getter.setAccessible(true);
                    setter.setAccessible(true);
                    setter.invoke(copia, getter.invoke(original));
                }
                assertThat(copia).as("Igualdade %s", tipo.getName()).isEqualTo(original);
                assertThat(copia.hashCode()).as("Hash %s", tipo.getName()).isEqualTo(original.hashCode());
                Set<Object> conjunto = new HashSet<>();
                conjunto.add(original);
                assertThat(conjunto).contains(copia);
                assertThat(JSON.readTree(JSON.writeValueAsBytes(copia)))
                        .isEqualTo(JSON.readTree(JSON.writeValueAsBytes(original)));
                if (!tipo.getSimpleName().equals("DadosBean")) {
                    assertThat(tipo.getDeclaredConstructors()).allMatch(c -> c.getParameterCount() == 0);
                }
            }
        }
    }

    @Test
    void leituraLegadaInfereModalidadeDepoisDeTodosOsBlocosIndependentementeDaOrdem() throws Exception {
        Class<?> tipo = Class.forName("br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento$DadosBean");
        for (String documento : List.of("{\"modalidade\":null,\"empresa\":{\"razaoSocial\":\"Teste\"}}",
                "{\"empresa\":{\"razaoSocial\":\"Teste\"},\"modalidade\":null}")) {
            Object dados = JSON.readValue(documento, tipo);
            Method getter = tipo.getDeclaredMethod("getModalidade");
            assertThat(getter.invoke(dados).toString()).isEqualTo("CORPORATION");
        }
    }

    @Test
    void preservaValidacaoDosElementosDeListasEMapasSemExporValoresWriteOnly() throws Exception {
        NovoUsuarioBean usuario = new NovoUsuarioBean();
        usuario.setPermissoesPlataforma(List.of("", "p".repeat(101)));

        AtualizarServicoBean atualizacao = new AtualizarServicoBean();
        atualizacao.setValores(Map.of("", "s".repeat(2049)));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violacoesUsuario = factory.getValidator().validate(usuario).stream()
                    .map(violacao -> violacao.getPropertyPath().toString()).toList();
            assertThat(violacoesUsuario).anyMatch(campo -> campo.startsWith("permissoesPlataforma[0]"))
                    .anyMatch(campo -> campo.startsWith("permissoesPlataforma[1]"));

            var violacoesMapa = factory.getValidator().validate(atualizacao);
            assertThat(violacoesMapa).hasSize(2);
            assertThat(JSON.readTree(JSON.writeValueAsBytes(atualizacao)).has("valores")).isFalse();
            assertThat(atualizacao.toString()).isEqualTo("AtualizarServico");
        }
    }

    @Test
    void recuperaJsonLegadoProtegidoMantendoInferenciaCamposIgnoradosEZerosBancarios() throws Exception {
        // A chave e os dados são exclusivos deste teste; nenhum registro real é lido ou
        // regravado.
        byte[] chave = new byte[32];
        SecureRandom aleatorio = new SecureRandom();
        aleatorio.nextBytes(chave);
        String chaveTeste = Base64.getEncoder().encodeToString(chave);
        CriptografiaCredenciais protecao = new CriptografiaCredenciais(chaveTeste);
        String legado = """
                {
                  "empresa": {"razaoSocial": "Exemplo sintetico", "faturamento": "12.34"},
                  "representante": {"nomeMae": "ignorado", "mother_name": "ignorado"},
                  "contaBancaria": {"banco": "001", "agencia": "0001", "conta": "0000123"}
                }
                """;
        String contexto = "caracterizacao:cadastro";
        String cifrado = protecao.criptografar(contexto, legado);
        String recuperado = protecao.descriptografar(contexto, cifrado);
        DadosBean dados = JSON.readValue(recuperado, DadosBean.class);

        assertThat(cifrado).doesNotContain("razaoSocial", "0000123");
        assertThat(dados.getModalidade()).isEqualTo(Modalidade.CORPORATION);
        assertThat(dados.getEmpresa().getFaturamento()).isEqualByComparingTo("12.34");
        assertThat(dados.getEmpresa().getFaturamentoAnual()).isNull();
        assertThat(dados.getContaBancaria().getBanco()).isEqualTo("001");
        assertThat(dados.getContaBancaria().getAgencia()).isEqualTo("0001");
        assertThat(dados.getContaBancaria().getConta()).isEqualTo("0000123");
        assertThat(JSON.writeValueAsString(dados)).doesNotContain("nomeMae", "mother_name");
        assertThat(dados.toString()).isEqualTo("DadosCredenciamento");
    }

    @Test
    void diagnosticoMantemSemanticaDeArrayPorReferenciaESemCorpoNoToString() {
        byte[] corpo = "resposta-sintetica".getBytes(StandardCharsets.UTF_8);
        DiagnosticoHttpBean original = new DiagnosticoHttpBean();
        original.setStatusHttp(412);
        original.setCorpo(corpo);

        DiagnosticoHttpBean mesmaReferencia = new DiagnosticoHttpBean();
        mesmaReferencia.setStatusHttp(412);
        mesmaReferencia.setCorpo(corpo);

        DiagnosticoHttpBean conteudoIgual = new DiagnosticoHttpBean();
        conteudoIgual.setStatusHttp(412);
        conteudoIgual.setCorpo(corpo.clone());

        assertThat(original.getCorpo()).isSameAs(corpo);
        assertThat(original).isEqualTo(mesmaReferencia).isNotEqualTo(conteudoIgual);
        assertThat(original.hashCode()).isEqualTo(mesmaReferencia.hashCode());
        assertThat(original.toString()).isEqualTo("DiagnosticoHttp[statusHttp=412]");
    }

    // O OpenAPI publicado omite atributos nulos do modelo; os exemplos JSON
    // continuam comparando null e ausência.
    private static JsonNode schemaPublicado(JsonNode no) {
        if (no.isObject()) {
            var resultado = JSON.createObjectNode();
            no.fields().forEachRemaining(campo -> {
                if (!campo.getValue().isNull()) {
                    resultado.set(campo.getKey(), schemaPublicado(campo.getValue()));
                }
            });
            return resultado;
        }
        if (no.isArray()) {
            var resultado = JSON.createArrayNode();
            no.forEach(item -> resultado.add(schemaPublicado(item)));
            return resultado;
        }
        return no;
    }

    private static Object instanciar(Class<?> tipo, boolean vazio, int profundidade) throws Exception {
        if (tipo.isRecord()) {
            var campos = tipo.getRecordComponents();
            var tipos = Arrays.stream(campos).map(RecordComponent::getType).toArray(Class<?>[]::new);
            var valores = new Object[campos.length];
            for (int i = 0; i < campos.length; i++) {
                valores[i] = valor(campos[i].getGenericType(), vazio, profundidade + 1);
            }
            var construtor = tipo.getDeclaredConstructor(tipos);
            construtor.setAccessible(true);
            Object objeto = construtor.newInstance(valores);
            return objeto;
        }
        var construtor = tipo.getDeclaredConstructor();
        construtor.setAccessible(true);
        Object objeto = construtor.newInstance();
        for (var campo : tipo.getDeclaredFields()) {
            if (Modifier.isStatic(campo.getModifiers())) {
                continue;
            }
            campo.setAccessible(true);
            campo.set(objeto, valor(campo.getGenericType(), vazio, profundidade + 1));
        }
        return objeto;
    }

    private static Object valor(Type tipo, boolean vazio, int profundidade) throws Exception {
        if (tipo instanceof ParameterizedType p) {
            tipo = p.getRawType();
        }
        if (!(tipo instanceof Class<?> classe)) {
            return null;
        }
        if (vazio && !classe.isPrimitive()) {
            return null;
        }
        if (classe == String.class) {
            return "valor-sintetico";
        }
        if (classe == boolean.class || classe == Boolean.class) {
            return !vazio;
        }
        if (classe == int.class || classe == Integer.class) {
            return vazio ? 0 : 3;
        }
        if (classe == long.class || classe == Long.class) {
            return vazio ? 0L : 7L;
        }
        if (classe == BigDecimal.class) {
            var bigDecimalPreparado = new BigDecimal("12.34");
            return bigDecimalPreparado;
        }
        if (classe == UUID.class) {
            return UUID.fromString("11111111-1111-4111-8111-111111111111");
        }
        if (classe == LocalDate.class) {
            return LocalDate.parse("1985-04-03");
        }
        if (classe == Instant.class) {
            return Instant.parse("2026-01-01T00:00:00Z");
        }
        if (classe == byte[].class) {
            var arrayPreparado = new byte[] { 1, 2 };
            return arrayPreparado;
        }
        if (List.class.isAssignableFrom(classe)) {
            return List.of();
        }
        if (Map.class.isAssignableFrom(classe)) {
            return Map.of();
        }
        if (classe.isEnum()) {
            return classe.getEnumConstants()[0];
        }
        if (classe.isSealed() && classe.isInterface()) {
            return instanciar(classe.getPermittedSubclasses()[0], vazio, profundidade);
        }
        if (classe.getPackageName().startsWith("br.com.hubdeservicos") && profundidade < 6
                && !classe.getName().contains(".entity.")) {
            return instanciar(classe, vazio, profundidade);
        }
        return null;
    }

}
