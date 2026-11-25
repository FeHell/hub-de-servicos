package br.com.hubdeservicos.service.credenciamento;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ErroRecebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.StatusPagarme;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity.Operacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClienteRecebedorPagarme {
    static final String RECEBEDORES = "https://api.pagar.me/core/v5/recipients";
    private static final Logger log = LoggerFactory.getLogger(ClienteRecebedorPagarme.class);
    private static final int TAMANHO_PAGINA = 50;
    private static final int LIMITE_PAGINAS = 3;
    private static final int LIMITE_CORPO_DIAGNOSTICO = 32 * 1024;
    private static final List<String> HEADERS_RASTREIO;
    static {

        HEADERS_RASTREIO = List.of("x-request-id", "request-id", "x-correlation-id", "correlation-id", "traceparent",
                "x-amzn-requestid", "cf-ray");
    }
    private static final String ID_RASTREIO = "(?i)(?:[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}|[0-9a-f]{16,64}(?:-[a-z]{3})?)";
    private static final String TRACEPARENT = "(?i)[0-9a-f]{2}-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}";

    private static final Map<String, String> CAMPOS;
    static {

        CAMPOS = Map.ofEntries(Map.entry("register_information.name", "pessoaFisica.nome"),
                Map.entry("register_information.document", "pessoaFisica.cpf"),
                Map.entry("register_information.email", "pessoaFisica.email"),
                Map.entry("register_information.site_url", "pessoaFisica.siteUrl"),
                Map.entry("register_information.birthdate", "pessoaFisica.nascimento"),
                Map.entry("register_information.monthly_income", "pessoaFisica.rendaMensal"),
                Map.entry("register_information.professional_occupation", "pessoaFisica.profissao"),
                Map.entry("register_information.phone_numbers", "pessoaFisica.telefone"),
                Map.entry("register_information.phone_numbers.ddd", "pessoaFisica.telefone"),
                Map.entry("register_information.phone_numbers.number", "pessoaFisica.telefone"),
                Map.entry("register_information.phone_numbers.type", "pessoaFisica.tipoTelefone"),
                Map.entry("register_information.address.street", "pessoaFisica.endereco.logradouro"),
                Map.entry("register_information.address.street_number", "pessoaFisica.endereco.numero"),
                Map.entry("register_information.address.complementary", "pessoaFisica.endereco.complemento"),
                Map.entry("register_information.address.neighborhood", "pessoaFisica.endereco.bairro"),
                Map.entry("register_information.address.city", "pessoaFisica.endereco.cidade"),
                Map.entry("register_information.address.state", "pessoaFisica.endereco.uf"),
                Map.entry("register_information.address.zip_code", "pessoaFisica.endereco.cep"),
                Map.entry("register_information.address.reference_point", "pessoaFisica.endereco.pontoReferencia"),
                Map.entry("default_bank_account.holder_name", "contaBancaria.titular"),
                Map.entry("default_bank_account.holder_type", "contaBancaria.tipoTitular"),
                Map.entry("default_bank_account.holder_document", "contaBancaria.documento"),
                Map.entry("default_bank_account.bank", "contaBancaria.banco"),
                Map.entry("default_bank_account.branch_number", "contaBancaria.agencia"),
                Map.entry("default_bank_account.branch_check_digit", "contaBancaria.digitoAgencia"),
                Map.entry("default_bank_account.account_number", "contaBancaria.conta"),
                Map.entry("default_bank_account.account_check_digit", "contaBancaria.digitoConta"),
                Map.entry("default_bank_account.type", "contaBancaria.tipo"));
    }

    private final RestClient cliente;
    private final ObjectMapper json;

    @Autowired
    public ClienteRecebedorPagarme(ObjectMapper json) {
        this(criarCliente(), json);
    }

    ClienteRecebedorPagarme(RestClient cliente, ObjectMapper json) {
        this.cliente = cliente;
        this.json = json;
    }

    private static RestClient criarCliente() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(factory).build();
    }

    public ResultadoBean criar(AutenticacaoPagarme.ContaVO conta, PagarmeRecebedorBean payload, UUID tentativa) {

        try {
            try {
                log.info("Requisição Pagar.me: tentativa={} ambiente={} método=POST URL={} payload=\n{}", tentativa,
                        conta.getAmbiente(), RECEBEDORES,
                        json.writerWithDefaultPrettyPrinter().writeValueAsString(payload));
            } catch (com.fasterxml.jackson.core.JsonProcessingException erro) {
                log.warn("Não foi possível imprimir o payload Pagar.me da tentativa {}", tentativa);
            }

            return cliente.post().uri(RECEBEDORES).headers(conta::aplicarAutenticacao)
                    .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).body(payload)
                    .exchange((requisicao, resposta) -> {

                        int http = resposta.getStatusCode().value();

                        if (http == 200 || http == 201) {

                            var diagnostico = capturarResposta(http, resposta);

                            var resultado = confirmar(lerCorpo(diagnostico), payload.getCode(), null);

                            return resultado.getId() == null ? resultado.comDiagnostico(diagnostico) : resultado;
                        }

                        log.warn("Pagar.me recusou ou não confirmou a tentativa {} (HTTP {})", tentativa, http);

                        var diagnostico = capturarResposta(http, resposta);

                        if (http == 401 || http == 403) {
                            return ResultadoBean.falha(ErroRecebedor.AUTENTICACAO, List.of())
                                    .comDiagnostico(diagnostico);
                        }

                        if (http == 400 || http == 422) {

                            var campos = camposRecusados(lerCorpo(diagnostico));

                            if (!campos.isEmpty()) {
                                return ResultadoBean.falha(ErroRecebedor.DADOS_RECUSADOS, campos)
                                        .comDiagnostico(diagnostico);
                            }
                        }

                        return ResultadoBean.indeterminado().comDiagnostico(diagnostico);

                    });

        } catch (RestClientException erro) {

            log.warn("Resposta da Pagar.me indisponível para a tentativa {}", tentativa);

            return ResultadoBean.indeterminado();
        }
    }

    private static DiagnosticoHttpBean capturarResposta(int http, ClientHttpResponse resposta) {
        var headers = new LinkedHashMap<String, String>();
        for (String nome : HEADERS_RASTREIO) {
            String valor = resposta.getHeaders().getFirst(nome);
            if (valor != null && valor.matches(nome.equals("traceparent") ? TRACEPARENT : ID_RASTREIO)) {
                headers.put(nome, valor);
            }
        }
        try {
            byte[] corpo = resposta.getBody().readNBytes(LIMITE_CORPO_DIAGNOSTICO + 1);
            boolean truncado = corpo.length > LIMITE_CORPO_DIAGNOSTICO;
            DiagnosticoHttpBean diagnosticoHttpPreparado = new DiagnosticoHttpBean();
            diagnosticoHttpPreparado.setStatusHttp(http);
            diagnosticoHttpPreparado.setCabecalhos(Map.copyOf(headers));
            diagnosticoHttpPreparado.setCorpo(truncado ? Arrays.copyOf(corpo, LIMITE_CORPO_DIAGNOSTICO) : corpo);
            diagnosticoHttpPreparado.setCorpoTruncado(truncado);
            diagnosticoHttpPreparado.setCorpoIndisponivel(false);
            return diagnosticoHttpPreparado;
        } catch (IOException erro) {
            DiagnosticoHttpBean diagnosticoHttpPreparado2 = new DiagnosticoHttpBean();
            diagnosticoHttpPreparado2.setStatusHttp(http);
            diagnosticoHttpPreparado2.setCabecalhos(Map.copyOf(headers));
            diagnosticoHttpPreparado2.setCorpo(new byte[0]);
            diagnosticoHttpPreparado2.setCorpoTruncado(false);
            diagnosticoHttpPreparado2.setCorpoIndisponivel(true);
            return diagnosticoHttpPreparado2;
        }
    }

    private JsonNode lerCorpo(DiagnosticoHttpBean diagnostico) {
        if (diagnostico.getCorpoTruncado() || diagnostico.getCorpoIndisponivel()) {
            return null;
        }
        try {
            return json.readTree(diagnostico.getCorpo());
        } catch (IOException erro) {
            return null;
        }
    }

    public ResultadoBean consultar(AutenticacaoPagarme.ContaVO conta, String codigo, String id) {
        try {
            if (id != null) {
                var resposta = cliente.get().uri(RECEBEDORES + "/{id}", id).headers(conta::aplicarAutenticacao)
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange((requisicao, recebido) -> recebido.getStatusCode().is2xxSuccessful()
                                ? json.readTree(recebido.getBody())
                                : null);
                return confirmar(resposta, codigo, id);
            }
            for (int pagina = 1; pagina <= LIMITE_PAGINAS; pagina++) {
                var resposta = cliente.get().uri(RECEBEDORES + "?page={pagina}&size={tamanho}", pagina, TAMANHO_PAGINA)
                        .headers(conta::aplicarAutenticacao).accept(MediaType.APPLICATION_JSON)
                        .exchange((requisicao, recebido) -> recebido.getStatusCode().is2xxSuccessful()
                                ? json.readTree(recebido.getBody())
                                : null);
                if (resposta == null || !resposta.path("data").isArray()) {
                    break;
                }
                for (JsonNode item : resposta.path("data")) {
                    if (codigo.equals(item.path("code").asText())) {
                        return confirmar(item, codigo, null);
                    }
                }
                if (resposta.path("data").size() < TAMANHO_PAGINA
                        && !resposta.path("paging").path("next").isTextual()) {
                    break;
                }
            }
        } catch (RestClientException erro) {
            log.warn("Consulta de confirmação da Pagar.me indisponível para a referência {}", codigo);
        }
        return ResultadoBean.indeterminado();
    }

    public ResultadoBean alterar(AutenticacaoPagarme.ContaVO conta, String id, PagarmeRecebedorBean payload,
            Operacao operacao, UUID tentativa) {
        if (operacao != Operacao.ATUALIZACAO_BANCARIA) {
            return ResultadoBean.falha(ErroRecebedor.ATUALIZACAO_RESTRITA, List.of());
        }
        JsonNode atual;
        try {
            atual = obter(conta, id);
        } catch (RestClientException erro) {
            return ResultadoBean.falha(ErroRecebedor.ATUALIZACAO_RESTRITA, List.of());
        }
        var recebedor = confirmar(atual, payload.getCode(), id);
        if (recebedor.getId() == null || recebedor.getStatus() != StatusPagarme.ACTIVE) {
            return ResultadoBean.falha(ErroRecebedor.ATUALIZACAO_RESTRITA, List.of());
        }
        if (!documentoCorresponde(atual, payload)) {
            return ResultadoBean.falha(ErroRecebedor.ATUALIZACAO_RESTRITA, List.of());
        }
        if (refleteAtualizacao(atual, payload)) {
            return recebedor;
        }
        Object corpo = Map.of("bank_account", payload.getDefaultBankAccount());
        String rota = RECEBEDORES + "/{id}/default-bank-account";
        try {
            var erro = cliente.method(HttpMethod.PATCH).uri(rota, id).headers(conta::aplicarAutenticacao)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).body(corpo)
                    .exchange((requisicao, resposta) -> {
                        int http = resposta.getStatusCode().value();
                        if (resposta.getStatusCode().is2xxSuccessful()) {
                            return null;
                        }
                        var diagnostico = capturarResposta(http, resposta);
                        log.warn("Pagar.me não confirmou a atualização da tentativa {} (HTTP {})", tentativa, http);
                        var resultado = http == 400 || http == 401 || http == 403 || http == 422
                                ? ResultadoBean.falha(ErroRecebedor.ATUALIZACAO_RESTRITA,
                                        camposRecusados(lerCorpo(diagnostico)))
                                : ResultadoBean.indeterminado();
                        return resultado.comDiagnostico(diagnostico);
                    });
            return erro != null ? erro : consultarAtualizacao(conta, id, payload, operacao);
        } catch (RestClientException erro) {
            log.warn("Resposta da atualização Pagar.me indisponível para a tentativa {}", tentativa);
            return ResultadoBean.indeterminado();
        }
    }

    public ResultadoBean consultarAtualizacao(AutenticacaoPagarme.ContaVO conta, String id,
            PagarmeRecebedorBean payload, Operacao operacao) {
        // Operações cadastrais antigas permanecem no histórico, sem reutilizar o campo
        // removido.
        if (operacao != Operacao.ATUALIZACAO_BANCARIA) {
            return ResultadoBean.indeterminado();
        }
        try {
            var resposta = obter(conta, id);
            return resposta != null && documentoCorresponde(resposta, payload) && refleteAtualizacao(resposta, payload)
                    ? confirmar(resposta, payload.getCode(), id)
                    : ResultadoBean.indeterminado();
        } catch (RestClientException erro) {
            return ResultadoBean.indeterminado();
        }
    }

    private JsonNode obter(AutenticacaoPagarme.ContaVO conta, String id) {
        return cliente.get().uri(RECEBEDORES + "/{id}", id).headers(conta::aplicarAutenticacao)
                .accept(MediaType.APPLICATION_JSON)
                .exchange((requisicao, resposta) -> resposta.getStatusCode().is2xxSuccessful()
                        ? json.readTree(resposta.getBody())
                        : null);
    }

    private static boolean documentoCorresponde(JsonNode resposta, PagarmeRecebedorBean payload) {
        String documento = resposta.path("register_information").path("document")
                .asText(resposta.path("document").asText());
        return payload.getDefaultBankAccount().getHolderDocument().equals(ValidacaoCredenciamento.documento(documento));
    }

    private boolean refleteAtualizacao(JsonNode resposta, PagarmeRecebedorBean payload) {
        var conta = resposta.path("default_bank_account");
        var esperada = json.valueToTree(payload.getDefaultBankAccount());
        return conta.isObject()
                && StreamSupport.stream(((Iterable<String>) esperada::fieldNames).spliterator(), false)
                        .allMatch(campo -> campo.equals("holder_document") && !conta.hasNonNull(campo)
                                || esperada.path(campo).asText().equalsIgnoreCase(conta.path(campo).asText("").strip()))
                && (esperada.has("branch_check_digit") || conta.path("branch_check_digit").asText("").isBlank());
    }

    private static ResultadoBean confirmar(JsonNode resposta, String codigo, String idEsperado) {
        if (resposta == null) {
            return ResultadoBean.indeterminado();
        }
        String id = resposta.path("id").asText();
        String codigoRetornado = resposta.path("code").asText("");
        if (!id.matches("(?:re|rp)_[A-Za-z0-9_-]{1,125}") || (idEsperado != null && !idEsperado.equals(id))
                || (!codigoRetornado.isEmpty() && !codigo.equals(codigoRetornado))) {
            return ResultadoBean.indeterminado();
        }
        StatusPagarme status;
        try {
            status = StatusPagarme.valueOf(resposta.path("status").asText().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignorado) {
            status = null;
        }
        ResultadoBean resultadoPreparado = new ResultadoBean();
        resultadoPreparado.setId(id);
        resultadoPreparado.setStatus(status);
        resultadoPreparado.setErro(null);
        resultadoPreparado.setCampos(List.of());
        return resultadoPreparado;
    }

    static List<String> camposRecusados(JsonNode resposta) {
        if (resposta == null || !resposta.path("errors").isObject()) {
            return List.of();
        }
        return StreamSupport.stream(((Iterable<String>) resposta.path("errors")::fieldNames).spliterator(), false).map(
                campo -> campo.toLowerCase(Locale.ROOT).replaceAll("\\[[0-9]+]", "").replaceFirst("^request\\.", ""))
                .map(CAMPOS::get).filter(Objects::nonNull).distinct().toList();
    }

    @Schema(name = "DiagnosticoHttp")
    public static final class DiagnosticoHttpBean {
        private int statusHttp;

        private Map<String, String> cabecalhos;

        private byte[] corpo;

        private boolean corpoTruncado;

        private boolean corpoIndisponivel;

        public DiagnosticoHttpBean() {
        }

        public int getStatusHttp() {
            return statusHttp;
        }

        public void setStatusHttp(int statusHttp) {
            this.statusHttp = statusHttp;
        }

        public Map<String, String> getCabecalhos() {
            return cabecalhos;
        }

        public void setCabecalhos(Map<String, String> cabecalhos) {
            this.cabecalhos = cabecalhos;
        }

        public byte[] getCorpo() {
            return corpo;
        }

        public void setCorpo(byte[] corpo) {
            this.corpo = corpo;
        }

        public boolean getCorpoTruncado() {
            return corpoTruncado;
        }

        public void setCorpoTruncado(boolean corpoTruncado) {
            this.corpoTruncado = corpoTruncado;
        }

        public boolean getCorpoIndisponivel() {
            return corpoIndisponivel;
        }

        public void setCorpoIndisponivel(boolean corpoIndisponivel) {
            this.corpoIndisponivel = corpoIndisponivel;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof DiagnosticoHttpBean outro)) {
                return false;
            }
            return Objects.equals(statusHttp, outro.statusHttp) && Objects.equals(cabecalhos, outro.cabecalhos)
                    && Objects.equals(corpo, outro.corpo) && Objects.equals(corpoTruncado, outro.corpoTruncado)
                    && Objects.equals(corpoIndisponivel, outro.corpoIndisponivel);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(statusHttp);
            resultado = 31 * resultado + Objects.hashCode(cabecalhos);
            resultado = 31 * resultado + Objects.hashCode(corpo);
            resultado = 31 * resultado + Objects.hashCode(corpoTruncado);
            resultado = 31 * resultado + Objects.hashCode(corpoIndisponivel);
            return resultado;
        }

        @Override
        public String toString() {
            return "DiagnosticoHttp[statusHttp=" + statusHttp + "]";
        }
    }

    @Schema(name = "Resultado")
    public static final class ResultadoBean {
        private String id;

        private StatusPagarme status;

        private ErroRecebedor erro;

        private List<String> campos;

        private DiagnosticoHttpBean diagnostico;

        public ResultadoBean() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public StatusPagarme getStatus() {
            return status;
        }

        public void setStatus(StatusPagarme status) {
            this.status = status;
        }

        public ErroRecebedor getErro() {
            return erro;
        }

        public void setErro(ErroRecebedor erro) {
            this.erro = erro;
        }

        public List<String> getCampos() {
            return campos;
        }

        public void setCampos(List<String> campos) {
            this.campos = campos;
        }

        public DiagnosticoHttpBean getDiagnostico() {
            return diagnostico;
        }

        public void setDiagnostico(DiagnosticoHttpBean diagnostico) {
            this.diagnostico = diagnostico;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ResultadoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(status, outro.status)
                    && Objects.equals(erro, outro.erro) && Objects.equals(campos, outro.campos)
                    && Objects.equals(diagnostico, outro.diagnostico);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(erro);
            resultado = 31 * resultado + Objects.hashCode(campos);
            resultado = 31 * resultado + Objects.hashCode(diagnostico);
            return resultado;
        }

        @Override
        public String toString() {
            return "Resultado[" + "id=" + id + ", status=" + status + ", erro=" + erro + ", campos=" + campos
                    + ", diagnostico=" + diagnostico + "]";
        }

        ResultadoBean comDiagnostico(DiagnosticoHttpBean captura) {
            ResultadoBean resultadoPreparado2 = new ResultadoBean();
            resultadoPreparado2.setId(id);
            resultadoPreparado2.setStatus(status);
            resultadoPreparado2.setErro(erro);
            resultadoPreparado2.setCampos(campos);
            resultadoPreparado2.setDiagnostico(captura);
            return resultadoPreparado2;
        }

        static ResultadoBean indeterminado() {
            return falha(ErroRecebedor.RESPOSTA_INDETERMINADA, List.of());
        }

        static ResultadoBean falha(ErroRecebedor erro, List<String> campos) {
            ResultadoBean resultadoPreparado3 = new ResultadoBean();
            resultadoPreparado3.setId(null);
            resultadoPreparado3.setStatus(null);
            resultadoPreparado3.setErro(erro);
            resultadoPreparado3.setCampos(campos);
            return resultadoPreparado3;
        }
    }
}
