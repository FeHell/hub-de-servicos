package br.com.hubdeservicos.sms.provider.textbee;

import br.com.hubdeservicos.sms.dto.SmsSendRequest;
import br.com.hubdeservicos.sms.dto.SmsSendResult;
import br.com.hubdeservicos.sms.dto.SmsSendResult.Erro;
import br.com.hubdeservicos.sms.provider.SmsProvider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TextBeeSmsProvider implements SmsProvider {

    private static final String CODIGO = "TEXTBEE";
    private static final String ROTA_ENVIO = "/api/v1/gateway/send-sms";
    private final RestClient cliente;
    private final ObjectMapper json;
    private final String chaveApi;
    private final String dispositivo;

    @Autowired
    public TextBeeSmsProvider(ObjectMapper json, @Value("${TEXTBEE_API_KEY:}") String chaveApi,
                             @Value("${TEXTBEE_DEVICE_ID:}") String dispositivo) {
        this(criarCliente(), json, chaveApi, dispositivo);
    }

    TextBeeSmsProvider(RestClient cliente, ObjectMapper json, String chaveApi, String dispositivo) {
        this.cliente = cliente;
        this.json = json;
        this.chaveApi = chaveApi;
        this.dispositivo = dispositivo.isBlank() ? null : dispositivo.trim();
    }

    private static RestClient criarCliente() {
        var requisicoes = new SimpleClientHttpRequestFactory();
        requisicoes.setConnectTimeout(3_000);
        requisicoes.setReadTimeout(7_000);
        return RestClient.builder().baseUrl("https://api.textbee.dev").requestFactory(requisicoes).build();
    }

    @Override
    public String getProviderCode() {
        return CODIGO;
    }

    @Override
    public String getDisplayName() {
        return "TextBee";
    }

    @Override
    public SmsSendResult send(SmsSendRequest request) {
        if (chaveApi.isBlank()) {
            return falha(Erro.CREDENCIAIS_AUSENTES);
        }
        try {
            return cliente.post().uri(ROTA_ENVIO).header("x-api-key", chaveApi)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(new Envio(List.of(request.destinatario()), request.mensagem(), dispositivo))
                    .exchange((requisicao, resposta) -> {
                        int status = resposta.getStatusCode().value();
                        if (status == 401 || status == 403) {
                            return falha(Erro.ACESSO_NEGADO_PELO_PROVEDOR);
                        }
                        if (status == 429) {
                            return falha(Erro.LIMITE_DO_PROVEDOR);
                        }
                        if (status >= 400 && status < 500) {
                            return falha(Erro.REJEITADO_PELO_PROVEDOR);
                        }
                        if (status != 200) {
                            return falha(Erro.FALHA_DO_PROVEDOR);
                        }
                        var data = json.readTree(resposta.getBody());
                        if (data == null) {
                            return falha(Erro.RESPOSTA_INVALIDA);
                        }
                        var resultado = data.path("data");
                        var lote = resultado.path("smsBatchId");
                        String idExterno = lote.isTextual() && !lote.textValue().isBlank() ? lote.textValue() : null;
                        var sucesso = resultado.path("success");
                        if (sucesso.isBoolean()) {
                            return sucesso.booleanValue() ? SmsSendResult.aceito(CODIGO, idExterno)
                                    : falha(Erro.REJEITADO_PELO_PROVEDOR);
                        }
                        var enviados = resultado.path("successCount");
                        var falhas = resultado.path("failureCount");
                        if (enviados.isIntegralNumber() && falhas.isIntegralNumber()) {
                            if (enviados.intValue() == 1 && falhas.intValue() == 0) {
                                return SmsSendResult.aceito(CODIGO, idExterno);
                            }
                            if (enviados.intValue() == 0 && falhas.intValue() == 1) {
                                return falha(Erro.REJEITADO_PELO_PROVEDOR);
                            }
                        }
                        return falha(Erro.RESPOSTA_INVALIDA);
                    });
        } catch (ResourceAccessException erro) {
            if (erro.contains(SocketTimeoutException.class)) {
                return falha(Erro.TIMEOUT);
            }
            if (erro.contains(JsonProcessingException.class)) {
                return falha(Erro.RESPOSTA_INVALIDA);
            }
            return falha(Erro.FALHA_DE_CONEXAO);
        } catch (RestClientException erro) {
            return falha(Erro.FALHA_DO_PROVEDOR);
        }
    }

    private static SmsSendResult falha(Erro erro) {
        return SmsSendResult.falha(CODIGO, erro);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Envio(List<String> recipients, String message, String deviceId) {
        @Override
        public String toString() {
            return "EnvioSms";
        }
    }
}
