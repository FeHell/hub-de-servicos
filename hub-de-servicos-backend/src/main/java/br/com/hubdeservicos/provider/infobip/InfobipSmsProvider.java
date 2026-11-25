package br.com.hubdeservicos.provider.infobip;

import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean;
import br.com.hubdeservicos.dto.sms.SmsSendResultBean.Erro;
import br.com.hubdeservicos.provider.SmsProvider;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
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
    private final ConfiguracoesExternasService configuracoes;
    private final String remetente;

    @Autowired
    public InfobipSmsProvider(ObjectMapper json, ConfiguracoesExternasService configuracoes,
            @Value("${INFOBIP_SMS_SENDER:${hub.sms.infobip.remetente-padrao:}}") String remetente) {
        this(criarCliente(), json, configuracoes, remetente);
    }

    InfobipSmsProvider(RestClient cliente, ObjectMapper json, ConfiguracoesExternasService configuracoes,
            String remetente) {
        this.cliente = cliente;
        this.json = json;
        this.configuracoes = configuracoes;
        this.remetente = remetente.strip();
    }

    private static RestClient criarCliente() {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(7));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String getProviderCode() {
        return CODIGO;
    }

    @Override
    public String getDisplayName() {
        return "Infobip";
    }

    @Override
    public SmsSendResultBean send(SmsSendRequestBean pedido) {
        ConfiguracoesExternasService.InfobipVO acesso;
        try {
            acesso = configuracoes.lerInfobip();
        } catch (CredencialIndisponivel erro) {
            LOG.warn("Infobip SMS: não foi possível abrir a credencial protegida. Confira a chave mestra.");
            return falha(Erro.CONFIGURACAO_INDISPONIVEL);
        } catch (DataAccessException erro) {
            LOG.warn("Infobip SMS: falha ao consultar as configurações no banco.");
            return falha(Erro.CONFIGURACAO_INDISPONIVEL);
        }
        if (!acesso.getChaveCadastrada()) {
            return falha(Erro.CREDENCIAIS_AUSENTES);
        }
        if (acesso.getUrl().isBlank()) {
            return falha(Erro.URL_BASE_AUSENTE);
        }
        var origem = ServicoExterno.origemInfobip(acesso.getUrl());
        if (origem.isEmpty()) {
            return falha(Erro.URL_BASE_INVALIDA);
        }
        if (remetente.isBlank()) {
            return falha(Erro.REMETENTE_AUSENTE);
        }
        if (!pedido.getDestinatario().matches("[+]?[1-9][0-9]{7,14}")) {
            return falha(Erro.DESTINATARIO_INVALIDO);
        }
        String numero = pedido.getDestinatario().replaceFirst("^\\+", "");
        var mensagem = Map.of("sender", remetente, "destinations", List.of(Map.of("to", numero)), "content",
                Map.of("text", pedido.getMensagem()));
        try {
            return cliente.post().uri(origem.get().resolve("/sms/3/messages"))
                    .header(HttpHeaders.AUTHORIZATION, "App " + acesso.getChave())
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("messages", List.of(mensagem)))
                    .exchange((req, resposta) -> interpretar(resposta.getStatusCode().value(), resposta.getBody()));
        } catch (ResourceAccessException erro) {
            return falha(erro.contains(HttpTimeoutException.class) ? Erro.TIMEOUT : Erro.FALHA_DE_CONEXAO);
        } catch (RestClientException erro) {
            return falha(Erro.FALHA_DO_PROVEDOR);
        }
    }

    private SmsSendResultBean interpretar(int http, java.io.InputStream corpo) throws IOException {
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
            return SmsSendResultBean.aceito(CODIGO, id);
        } catch (JsonProcessingException erro) {
            return falha(Erro.RESPOSTA_INVALIDA);
        }
    }

    private static SmsSendResultBean falha(Erro erro) {
        String diagnostico = switch (erro) {
        case CREDENCIAIS_AUSENTES -> "INFOBIP_API_KEY ausente no banco";
        case URL_BASE_AUSENTE -> "INFOBIP_BASE_URL ausente no banco";
        case URL_BASE_INVALIDA -> "INFOBIP_BASE_URL inválida; use a origem HTTPS da conta, sem caminho";
        case REMETENTE_AUSENTE -> "INFOBIP_SMS_SENDER ausente";
        case AUTENTICACAO_RECUSADA_PELO_PROVEDOR ->
            "Infobip recusou a autenticação; confira a chave e a URL da mesma conta";
        case ACESSO_NEGADO_PELO_PROVEDOR -> "Infobip negou acesso; confira as permissões da chave e da conta";
        case REMETENTE_NAO_PERMITIDO -> "remetente recusado; confira o remetente autorizado na conta";
        case DESTINATARIO_NAO_AUTORIZADO ->
            "destinatário recusado; confira a lista de números autorizados e bloqueios da conta";
        case DESTINATARIO_INVALIDO -> "formato ou prefixo do destinatário recusado";
        case FALHA_DE_CONEXAO -> "falha de conexão HTTPS";
        case TIMEOUT -> "tempo de resposta esgotado";
        default -> "consulte o status HTTP ou o status da mensagem no Console";
        };
        LOG.warn("Falha no envio Infobip SMS: {} — {}", erro, diagnostico);
        return SmsSendResultBean.falha(CODIGO, erro);
    }
}
