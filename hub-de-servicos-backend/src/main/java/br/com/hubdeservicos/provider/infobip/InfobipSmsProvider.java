package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.dto.sms.SmsSendRequest;
import br.com.hubdeservicos.dto.sms.SmsSendResult;
import br.com.hubdeservicos.dto.sms.SmsSendResult.Erro;
import br.com.hubdeservicos.provider.SmsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InfobipSmsProvider implements SmsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InfobipSmsProvider.class);
    private static final String CODIGO = "INFOBIP";
    private final RestClient cliente;
    private final ObjectMapper json;
    private final URI origem;
    private final boolean origemInformada;
    private final String chave;
    private final String remetente;

    @Autowired
    public InfobipSmsProvider(ObjectMapper json, @Value("${INFOBIP_BASE_URL:}") String origem,
                              @Value("${INFOBIP_API_KEY:}") String chave,
                              @Value("${INFOBIP_SMS_SENDER:${hub.sms.infobip.remetente-padrao:}}") String remetente) {
        this(criarCliente(), json, origem, chave, remetente);
        LOG.info("Infobip SMS: INFOBIP_BASE_URL={}, INFOBIP_API_KEY={}, remetente={}",
                !origemInformada ? "ausente" : this.origem == null ? "inválida (esperada origem HTTPS)" : "válida",
                this.chave.isBlank() ? "ausente" : "presente",
                this.remetente.isBlank() ? "ausente (defina INFOBIP_SMS_SENDER)" : "configurado");
    }

    InfobipSmsProvider(RestClient cliente, ObjectMapper json, String origem, String chave, String remetente) {
        this.cliente = cliente;
        this.json = json;
        this.origemInformada = !origem.isBlank();
        this.origem = origemValida(origem);
        this.chave = chave.strip();
        this.remetente = remetente.strip();
    }

    private static RestClient criarCliente() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(7));
        return RestClient.builder().requestFactory(factory).build();
    }

    private static URI origemValida(String valor) {
        try {
            URI uri = URI.create(valor.strip());
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) return uri;
        } catch (IllegalArgumentException erro) {
            // A URL de configuração pode conter dados sensíveis. Não a registre.
        }
        return null;
    }

    @Override
    public String getProviderCode() { return CODIGO; }

    @Override
    public String getDisplayName() { return "Infobip"; }

    @Override
    public SmsSendResult send(SmsSendRequest pedido) {
        if (chave.isBlank()) return falha(Erro.CREDENCIAIS_AUSENTES);
        if (!origemInformada) return falha(Erro.URL_BASE_AUSENTE);
        if (origem == null) return falha(Erro.URL_BASE_INVALIDA);
        if (remetente.isBlank()) return falha(Erro.REMETENTE_AUSENTE);
        if (!pedido.destinatario().matches("[+]?[1-9][0-9]{7,14}")) return falha(Erro.DESTINATARIO_INVALIDO);
        String numero = pedido.destinatario().replaceFirst("^\\+", "");
        var mensagem = Map.of("sender", remetente, "destinations", List.of(Map.of("to", numero)),
                "content", Map.of("text", pedido.mensagem()));
        try {
            return cliente.post().uri(origem.resolve("/sms/3/messages"))
                    .header(HttpHeaders.AUTHORIZATION, "App " + chave)
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("messages", List.of(mensagem)))
                    .exchange((req, resposta) -> interpretar(resposta.getStatusCode().value(), resposta.getBody()));
        } catch (ResourceAccessException erro) {
            return falha(erro.contains(HttpTimeoutException.class) ? Erro.TIMEOUT : Erro.FALHA_DE_CONEXAO);
        } catch (RestClientException erro) {
            return falha(Erro.FALHA_DO_PROVEDOR);
        }
    }

    private SmsSendResult interpretar(int http, java.io.InputStream corpo) throws IOException {
        if (http < 200 || http >= 300) {
            LOG.warn("Infobip SMS: resposta HTTP {}", http);
            return falha(switch (http) {
                case 401 -> Erro.AUTENTICACAO_RECUSADA_PELO_PROVEDOR;
                case 403 -> Erro.ACESSO_NEGADO_PELO_PROVEDOR;
                case 429 -> Erro.LIMITE_DO_PROVEDOR;
                default -> http >= 400 && http < 500 ? Erro.REJEITADO_PELO_PROVEDOR : Erro.FALHA_DO_PROVEDOR;
            });
        }
        try {
            var retorno = json.readTree(corpo);
            if (retorno == null || !retorno.path("messages").isArray() || retorno.path("messages").size() != 1) {
                return falha(Erro.RESPOSTA_INVALIDA);
            }
            var mensagem = retorno.path("messages").get(0);
            var status = mensagem.path("status");
            String grupo = status.path("groupName").asText();
            if (List.of("UNDELIVERABLE", "EXPIRED", "REJECTED").contains(grupo)) {
                int idStatus = status.path("id").asInt(-1);
                LOG.warn("Infobip SMS recusado: grupo={}, status={}", grupo, idStatus);
                if ("REJECTED".equals(grupo)) {
                    return falha(switch (idStatus) {
                        case 11, 13 -> Erro.REMETENTE_NAO_PERMITIDO;
                        case 14, 18 -> Erro.DESTINATARIO_NAO_AUTORIZADO;
                        case 8, 51, 52 -> Erro.DESTINATARIO_INVALIDO;
                        default -> Erro.REJEITADO_PELO_PROVEDOR;
                    });
                }
                return falha(Erro.REJEITADO_PELO_PROVEDOR);
            }
            String id = mensagem.path("messageId").asText();
            if (!List.of("PENDING", "DELIVERED").contains(grupo) || !id.matches("[A-Za-z0-9._:-]{1,128}")) {
                return falha(Erro.RESPOSTA_INVALIDA);
            }
            LOG.info("Infobip aceitou SMS para processamento: messageId={}", id);
            return SmsSendResult.aceito(CODIGO, id);
        } catch (JsonProcessingException erro) {
            return falha(Erro.RESPOSTA_INVALIDA);
        }
    }

    private static SmsSendResult falha(Erro erro) {
        String diagnostico = switch (erro) {
            case CREDENCIAIS_AUSENTES -> "INFOBIP_API_KEY ausente";
            case URL_BASE_AUSENTE -> "INFOBIP_BASE_URL ausente";
            case URL_BASE_INVALIDA -> "INFOBIP_BASE_URL inválida; use a origem HTTPS da conta, sem caminho";
            case REMETENTE_AUSENTE -> "INFOBIP_SMS_SENDER ausente";
            case AUTENTICACAO_RECUSADA_PELO_PROVEDOR -> "Infobip recusou a autenticação; confira a chave e a URL da mesma conta";
            case ACESSO_NEGADO_PELO_PROVEDOR -> "Infobip negou acesso; confira as permissões da chave e da conta";
            case REMETENTE_NAO_PERMITIDO -> "remetente recusado; confira o remetente autorizado na conta";
            case DESTINATARIO_NAO_AUTORIZADO -> "destinatário recusado; confira a lista de números autorizados e bloqueios da conta";
            case DESTINATARIO_INVALIDO -> "formato ou prefixo do destinatário recusado";
            case FALHA_DE_CONEXAO -> "falha de conexão HTTPS";
            case TIMEOUT -> "tempo de resposta esgotado";
            default -> "consulte o status HTTP ou o status da mensagem no Console";
        };
        LOG.warn("Falha no envio Infobip SMS: {} — {}", erro, diagnostico);
        return SmsSendResult.falha(CODIGO, erro);
    }
}
